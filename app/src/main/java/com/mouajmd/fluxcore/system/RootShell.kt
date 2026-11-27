package com.mouajmd.fluxcore.system

import android.os.Looper
import android.util.Log
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicLong
import java.util.regex.Pattern

data class ShellResult(
    val output: String,
    val exitCode: Int,
    val timedOut: Boolean = false
) {
    val isSuccess: Boolean get() = exitCode == 0 && !timedOut
}

/**
 * Persistent Root Shell channel with strict per-command timeout protection,
 * automatic broken-pipe reconnection, and forcible teardown to prevent zombie processes.
 */
object RootShell {

    private const val TAG = "FluxCoreRootShell"
    const val DEFAULT_TIMEOUT_MS = 8_000L
    const val EXTENDED_TIMEOUT_MS = 25_000L
    private const val MAX_OUTPUT_CHARS = 256_000

    private val seq = AtomicLong(1L)
    private val sessionLock = Any()

    // Dedicated daemon executor used solely for reading persistent shell stdout under timed Future
    private val readerPool = Executors.newCachedThreadPool { r ->
        Thread(r, "fluxcore-su-reader").apply { isDaemon = true }
    }

    private class PersistentSession(
        val process: Process,
        val writer: DataOutputStream,
        val reader: BufferedReader
    ) {
        fun isAlive(): Boolean = runCatching { process.isAlive }.getOrDefault(false)

        fun destroyForcibly() {
            runCatching { writer.close() }
            runCatching { reader.close() }
            runCatching { process.destroyForcibly() }
        }
    }

    @Volatile
    private var session: PersistentSession? = null

    private val quotedSsidPattern = Pattern.compile("SSID:\\s*\"([^\"]+)\"")
    private val unquotedSsidPattern = Pattern.compile("SSID:\\s*([^,\\s\\n\"<>]+)")
    private val btNamePattern = Pattern.compile("\\(([^()]{2,40})\\)")
    private val btMacPattern = Pattern.compile("([0-9A-Fa-f]{2}(?::[0-9A-Fa-f]{2}){5})")
    private val focusPkgPattern = Pattern.compile("([a-zA-Z0-9_]+(?:\\.[a-zA-Z0-9_]+)+)/")

    fun shellQuote(raw: String): String = "'${raw.replace("'", "'\\''")}'"

    private fun openSessionLocked(): PersistentSession {
        val existing = session
        if (existing != null && existing.isAlive()) {
            return existing
        }
        existing?.destroyForcibly()
        val pb = ProcessBuilder("su")
        pb.redirectErrorStream(true)
        val proc = pb.start()
        val created = PersistentSession(
            process = proc,
            writer = DataOutputStream(proc.outputStream),
            reader = BufferedReader(InputStreamReader(proc.inputStream, StandardCharsets.UTF_8))
        )
        session = created
        return created
    }

    private fun resetSessionLocked() {
        val cur = session
        session = null
        cur?.destroyForcibly()
    }

    fun closeSession() = synchronized(sessionLock) {
        resetSessionLocked()
    }

    fun execDetailed(cmd: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS): ShellResult {
        if (cmd.isBlank()) return ShellResult("", 0)
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Log.w(TAG, "RootShell invoked on main thread; command truncated: ${cmd.take(64)}")
        }

