package com.readyplayerai.gainsgoblin.data.backup

import java.util.Locale

object BackupDetector {

    fun detect(bytes: ByteArray): BackupFormat {
        var text = String(bytes, Charsets.UTF_8)
        if (text.startsWith('\uFEFF')) text = text.substring(1)
        val trimmed = text.trimStart()

        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return BackupFormat.GAINS_GOBLIN_JSON
        }

        val firstLine = trimmed
            .lineSequence()
            .firstOrNull { it.isNotBlank() }
            ?.lowercase(Locale.ROOT)
            ?: ""

        return if (firstLine.contains("weight (kgs)") || firstLine.contains("distance unit")) {
            BackupFormat.FITNOTES_CSV
        } else {
            BackupFormat.GAINS_GOBLIN_CSV
        }
    }
}
