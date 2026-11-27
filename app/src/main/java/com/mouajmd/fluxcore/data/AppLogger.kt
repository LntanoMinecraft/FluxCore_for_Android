package com.mouajmd.fluxcore.data

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.Immutable
import com.mouajmd.fluxcore.BuildConfig
import com.mouajmd.fluxcore.system.RootShell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

enum class LogLevel { INFO, WARN, ERROR }

@Immutable
data class LogEntry(
    val id: Long,
    val timestampMs: Long,
    val formattedTime: String,
    val tag: String,
    val level: LogLevel,
    val message: String
) {
    fun toPlainLine(): String = "[$formattedTime] [${level.name}] [$tag] $message"
}

/**
 * Thread-safe in-memory ring buffer logger with non-blocking asynchronous disk persistence.
 * Never performs synchronous file I/O on the caller thread (safe from Main/UI thread).
 */
object AppLogger {
    private const val MAX_ENTRIES = 2000
    private const val MAX_FILE_BYTES = 512 * 1024L
    private const val PERSIST_FILE_NAME = "fluxcore_live.log"

    private val lock = Any()
    private val seq = AtomicLong(1L)
    private val buffer = ArrayDeque<LogEntry>(MAX_ENTRIES)
    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries: StateFlow<List<LogEntry>> = _entries.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val diskScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO.limitedParallelism(1)
    )

    private val persistedLineRegex = Regex("^\\[([^\\]]+)] \\[([A-Z]+)] \\[([^\\]]+)] (.*)$")

    private val timeFmt = ThreadLocal.withInitial { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }
    private val exportStampFmt = ThreadLocal.withInitial { SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US) }
    private val fullDateFmt = ThreadLocal.withInitial { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US) }

    @Volatile
    private var logFile: File? = null

    fun init(context: Context) {
        val file = File(context.applicationContext.filesDir, PERSIST_FILE_NAME)
        logFile = file
        diskScope.launch {
            if (!file.exists()) return@launch
            val loaded = runCatching {
                file.readLines(StandardCharsets.UTF_8).takeLast(400).mapNotNull(::parsePersistedLine)
            }.getOrDefault(emptyList())
            if (loaded.isEmpty()) return@launch
            synchronized(lock) {
                if (buffer.isEmpty()) {
                    loaded.forEach { buffer.addLast(it) }
                    _entries.value = buffer.toList()
                }
            }
        }
    }

    private fun parsePersistedLine(line: String): LogEntry? {
        val match = persistedLineRegex.matchEntire(line) ?: return null
        val time = match.groupValues[1]
        val level = runCatching { LogLevel.valueOf(match.groupValues[2]) }.getOrDefault(LogLevel.INFO)
        val tag = match.groupValues[3]
        val msg = match.groupValues[4]
        return LogEntry(
            id = seq.getAndIncrement(),
            timestampMs = System.currentTimeMillis(),
            formattedTime = time,
            tag = tag,
            level = level,
            message = msg
        )
    }

    fun log(tag: String, message: String, level: LogLevel = inferLevel(tag)) {
        val now = System.currentTimeMillis()
        val formatted = timeFmt.get()?.format(Date(now)) ?: "00:00:00.000"
        val entry = LogEntry(
            id = seq.getAndIncrement(),
            timestampMs = now,
            formattedTime = formatted,
            tag = tag,
            level = level,
            message = message
        )
        val plainLine = entry.toPlainLine()
        synchronized(lock) {
            buffer.addLast(entry)
            while (buffer.size > MAX_ENTRIES) {
                buffer.removeFirst()
            }
            _entries.value = buffer.toList()
        }
        enqueueDiskAppend(plainLine)
    }

    fun warn(tag: String, message: String) = log(tag, message, LogLevel.WARN)
    fun error(tag: String, message: String) = log(tag, message, LogLevel.ERROR)

    fun clear() {
        synchronized(lock) {
            buffer.clear()
            _entries.value = emptyList()
        }
        diskScope.launch {
            runCatching { logFile?.writeText("", StandardCharsets.UTF_8) }
        }
    }

    private fun inferLevel(tag: String): LogLevel = when {
        tag.equals("ERROR", ignoreCase = true) || tag.equals("ERR", ignoreCase = true) -> LogLevel.ERROR
        tag.equals("WARN", ignoreCase = true) -> LogLevel.WARN
        else -> LogLevel.INFO
    }

    private fun enqueueDiskAppend(plainLine: String) {
        diskScope.launch {
            val f = logFile ?: return@launch
            runCatching {
                if (f.exists() && f.length() > MAX_FILE_BYTES) {
                    val tail = synchronized(lock) {
                        buffer.takeLast(500).joinToString("\n") { it.toPlainLine() } + "\n"
                    }
                    f.writeText(tail, StandardCharsets.UTF_8)
                } else {
                    f.appendText(plainLine + "\n", StandardCharsets.UTF_8)
                }
            }
        }
    }

    fun buildExportContent(): String {
        val snapshot = synchronized(lock) { buffer.toList() }
        val stamp = fullDateFmt.get()?.format(Date()) ?: ""
        return buildString {
            append("=== FluxCore v").append(BuildConfig.VERSION_NAME)
                .append(" (").append(BuildConfig.VERSION_CODE).append(") Runtime Log ===\n")
            append("Exported: ").append(stamp).append("\n")
            append("Device: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL)
                .append(" (Android ").append(Build.VERSION.RELEASE)
                .append(" / SDK ").append(Build.VERSION.SDK_INT).append(")\n")
            append("Root: ").append(if (RootShell.hasRoot()) "Granted" else "Unavailable").append("\n")
            append("Entries: ").append(snapshot.size).append("\n")
            append("========================================\n\n")
            snapshot.forEach { append(it.toPlainLine()).append("\n") }
        }
    }

    fun exportToUri(context: Context, uri: Uri): Boolean {
        return try {
            val content = buildExportContent()
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(content.toByteArray(StandardCharsets.UTF_8))
                out.flush()
            } ?: return false
            log("EXPORT", "Log exported via SAF")
            true
        } catch (t: Throwable) {
            error("EXPORT", "SAF log export failed: ${t.message}")
            false
        }
    }

    fun exportToDownload(context: Context): String? {
        val dirPath = "/storage/emulated/0/Download/FluxCore"
        val stamp = exportStampFmt.get()?.format(Date()) ?: System.currentTimeMillis().toString()
        val fileName = "FluxCore_log_$stamp.txt"
        val fullPath = "$dirPath/$fileName"
        return try {
            val bytes = buildExportContent().toByteArray(StandardCharsets.UTF_8)
            val tmp = File(context.cacheDir, fileName)
            tmp.writeBytes(bytes)

            val outDir = File(dirPath)
            if (!outDir.exists()) outDir.mkdirs()
            try {
                File(outDir, fileName).writeBytes(bytes)
                log("EXPORT", "Log saved: $fullPath")
                fullPath
            } catch (_: Throwable) {
                val res = RootShell.execDetailed(
                    "mkdir -p ${RootShell.shellQuote(dirPath)} && " +
                        "cp -f ${RootShell.shellQuote(tmp.absolutePath)} ${RootShell.shellQuote(fullPath)} && " +
                        "chmod 664 ${RootShell.shellQuote(fullPath)}"
                )
                if (res.isSuccess) {
                    log("EXPORT", "Log saved via Root: $fullPath")
                    fullPath
                } else {
                    error("EXPORT", "Root log export failed: ${res.output}")
                    null
                }
            } finally {
                runCatching { tmp.delete() }
            }
        } catch (t: Throwable) {
            error("EXPORT", "Log export failed: ${t.message}")
            null
        }
    }
}
