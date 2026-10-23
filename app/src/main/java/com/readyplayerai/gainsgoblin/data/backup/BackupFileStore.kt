package com.readyplayerai.gainsgoblin.data.backup

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupFileStore(private val root: File) {
    init {
        root.mkdirs()
    }

    fun writePreImport(
        json: String,
        kind: String,
        now: Long = System.currentTimeMillis()
    ): File {
        val safeKind = kind
            .map { c -> if (c.isLetterOrDigit() || c == '_' || c == '-') c else '_' }
            .joinToString("")
            .ifBlank { "backup" }
        val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.ROOT).format(Date(now))
        val file = File(root, "pre-import-$safeKind-$timestamp.json")
        file.writeText(json, Charsets.UTF_8)
        file.setLastModified(now)
        return file
    }

    fun prune(keep: Int = 3) {
        val limit = if (keep < 0) 0 else keep
        prunePrefix("pre-import-", limit)
        prunePrefix("legacy-pre-migration-", limit)
    }

    fun latestPreImport(): File? =
        listPreImports().firstOrNull()

    fun listPreImports(): List<File> =
        preImportFiles("pre-import-")

    fun readBackup(name: String): String? {
        if (!isSafeName(name)) return null
        return File(root, name).takeIf { it.isFile }?.readText(Charsets.UTF_8)
    }

    fun deleteBackup(name: String): Boolean {
        if (!isSafeName(name)) return false
        return File(root, name).delete()
    }

    private fun prunePrefix(prefix: String, keep: Int) {
        val candidates = preImportFiles(prefix)
        candidates.drop(keep).forEach { file ->
            runCatching { file.delete() }
        }
    }

    private fun preImportFiles(prefix: String): List<File> {
        val files = root.listFiles() ?: return emptyList()
        return files
            .filter { it.isFile && it.name.startsWith(prefix) }
            .sortedWith(compareByDescending<File> { it.lastModified() }.thenByDescending { it.name })
    }

    private fun isSafeName(name: String): Boolean =
        name.isNotBlank() &&
            !name.contains("/") &&
            !name.contains("\\") &&
            !name.contains("..")
}
