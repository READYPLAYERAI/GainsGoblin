package com.readyplayerai.gainsgoblin.data.backup

import android.content.Context
import android.net.Uri
import com.readyplayerai.gainsgoblin.data.db.RoomWorkoutStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ImportPreview(
    val format: BackupFormat,
    val setCount: Int,
    val exerciseCount: Int,
    val warnings: List<String>
)

enum class ImportMode {
    MERGE,
    REPLACE
}

data class ImportResult(
    val format: BackupFormat,
    val mode: ImportMode,
    val setCount: Int,
    val exerciseCount: Int,
    val backupFileName: String,
    val warnings: List<String>
)

data class BackupExportResult(
    val format: BackupFormat,
    val setCount: Int,
    val exerciseCount: Int
)

@Singleton
class BackupService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: RoomWorkoutStore,
    private val fileStore: BackupFileStore
) {
    suspend fun previewFrom(uri: Uri): ImportPreview = withContext(Dispatchers.IO) {
        val bytes = readBytes(uri)
        val format = BackupDetector.detect(bytes)
        val parsed = parse(bytes, format)
        val exercises = mergeExercisesForImport(parsed.sets, parsed.exercises)
        ImportPreview(format, parsed.sets.size, exercises.size, parsed.warnings)
    }

    suspend fun importFrom(uri: Uri, mode: ImportMode): ImportResult = withContext(Dispatchers.IO) {
        val bytes = readBytes(uri)
        val format = BackupDetector.detect(bytes)
        val parsed = parse(bytes, format)
        val mergedExercises = mergeExercisesForImport(parsed.sets, parsed.exercises)
        val importedSets = parsed.sets.map { it.toWorkoutSet() }

        val (currentSets, currentExercises) = store.snapshot()
        val backupJson = JsonBackup.encode(BackupData(currentSets, currentExercises))
        val backupFile =
            fileStore.writePreImport(backupJson, "before-import", System.currentTimeMillis())
        fileStore.prune(3)

        store.applyImport(
            sets = importedSets,
            exercises = mergedExercises,
            replaceSets = mode == ImportMode.REPLACE,
            replaceExercises = false,
            lastImportBackupName = backupFile.name
        )

        ImportResult(
            format = format,
            mode = mode,
            setCount = importedSets.size,
            exerciseCount = mergedExercises.size,
            backupFileName = backupFile.name,
            warnings = parsed.warnings
        )
    }

    suspend fun exportTo(uri: Uri): BackupExportResult = withContext(Dispatchers.IO) {
        val (sets, exercises) = store.snapshot()
        val json = JsonBackup.encode(BackupData(sets, exercises))
        writeBytes(uri, json.toByteArray(Charsets.UTF_8))
        BackupExportResult(BackupFormat.GAINS_GOBLIN_JSON, sets.size, exercises.size)
    }

    suspend fun undoLastImport(): Boolean = withContext(Dispatchers.IO) {
        val name = store.meta("last_import_backup")?.takeIf { it.isNotBlank() }
            ?: return@withContext false
        val text = fileStore.readBackup(name) ?: return@withContext false
        val parsed = JsonBackup.decode(text)
        val sets = parsed.sets.map { it.toWorkoutSet() }
        store.applyImport(
            sets = sets,
            exercises = parsed.exercises,
            replaceSets = true,
            replaceExercises = true,
            lastImportBackupName = ""
        )
        true
    }

    suspend fun lastImportBackupName(): String? =
        store.meta("last_import_backup")?.takeIf { it.isNotBlank() }

    private fun readBytes(uri: Uri): ByteArray =
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IOException("Could not open backup file")

    private fun writeBytes(uri: Uri, bytes: ByteArray) {
        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
            ?: throw IOException("Could not write backup file")
    }

    private fun parse(bytes: ByteArray, format: BackupFormat): ParsedImport {
        val text = String(bytes, Charsets.UTF_8).removePrefix("\uFEFF")
        return when (format) {
            BackupFormat.GAINS_GOBLIN_CSV -> LegacyCsv.decode(text)
            BackupFormat.FITNOTES_CSV -> FitNotesCsv.decode(text)
            BackupFormat.GAINS_GOBLIN_JSON -> JsonBackup.decode(text)
        }
    }
}
