package com.mouajmd.fluxcore.data

import android.content.Context
import android.net.Uri
import com.mouajmd.fluxcore.BuildConfig
import com.mouajmd.fluxcore.model.AutomationRule
import com.mouajmd.fluxcore.system.RootShell
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object RuleStore {
    private const val PREF = "fluxcore_rules_v1"
    private const val KEY = "rules_json"

    private val lock = Any()
    @Volatile
    private var loadedFromDisk = false

    private val _rules = MutableStateFlow<List<AutomationRule>>(emptyList())
    val rules: StateFlow<List<AutomationRule>> = _rules.asStateFlow()

    fun init(context: Context) {
        ensureLoaded(context)
    }

    fun getSnapshot(context: Context): List<AutomationRule> = ensureLoaded(context)

    fun load(context: Context): List<AutomationRule> = synchronized(lock) {
        val sp = context.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val raw = sp.getString(KEY, "[]") ?: "[]"
        val list = mutableListOf<AutomationRule>()
        runCatching {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                arr.optJSONObject(i)?.let { list.add(AutomationRule.fromJson(it)) }
            }
        }.onFailure {
            AppLogger.error("STORE", "Failed to parse stored rules: ${it.message}")
        }
        val immutable = list.toList()
        loadedFromDisk = true
        _rules.value = immutable
        immutable
    }

    private fun ensureLoaded(context: Context): List<AutomationRule> {
        if (loadedFromDisk) return _rules.value
        return load(context)
    }

    fun findById(context: Context, ruleId: String): AutomationRule? =
        ensureLoaded(context).firstOrNull { it.id == ruleId }

    fun saveAll(context: Context, rules: List<AutomationRule>) = synchronized(lock) {
        val arr = JSONArray()
        rules.forEach { r ->
            runCatching { arr.put(r.toJson()) }
        }
        context.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, arr.toString())
            .apply()
        loadedFromDisk = true
        _rules.value = rules.toList()
    }

    fun upsert(context: Context, rule: AutomationRule) = synchronized(lock) {
        val all = ensureLoaded(context).toMutableList()
        val idx = all.indexOfFirst { it.id == rule.id }
        if (idx >= 0) {
            all[idx] = rule
        } else {
            all.add(0, rule)
        }
        saveAll(context, all)
    }

    fun delete(context: Context, ruleId: String) = synchronized(lock) {
        val remaining = ensureLoaded(context).filterNot { it.id == ruleId }
        saveAll(context, remaining)
    }

    fun buildExportJson(context: Context): String {
        val root = JSONObject().apply {
            put("app", "FluxCore")
            put("version", BuildConfig.VERSION_NAME)
            put("schemaVersion", 2)
            put("advancedMode", SettingsStore.settings.value.advancedMode)
            put("rules", JSONArray().apply {
                ensureLoaded(context).forEach { put(it.toJson()) }
            })
        }
        return root.toString(2)
    }

    fun exportToUri(context: Context, uri: Uri): Boolean {
        return try {
            val json = buildExportJson(context)
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(json.toByteArray(StandardCharsets.UTF_8))
                out.flush()
            } ?: return false
            AppLogger.log("CONFIG", "Config exported via SAF")
            true
        } catch (t: Throwable) {
            AppLogger.error("CONFIG", "SAF config export failed: ${t.message}")
            false
        }
    }

    fun exportToDownload(context: Context): String? {
        val dirPath = "/storage/emulated/0/Download/FluxCore"
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "FluxCore_config_$stamp.json"
        val fullPath = "$dirPath/$fileName"
        return try {
            val bytes = buildExportJson(context).toByteArray(StandardCharsets.UTF_8)
            val tmp = File(context.cacheDir, fileName)
            tmp.writeBytes(bytes)
            val outDir = File(dirPath)
            if (!outDir.exists()) outDir.mkdirs()
            try {
                File(outDir, fileName).writeBytes(bytes)
                AppLogger.log("CONFIG", "Config exported: $fullPath")
                fullPath
            } catch (_: Throwable) {
                val res = RootShell.execDetailed(
                    "mkdir -p ${RootShell.shellQuote(dirPath)} && " +
                        "cp -f ${RootShell.shellQuote(tmp.absolutePath)} ${RootShell.shellQuote(fullPath)} && " +
                        "chmod 664 ${RootShell.shellQuote(fullPath)}"
                )
                if (res.isSuccess) {
                    AppLogger.log("CONFIG", "Config exported via Root: $fullPath")
                    fullPath
                } else {
                    AppLogger.error("CONFIG", "Root config export failed: ${res.output}")
                    null
                }
            } finally {
                runCatching { tmp.delete() }
            }
        } catch (t: Throwable) {
            AppLogger.error("CONFIG", "Config export failed: ${t.message}")
            null
        }
    }

    fun importFromJsonString(context: Context, raw: String, overwrite: Boolean): Int {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return -1
        return try {
            val arr: JSONArray
            if (trimmed.startsWith("[")) {
                // Support raw legacy rule-array JSON files directly
                arr = JSONArray(trimmed)
            } else {
                val root = JSONObject(trimmed)
                if (root.has("advancedMode")) {
                    SettingsStore.setAdvancedMode(context, root.optBoolean("advancedMode", false))
                }
                arr = root.optJSONArray("rules") ?: return -1
            }

            val imported = mutableListOf<AutomationRule>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                var rule = AutomationRule.fromJson(obj)
                if (!overwrite) {
                    rule = rule.copy(id = UUID.randomUUID().toString())
                }
                imported.add(rule)
            }
            synchronized(lock) {
                if (overwrite) {
                    saveAll(context, imported)
                } else {
                    val current = ensureLoaded(context).toMutableList()
                    current.addAll(0, imported)
                    saveAll(context, current)
                }
            }
            AppLogger.log("CONFIG", "Imported ${imported.size} rules (${if (overwrite) "overwrite" else "merge"})")
            imported.size
        } catch (t: Throwable) {
            AppLogger.error("CONFIG", "Config import failed: ${t.message}")
            -1
        }
    }

    fun importFromUri(context: Context, uri: Uri, overwrite: Boolean): Int {
        return try {
            val raw = context.contentResolver.openInputStream(uri)?.use { ins ->
                ins.readBytes().toString(StandardCharsets.UTF_8)
            } ?: return -1
            importFromJsonString(context, raw, overwrite)
        } catch (t: Throwable) {
            AppLogger.error("CONFIG", "SAF config import failed: ${t.message}")
            -1
        }
    }

    fun importFromFile(context: Context, file: File, overwrite: Boolean): Int {
        return try {
            val raw = try {
                file.readText(StandardCharsets.UTF_8)
            } catch (_: Throwable) {
                val res = RootShell.execDetailed("cat ${RootShell.shellQuote(file.absolutePath)}")
                if (!res.isSuccess) return -1
                res.output
            }
            importFromJsonString(context, raw, overwrite)
        } catch (t: Throwable) {
            AppLogger.error("CONFIG", "File config import failed: ${t.message}")
            -1
        }
    }

    fun listLocalBackupFiles(): List<File> {
        val dir = File("/storage/emulated/0/Download/FluxCore")
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        return runCatching {
            dir.listFiles()
                ?.filter { it.isFile && it.name.endsWith(".json", ignoreCase = true) }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
        }.getOrDefault(emptyList())
    }
}
