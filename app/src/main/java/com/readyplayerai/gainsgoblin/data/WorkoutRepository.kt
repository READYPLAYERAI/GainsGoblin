package com.readyplayerai.gainsgoblin.data

import android.content.Context
import android.net.Uri
import com.readyplayerai.gainsgoblin.data.backup.BackupExportResult
import com.readyplayerai.gainsgoblin.data.backup.BackupService
import com.readyplayerai.gainsgoblin.data.backup.ImportMode
import com.readyplayerai.gainsgoblin.data.backup.ImportPreview
import com.readyplayerai.gainsgoblin.data.backup.ImportResult
import com.readyplayerai.gainsgoblin.data.db.RoomWorkoutStore
import com.readyplayerai.gainsgoblin.data.legacy.LegacyMigrator
import com.readyplayerai.gainsgoblin.data.legacy.MigrationResult
import com.readyplayerai.gainsgoblin.domain.applyPrFlags
import com.readyplayerai.gainsgoblin.domain.calculatePersonalRecords
import com.readyplayerai.gainsgoblin.domain.daysFromSets
import com.readyplayerai.gainsgoblin.domain.todayDateString
import com.readyplayerai.gainsgoblin.model.Exercise
import com.readyplayerai.gainsgoblin.model.WorkoutSet
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Single source of truth for persisted workout data.
 *
 * Room is the single source of truth: the repository observes the [RoomWorkoutStore]
 * flows and derives an observable [appState]. The one-time legacy migration runs on
 * construction; a failure is surfaced as [AppState.storageError] and the raw legacy
 * blobs are retained for recovery instead of being deleted.
 *
 * All I/O is suspend and runs on `Dispatchers.IO`; no blocking bridges are used.
 */
