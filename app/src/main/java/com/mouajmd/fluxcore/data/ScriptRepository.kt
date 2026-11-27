package com.mouajmd.fluxcore.data

import com.mouajmd.fluxcore.model.ScriptCategory
import com.mouajmd.fluxcore.model.ShellScriptItem
import com.mouajmd.fluxcore.system.RootShell
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * Discovers local .sh scripts across user storage and /data/adb directories
 * and provides bounded, safe read-only previews.
 */
object ScriptRepository {

    private const val MAX_PREVIEW_BYTES = 65_536
    private const val MAX_FALLBACK_RESULTS = 150

    private val storageDirs = listOf(
        "/storage/emulated/0/Download",
        "/storage/emulated/0/Documents",
        "/storage/emulated/0/Scripts"
    )
    private val adbDirs = listOf(
        "/data/adb/service.d",
        "/data/adb/post-fs-data.d",
        "/data/adb/modules"
    )

    fun scanLocalShellScripts(): List<ShellScriptItem> {
        val results = LinkedHashMap<String, ShellScriptItem>()

        val shellCmd = buildString {
            for (dir in storageDirs + adbDirs) {
                val depth = if (dir.endsWith("/modules")) 3 else 4
                val qDir = RootShell.shellQuote(dir)
                append("if [ -d $qDir ]; then find $qDir -maxdepth $depth -type f -name '*.sh' 2>/dev/null | head -n 120 | while IFS= read -r f; do ")
                append("sz=$(stat -c '%s' \"\$f\" 2>/dev/null || wc -c < \"\$f\" 2>/dev/null || echo 0); ")
                append("mt=$(stat -c '%Y' \"\$f\" 2>/dev/null || echo 0); ")
                append("printf '%s\\t%s\\t%s\\n' \"\$f\" \"\$sz\" \"\$mt\"; ")
                append("done; fi;\n")
            }
        }

        val rootRes = RootShell.execDetailed(shellCmd, timeoutMs = 12_000L)
        if (rootRes.isSuccess && rootRes.output.isNotBlank()) {
            rootRes.output.lineSequence().forEach { line ->
                val parts = line.split('\t')
                val path = parts.firstOrNull()?.trim().orEmpty()
                if (isSafeScriptPath(path)) {
                    val size = parts.getOrNull(1)?.trim()?.toLongOrNull() ?: 0L
                    val modSec = parts.getOrNull(2)?.trim()?.toLongOrNull() ?: 0L
                    val file = File(path)
                    val category = if (path.startsWith("/data/adb")) ScriptCategory.ADB else ScriptCategory.STORAGE
                    results[path] = ShellScriptItem(
                        path = path,
                        fileName = file.name,
                        parentDir = file.parent ?: "/",
                        category = category,
                        sizeBytes = size,
                        lastModifiedMs = modSec * 1000L
                    )
                }
            }
        }

        // Fallback to standard Java File traversal for user storage when Root is unavailable
        if (results.isEmpty()) {
            for (rootPath in storageDirs) {
                val dir = File(rootPath)
                if (dir.exists() && dir.isDirectory) {
                    walkDirLimited(dir, maxDepth = 3, out = results)
                }
            }
        }

        return results.values.sortedWith(
            compareBy<ShellScriptItem> { it.category.ordinal }
                .thenByDescending { it.lastModifiedMs }
                .thenBy { it.fileName.lowercase() }
        )
    }

    fun readScriptPreview(path: String): String {
        val clean = path.trim()
        if (!isSafeScriptPath(clean)) return ""

        val directFile = File(clean)
        if (directFile.exists() && directFile.isFile && directFile.canRead()) {
            return runCatching {
                directFile.inputStream().use { ins ->
                    val buf = ByteArray(MAX_PREVIEW_BYTES)
                    val read = ins.read(buf)
                    if (read <= 0) "" else String(buf, 0, read, StandardCharsets.UTF_8)
                }
            }.getOrElse { "" }
        }

        val res = RootShell.execDetailed("head -c $MAX_PREVIEW_BYTES ${RootShell.shellQuote(clean)}", timeoutMs = 5_000L)
        return res.output
    }

    private fun isSafeScriptPath(path: String): Boolean =
        path.startsWith("/") &&
            path.endsWith(".sh", ignoreCase = true) &&
            !path.contains('\u0000') &&
            !path.contains('\n') &&
            !path.contains("..")

    private fun walkDirLimited(dir: File, maxDepth: Int, out: MutableMap<String, ShellScriptItem>) {
        if (maxDepth < 0 || out.size >= MAX_FALLBACK_RESULTS) return
        val children = runCatching { dir.listFiles() }.getOrNull() ?: return
        for (f in children) {
            if (out.size >= MAX_FALLBACK_RESULTS) return
            if (f.isDirectory && !f.name.startsWith(".")) {
                walkDirLimited(f, maxDepth - 1, out)
            } else if (f.isFile && f.name.endsWith(".sh", ignoreCase = true)) {
                out[f.absolutePath] = ShellScriptItem(
                    path = f.absolutePath,
                    fileName = f.name,
                    parentDir = f.parent ?: "/",
                    category = ScriptCategory.STORAGE,
                    sizeBytes = f.length(),
                    lastModifiedMs = f.lastModified()
                )
            }
        }
    }
}