        synchronized(sessionLock) {
            // Try up to 2 attempts: initial persistent session + 1 clean reconnect on broken pipe
            for (attempt in 0..1) {
                val active = try {
                    openSessionLocked()
                } catch (t: Throwable) {
                    return ShellResult("ERR: ${t.message ?: t.javaClass.simpleName}", -1)
                }

                val token = "__FLUX_END_${seq.getAndIncrement()}_${System.nanoTime()}__"
                val framedCmd = "$cmd\n__FLUX_EC=\$?\necho \"$token:\$__FLUX_EC\"\n"

                val writeResult = runCatching {
                    active.writer.write(framedCmd.toByteArray(StandardCharsets.UTF_8))
                    active.writer.flush()
                }
                if (writeResult.isFailure) {
                    resetSessionLocked()
                    if (attempt == 0) continue
                    val err = writeResult.exceptionOrNull()
                    return ShellResult("ERR: ${err?.message ?: "Broken su pipe"}", -1)
                }

                val future = readerPool.submit(Callable<ShellResult?> {
                    val out = StringBuilder()
                    var truncated = false
                    var line: String?
                    while (active.reader.readLine().also { line = it } != null) {
                        val current = line ?: continue
                        val markerIdx = current.indexOf("$token:")
                        if (markerIdx >= 0) {
                            if (markerIdx > 0 && !truncated) {
                                out.append(current.substring(0, markerIdx))
                            }
                            val codeStr = current.substring(markerIdx + token.length + 1).trim()
                            val exitCode = codeStr.toIntOrNull() ?: 0
                            return@Callable ShellResult(out.toString().trim(), exitCode)
                        }
                        if (!truncated) {
                            if (out.length + current.length < MAX_OUTPUT_CHARS) {
                                out.append(current).append('\n')
                            } else {
                                out.append("\n... [output truncated]")
                                truncated = true
                            }
                        }
                    }
                    // EOF reached before sentinel: shell died unexpectedly
                    null
                })

                return try {
                    val result = future.get(timeoutMs.coerceIn(1_000L, 60_000L), TimeUnit.MILLISECONDS)
                    if (result != null) {
                        result
                    } else {
                        resetSessionLocked()
                        if (attempt == 0) continue
                        ShellResult("ERR: Root shell terminated unexpectedly", -1)
                    }
                } catch (_: TimeoutException) {
                    future.cancel(true)
                    resetSessionLocked()
                    ShellResult("ERR: Command timed out after ${timeoutMs}ms", -124, timedOut = true)
                } catch (t: Throwable) {
                    future.cancel(true)
                    resetSessionLocked()
                    if (attempt == 0) continue
                    ShellResult("ERR: ${t.message ?: t.javaClass.simpleName}", -1)
                }
            }
            return ShellResult("ERR: Root shell unavailable", -1)
        }
    }

    fun exec(cmd: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS): String =
        execDetailed(cmd, timeoutMs).output

    fun hasRoot(): Boolean {
        val res = execDetailed("id", timeoutMs = 4_000L)
        return res.isSuccess && res.output.contains("uid=0")
    }

    fun queryTopPackageViaDumpsys(): String {
        val out = exec(
            "dumpsys window 2>/dev/null | grep -E 'mCurrentFocus|mFocusedApp' | tail -n 2",
            timeoutMs = 4_000L
        )
        if (out.isEmpty()) return ""
        val m = focusPkgPattern.matcher(out)
        var last = ""
        while (m.find()) {
            last = m.group(1).orEmpty()
        }
        return last
    }

    fun queryCurrentWifiSsid(): String {
        val raw = exec(
            "cmd wifi status 2>/dev/null; dumpsys wifi 2>/dev/null | grep -E 'WifiInfo|mWifiInfo|SSID:' | head -n 15",
            timeoutMs = 5_000L
        )
        if (raw.isEmpty()) return ""

        val mQuoted = quotedSsidPattern.matcher(raw)
        while (mQuoted.find()) {
            val s = mQuoted.group(1)?.trim().orEmpty()
            if (s.isNotEmpty() && !s.equals("<unknown ssid>", ignoreCase = true)) return s
        }

        val mUnquoted = unquotedSsidPattern.matcher(raw)
        while (mUnquoted.find()) {
            val s = mUnquoted.group(1)?.trim().orEmpty()
            if (s.isNotEmpty() && !s.equals("null", ignoreCase = true) && !s.equals("<unknown", ignoreCase = true)) {
                return s
            }
        }
        return ""
    }

    fun queryConnectedBtDevices(): String =
        exec(
            "dumpsys bluetooth_manager 2>/dev/null | grep -iE 'Connected:|mActiveDevice|Device:' | head -n 15",
            timeoutMs = 5_000L
        )

    fun parseBtCandidates(dump: String): List<String> {
        if (dump.isEmpty()) return emptyList()
        val set = LinkedHashSet<String>()
        val nameMatcher = btNamePattern.matcher(dump)
        while (nameMatcher.find()) {
            val name = nameMatcher.group(1)?.trim().orEmpty()
            if (name.isNotEmpty() && !name.equals("null", ignoreCase = true) && !name.all { it.isDigit() }) {
                set.add(name)
            }
        }
        val macMatcher = btMacPattern.matcher(dump)
        while (macMatcher.find()) {
            val mac = macMatcher.group(1).orEmpty()
            if (mac.isNotEmpty() && mac != "00:00:00:00:00:00") {
                set.add(mac)
            }
        }
        return set.toList()
    }
}