@Singleton
class WorkoutRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: RoomWorkoutStore,
    private val migrator: LegacyMigrator,
    private val backupService: BackupService
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var observationJob: Job? = null

    private val selectedDateFlow = MutableStateFlow(todayDateString())
    private val appStateFlow = MutableStateFlow(AppState(loaded = false))

    /** The current derived application state. */
    val appState: StateFlow<AppState> = appStateFlow.asStateFlow()

    /** The currently selected `yyyy-MM-dd` date. Mirrored inside [appState]. */
    val selectedDate: StateFlow<String> = selectedDateFlow.asStateFlow()

    // Raw legacy payloads retained when migration fails, so the user can still
    // export them even though they could not be decoded into Room.
    private var legacyRawWorkouts: String? = null
    private var legacyRawKnown: String? = null

    /** True when a failed migration retained raw legacy data for recovery export. */
    fun hasLegacyRecoveryData(): Boolean =
        legacyRawWorkouts != null || legacyRawKnown != null

    init {
        scope.launch {
            val result = try {
                migrator.migrateIfNeeded()
            } catch (e: Exception) {
                appStateFlow.value = AppState(loaded = true, storageError = e.toString())
                return@launch
            }
            when (result) {
                is MigrationResult.Failed -> {
                    legacyRawWorkouts = result.rawWorkouts
                    legacyRawKnown = result.rawKnown
                    appStateFlow.value = AppState(loaded = true, storageError = result.error)
                }
                else -> observeRoom()
            }
        }
    }

    private fun observeRoom() {
        observationJob?.cancel()
        observationJob = scope.launch {
            try {
                combine(store.sets, store.exercises) { sets, exercises ->
                    buildState(sets, exercises)
                }.collect { appStateFlow.value = it }
            } catch (e: Exception) {
                appStateFlow.value = appStateFlow.value.copy(
                    loaded = true,
                    storageError = e.toString()
                )
            }
        }
    }

    // ---------------------------------------------------------------------------------
    // State building
    // ---------------------------------------------------------------------------------

    /** Derives an [AppState] from one Room snapshot of raw sets and known exercises. */
    private fun buildState(sets: List<WorkoutSet>, known: List<Exercise>): AppState {
        val days = daysFromSets(sets)
        val personalRecords = calculatePersonalRecords(days, known)
        val flaggedDays = applyPrFlags(days, known)
        return AppState(
            loaded = true,
            workoutDays = flaggedDays,
            knownExercises = known,
            personalRecords = personalRecords,
            selectedDate = selectedDateFlow.value
        )
    }

    /** Builds a fresh [AppState] from one consistent Room snapshot. */
    private suspend fun currentState(): AppState {
        val (sets, known) = store.snapshot()
        return buildState(sets, known)
    }

    /**
     * Builds a fresh [AppState] straight from the latest Room snapshot.
     *
     * Unlike [appState], this never returns a cached/stale snapshot. Use after another
     * screen has persisted data.
     */
    suspend fun currentAppState(): AppState = currentState()

    // ---------------------------------------------------------------------------------
    // Date selection and lookups
    // ---------------------------------------------------------------------------------

    /**
     * Updates the selected date without touching persisted data.
     *
     * The freshness of the selection is also carried into the current [appState].
     */
    suspend fun selectDate(date: String) {
        selectedDateFlow.value = date
        appStateFlow.value = appStateFlow.value.copy(selectedDate = date)
    }

    // ---------------------------------------------------------------------------------
    // Workout mutations (Room is the source of truth; flows update [appState])
    // ---------------------------------------------------------------------------------

    suspend fun addSet(set: WorkoutSet) = store.addSet(set)

    suspend fun updateSet(date: String, setIndex: Int, reps: Double, weight: Double) =
        store.updateSet(date, setIndex, reps, weight)

    suspend fun updateSetNote(date: String, setIndex: Int, comment: String?) =
        store.updateSetNote(date, setIndex, comment)

    suspend fun removeSet(date: String, setIndex: Int) = store.removeSet(date, setIndex)

    suspend fun deleteDay(date: String) = store.deleteDay(date)

    suspend fun updateExerciseComment(date: String, exerciseName: String, comment: String?) =
        store.updateExerciseComment(date, exerciseName, comment)

    suspend fun deleteExercise(exerciseName: String) = store.deleteExercise(exerciseName)

    suspend fun editExercise(oldName: String, newName: String, newBodyPart: String?) =
        store.editExercise(oldName, newName, newBodyPart)

    suspend fun setFavorite(exerciseName: String, favorite: Boolean?) =
        store.setFavorite(exerciseName, favorite)

    suspend fun addExercise(name: String, category: String?): Boolean =
        store.addExercise(name, category)

    suspend fun clearWorkouts() = store.clearWorkouts()

    suspend fun clearAll() = store.clearAll()

    // ---------------------------------------------------------------------------------
    // Import / export
    // ---------------------------------------------------------------------------------

    suspend fun previewImport(uri: Uri): ImportPreview = backupService.previewFrom(uri)

    suspend fun importBackup(uri: Uri, mode: ImportMode): ImportResult =
        backupService.importFrom(uri, mode)

    suspend fun exportBackup(uri: Uri): BackupExportResult = backupService.exportTo(uri)

    suspend fun undoLastImport(): Boolean = backupService.undoLastImport()

    suspend fun lastImportBackupName(): String? = backupService.lastImportBackupName()

    // ---------------------------------------------------------------------------------
    // Migration recovery
    // ---------------------------------------------------------------------------------

    /**
     * Retries the one-time migration after a failed startup.
     *
     * On success the observer is (re)started; on failure the recovery payload is
     * refreshed and the error surfaced. Only called after a failed startup, so there
     * is no existing Room observer to duplicate.
     */
    suspend fun retryMigration(): Boolean = withContext(Dispatchers.IO) {
        try {
            when (val result = migrator.migrateIfNeeded()) {
                is MigrationResult.Failed -> {
                    legacyRawWorkouts = result.rawWorkouts
                    legacyRawKnown = result.rawKnown
                    appStateFlow.value = appStateFlow.value.copy(storageError = result.error)
                    false
                }
                else -> {
                    legacyRawWorkouts = null
                    legacyRawKnown = null
                    appStateFlow.value = appStateFlow.value.copy(storageError = null)
                    observeRoom()
                    true
                }
            }
        } catch (e: Exception) {
            appStateFlow.value = appStateFlow.value.copy(
                loaded = true,
                storageError = e.toString()
            )
            false
        }
    }

    /**
     * Writes the retained raw legacy payloads to [uri] for manual recovery.
     *
     * Returns false when nothing was retained or the write failed.
     */
    suspend fun exportLegacyRaw(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val text = "WORKOUTS\n" +
                (legacyRawWorkouts ?: "") +
                "\n\nKNOWN_EXERCISES\n" +
                (legacyRawKnown ?: "")
            context.contentResolver.openOutputStream(uri)?.use {
                it.write(text.toByteArray(Charsets.UTF_8))
            } ?: return@withContext false
            true
        } catch (e: Exception) {
            false
        }
    }
}
