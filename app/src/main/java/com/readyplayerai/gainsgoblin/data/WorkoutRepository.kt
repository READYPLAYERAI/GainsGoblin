package com.readyplayerai.gainsgoblin.data

import android.content.ContentValues
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.readyplayerai.gainsgoblin.CSVFile
import com.readyplayerai.gainsgoblin.domain.applyPrFlags
import com.readyplayerai.gainsgoblin.domain.backfillExerciseFields
import com.readyplayerai.gainsgoblin.domain.calculatePersonalRecords
import com.readyplayerai.gainsgoblin.domain.daysFromSets
import com.readyplayerai.gainsgoblin.domain.dedupeKnownExercises
import com.readyplayerai.gainsgoblin.domain.defaultKnownExercises
import com.readyplayerai.gainsgoblin.domain.setsFromCsvRows
import com.readyplayerai.gainsgoblin.domain.todayDateString
import com.readyplayerai.gainsgoblin.domain.updateDay
import com.readyplayerai.gainsgoblin.model.Exercise
import com.readyplayerai.gainsgoblin.model.WorkoutDay
import com.readyplayerai.gainsgoblin.model.WorkoutExercise
import com.readyplayerai.gainsgoblin.model.WorkoutSet
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val WORKOUTS_KEY = stringPreferencesKey("workouts")
private val KNOWN_EXERCISES_KEY = stringPreferencesKey("known_exercises")
private const val TAG = "WorkoutRepository"
private const val LEGACY_PREFS_NAME = "shared preferences"

/**
 * Result of [WorkoutRepository.exportCsv].
 *
 * [message] carries the legacy user-facing text; [isError] says whether it is an
 * error message. The repository never shows toasts itself.
 */
data class ExportResult(val message: String, val isError: Boolean)

/**
 * Result of [WorkoutRepository.importCsv].
 *
 * [success] mirrors the legacy boolean; [error] carries the legacy `e.toString()`
 * text so the caller can toast it on failure.
 */
data class ImportCsvResult(val success: Boolean, val error: String? = null)

/**
 * Single source of truth for persisted workout data.
 *
 * Owns all DataStore JSON I/O and exposes an observable [appState].
 *
 * All I/O is suspend and runs on `Dispatchers.IO`; no blocking bridges are used.
 */
@Singleton
class WorkoutRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
    private val json: Json
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val selectedDateFlow = MutableStateFlow(todayDateString())
    private val appStateFlow = MutableStateFlow(AppState(loaded = false))

    /** The current derived application state. */
    val appState: StateFlow<AppState> = appStateFlow.asStateFlow()

    /** The currently selected `yyyy-MM-dd` date. Mirrored inside [appState]. */
    val selectedDate: StateFlow<String> = selectedDateFlow.asStateFlow()

    init {
        // Eagerly collect so [appState] is populated before any screen reads it.
        scope.launch {
            dataStore.data.collect { preferences ->
                appStateFlow.value = buildState(preferences)
            }
        }
    }

    // ---------------------------------------------------------------------------------
    // State building
    // ---------------------------------------------------------------------------------

    /**
     * Maps a raw DataStore emission into an [AppState].
     *
     * Performs the one-time SharedPreferences migration, decodes and repairs the
     * persisted workouts, resolves known exercises, computes PRs and applies PR
     * flags, then returns the result.
     */
    private suspend fun buildState(preferences: Preferences): AppState {
        val workoutsRaw = readStringWithMigration(preferences, WORKOUTS_KEY)
        val knownRaw = readStringWithMigration(preferences, KNOWN_EXERCISES_KEY)

        val daySerializer = ListSerializer(WorkoutDay.serializer())
        val rawDays: List<WorkoutDay> =
            if (workoutsRaw == null) ArrayList() else json.decodeFromString(daySerializer, workoutsRaw)

        // Port of repairWorkoutData() on load. Persisted day order is preserved.
        val days = ArrayList<WorkoutDay>(rawDays.size)
        for (day in rawDays) {
            days.add(backfillExerciseFields(day))
        }

        val exerciseSerializer = ListSerializer(Exercise.serializer())
        val decodedKnown: List<Exercise> =
            if (knownRaw == null) ArrayList() else json.decodeFromString(exerciseSerializer, knownRaw)
        val known = if (decodedKnown.isEmpty()) defaultKnownExercises() else ArrayList(decodedKnown)
        for (exercise in known) {
            if (exercise.favorite == null) {
                exercise.favorite = false
            }
        }

        val personalRecords = calculatePersonalRecords(days, known)
        val flaggedDays = applyPrFlags(days, known)

        val state = AppState(
            loaded = true,
            workoutDays = flaggedDays,
            knownExercises = known,
            personalRecords = personalRecords,
            selectedDate = selectedDateFlow.value
        )
        return state
    }

    /**
     * Legacy SharedPreferences migration, ported from `readStringWithMigration`.
     *
     * If the DataStore has no value for [key] but the legacy `"shared preferences"`
     * file does, the legacy value is written into DataStore and returned.
     */
    private suspend fun readStringWithMigration(
        preferences: Preferences,
        key: Preferences.Key<String>
    ): String? {
        val current = preferences[key]
        if (current != null) {
            return current
        }
        val legacyValue = context
            .getSharedPreferences(LEGACY_PREFS_NAME, MODE_PRIVATE)
            .getString(key.name, null)
        if (legacyValue != null) {
            dataStore.edit { it[key] = legacyValue }
        }
        return legacyValue
    }

    /**
     * Rebuilds the current state from the latest persisted DataStore value.
     *
     * Used by mutations so they always operate on fresh data regardless of how
     * current the cached [appState] is.
     */
    private suspend fun currentState(): AppState = buildState(dataStore.data.first())

    /**
     * Builds a fresh [AppState] straight from the latest persisted DataStore value.
     *
     * Unlike [appState], this never returns a cached/stale snapshot: it re-runs the
     * full state pipeline (decode, repair, PRs) on the current DataStore contents.
     * Use after another screen has persisted data.
     */
    suspend fun currentAppState(): AppState = buildState(dataStore.data.first())

    private fun encodeDays(days: List<WorkoutDay>): String =
        json.encodeToString(ListSerializer(WorkoutDay.serializer()), days)

    private fun encodeKnown(exercises: List<Exercise>): String =
        json.encodeToString(ListSerializer(Exercise.serializer()), exercises)

    // ---------------------------------------------------------------------------------
    // Mutation plumbing
    // ---------------------------------------------------------------------------------

    /** Which keys a [mutate] call should persist; null means "leave untouched". */
    private class MutationResult(
        val workoutDays: List<WorkoutDay>? = null,
        val knownExercises: List<Exercise>? = null
    )

    /**
     * Applies [transform] to the current state and persists the affected keys.
     *
     * The DataStore re-emission rebuilds [appState], so this helper never assigns
     * [appStateFlow] itself.
     */
    private suspend fun mutate(transform: (AppState) -> MutationResult) {
        withContext(Dispatchers.IO) {
            val current = currentState()
            val result = transform(current)
            dataStore.edit { preferences ->
                result.workoutDays?.let { preferences[WORKOUTS_KEY] = encodeDays(it) }
                result.knownExercises?.let { preferences[KNOWN_EXERCISES_KEY] = encodeKnown(it) }
            }
        }
    }

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

    /** Returns the index of the day with [date] in the current days list, or -1. */
    fun dayPosition(date: String): Int {
        val days = appState.value.workoutDays
        for (i in days.indices) {
            if (days[i].Date == date) {
                return i
            }
        }
        return -1
    }

    /** Returns the index of [exerciseName] within the day with [date], or -1. */
    fun exercisePosition(date: String, exerciseName: String): Int {
        val position = dayPosition(date)
        if (position == -1) {
            return -1
        }
        val exercises = appState.value.workoutDays[position].Exercises ?: return -1
        for (i in exercises.indices) {
            if (exercises[i].Exercise == exerciseName) {
                return i
            }
        }
        return -1
    }

    // ---------------------------------------------------------------------------------
    // Workout mutations
    // ---------------------------------------------------------------------------------

    /**
     * Appends [set] to its day, creating the day when missing.
     *
     * A day is matched by `Date == set.date`. The day's aggregates are recomputed
     * with [updateDay]; known exercises are not touched (legacy parity).
     */
    suspend fun addSet(set: WorkoutSet) = mutate { state ->
        val days = ArrayList(state.workoutDays)
        val index = days.indexOfFirst { it.Date == set.date }
        if (index >= 0) {
            val newSets = ArrayList(days[index].Sets ?: ArrayList())
            newSets.add(set)
            days[index] = dayWithSets(newSets)
        } else {
            val newDay = WorkoutDay()
            newDay.Sets = arrayListOf(set)
            days.add(updateDay(newDay))
        }
        MutationResult(workoutDays = days)
    }

    /**
     * Updates the reps and weight of one set, recomputing the day.
     *
     * The stored set is replaced by a fresh copy; the passed-in data is never used
     * to mutate shared instances.
     */
    suspend fun updateSet(date: String, setIndex: Int, reps: Double, weight: Double) = mutate { state ->
        val days = ArrayList(state.workoutDays)
        val index = days.indexOfFirst { it.Date == date }
        if (index >= 0) {
            val oldSets = days[index].Sets ?: ArrayList()
            if (setIndex in oldSets.indices) {
                val newSets = ArrayList(oldSets)
                newSets[setIndex] = copySet(oldSets[setIndex]).also {
                    it.reps = reps
                    it.weight = weight
                }
                days[index] = dayWithSets(newSets)
            }
        }
        MutationResult(workoutDays = days)
    }

    /**
     * Removes one set from its day, recomputing the day.
     *
     * A day left with no sets is kept in place with reset semantics (legacy parity).
     */
    suspend fun removeSet(date: String, setIndex: Int) = mutate { state ->
        val days = ArrayList(state.workoutDays)
        val index = days.indexOfFirst { it.Date == date }
        if (index >= 0) {
            val oldSets = days[index].Sets ?: ArrayList()
            if (setIndex in oldSets.indices) {
                val newSets = ArrayList(oldSets)
                newSets.removeAt(setIndex)
                days[index] = dayWithSets(newSets)
            }
        }
        MutationResult(workoutDays = days)
    }

    /** Removes the whole day with [date] from the persisted days. */
    suspend fun deleteDay(date: String) = mutate { state ->
        MutationResult(workoutDays = ArrayList(state.workoutDays.filterNot { it.Date == date }))
    }

    /**
     * Sets the comment on an exercise and on each of its sets.
     *
     * Ported from the legacy `saveComment`: both the day exercise's `Comment` and
     * every raw set comment for that exercise are updated so later recomputations
     * (for example an [addSet]) keep the comment.
     */
    suspend fun updateExerciseComment(date: String, exerciseName: String, comment: String?) = mutate { state ->
        val days = ArrayList(state.workoutDays)
        val index = days.indexOfFirst { it.Date == date }
        if (index >= 0) {
            val oldDay = days[index]
            val oldSets = oldDay.Sets ?: ArrayList()
            val oldExercises = oldDay.Exercises ?: ArrayList()

            val newSets = ArrayList<WorkoutSet>(oldSets.size)
            for (workoutSet in oldSets) {
                if (workoutSet.exerciseName == exerciseName) {
                    newSets.add(copySet(workoutSet).also { it.comment = comment })
                } else {
                    newSets.add(workoutSet)
                }
            }

            val newExercises = ArrayList<WorkoutExercise>(oldExercises.size)
            for (exercise in oldExercises) {
                if (exercise.Exercise == exerciseName) {
                    val copy = copyExercise(exercise)
                    copy.Comment = comment
                    copy.Sets = ArrayList(newSets.filter { it.exerciseName == exerciseName })
                    newExercises.add(copy)
                } else {
                    newExercises.add(exercise)
                }
            }

            val newDay = copyDay(oldDay)
            newDay.Sets = newSets
            newDay.Exercises = newExercises
            days[index] = newDay
        }
        MutationResult(workoutDays = days)
    }

    /**
     * Deletes [exerciseName] from every day and from the known list.
     *
     * Ported from the legacy `deleteExercise`: matching sets and day exercises are
     * removed, days left without sets are dropped, and both keys are persisted.
     */
    suspend fun deleteExercise(exerciseName: String) = mutate { state ->
        val days = ArrayList<WorkoutDay>()
        for (day in state.workoutDays) {
            val oldSets = day.Sets ?: ArrayList()
            val newSets = ArrayList(oldSets.filterNot { it.exerciseName == exerciseName })
            if (newSets.isEmpty()) {
                continue
            }
            val oldExercises = day.Exercises ?: ArrayList()
            val newExercises = ArrayList(oldExercises.filterNot { it.Exercise == exerciseName })
            val newDay = copyDay(day)
            newDay.Sets = newSets
            newDay.Exercises = newExercises
            days.add(newDay)
        }
        val known = ArrayList(state.knownExercises.filterNot { it.Name == exerciseName })
        MutationResult(workoutDays = days, knownExercises = known)
    }

    /**
     * Renames [oldName] to [newName] with [newBodyPart] everywhere.
     *
     * Ported from the legacy `editExercise`: known exercises, raw sets and day
     * exercises (including their nested sets) are renamed on fresh copies, and each
     * touched day is recomputed with [updateDay]. Both keys are persisted.
     */
    suspend fun editExercise(oldName: String, newName: String, newBodyPart: String?) = mutate { state ->
        val known = ArrayList<Exercise>(state.knownExercises.size)
        for (exercise in state.knownExercises) {
            if (exercise.Name == oldName) {
                val copy = Exercise(newName, newBodyPart)
                copy.favorite = exercise.favorite
                known.add(copy)
            } else {
                known.add(exercise)
            }
        }

        val days = ArrayList<WorkoutDay>(state.workoutDays.size)
        for (day in state.workoutDays) {
            val oldSets = day.Sets ?: ArrayList()
            val oldExercises = day.Exercises ?: ArrayList()
            var touched = false

            val newSets = ArrayList<WorkoutSet>(oldSets.size)
            for (workoutSet in oldSets) {
                if (workoutSet.exerciseName == oldName) {
                    touched = true
                    val copy = copySet(workoutSet)
                    copy.exerciseName = newName
                    copy.category = newBodyPart
                    newSets.add(copy)
                } else {
                    newSets.add(workoutSet)
                }
            }

            val newExercises = ArrayList<WorkoutExercise>(oldExercises.size)
            for (exercise in oldExercises) {
                if (exercise.Exercise == oldName) {
                    touched = true
                    val copy = copyExercise(exercise)
                    copy.Exercise = newName
                    copy.Sets = ArrayList(
                        newSets.filter { it.exerciseName == newName }
                    )
                    newExercises.add(copy)
                } else {
                    newExercises.add(exercise)
                }
            }

            if (touched) {
                val temp = copyDay(day)
                temp.Sets = newSets
                temp.Exercises = newExercises
                days.add(updateDay(temp))
            } else {
                days.add(copyDay(day))
            }
        }

        MutationResult(workoutDays = days, knownExercises = known)
    }

    /** Sets [favorite] on every known exercise named [exerciseName]. */
    suspend fun setFavorite(exerciseName: String, favorite: Boolean?) = mutate { state ->
        val known = ArrayList<Exercise>(state.knownExercises.size)
        for (exercise in state.knownExercises) {
            if (exercise.Name == exerciseName) {
                val copy = Exercise(exercise.Name, exercise.BodyPart)
                copy.favorite = favorite
                known.add(copy)
            } else {
                known.add(exercise)
            }
        }
        MutationResult(knownExercises = known)
    }

    /**
     * Appends a new known exercise. Returns false when an exercise with [name]
     * already exists (legacy `doesExerciseExist` gate). Persists the known list.
     */
    suspend fun addExercise(name: String, category: String?): Boolean {
        var added = false
        mutate { state ->
            if (state.knownExercises.any { it.Name == name }) {
                added = false
                MutationResult()
            } else {
                val known = ArrayList(state.knownExercises)
                known.add(Exercise(name, category))
                added = true
                MutationResult(knownExercises = known)
            }
        }
        return added
    }

    /** Clears workouts only; known exercises stay (Settings "clear" parity). */
    suspend fun clearWorkouts() = mutate {
        MutationResult(workoutDays = emptyList())
    }

    /** Clears workouts AND known exercises (legacy `clearDataStructures` parity). */
    suspend fun clearAll() = mutate {
        MutationResult(workoutDays = emptyList(), knownExercises = emptyList())
    }

    // ---------------------------------------------------------------------------------
    // Import / export
    // ---------------------------------------------------------------------------------

    /**
     * Imports sets from the CSV at [uri].
     *
     * Ported from the legacy `readFile`. Returns a successful [ImportCsvResult]
     * on success. On any failure the repository clears everything (legacy parity)
     * and returns a failed result carrying `e.toString()` so the caller can toast.
     */
    suspend fun importCsv(uri: Uri): ImportCsvResult = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IOException("Could not open input stream for $uri")
            val rows = CSVFile(inputStream).read()
            val sets = setsFromCsvRows(rows)
            val days = daysFromSets(sets)
            val known = dedupeKnownExercises(days, currentState().knownExercises)
            dataStore.edit { preferences ->
                preferences[WORKOUTS_KEY] = encodeDays(days)
                preferences[KNOWN_EXERCISES_KEY] = encodeKnown(known)
            }
            ImportCsvResult(success = true)
        } catch (e: Exception) {
            clearAll()
            ImportCsvResult(success = false, error = e.toString())
        }
    }

    /**
     * Exports a CSV backup of the current workouts.
     *
     * Ported from the legacy `writeFile`, minus toasts. Returns the legacy message
     * text and whether it represents an error.
     */
    suspend fun exportCsv(): ExportResult = withContext(Dispatchers.IO) {
        val fileName = "gainsgoblin" + SimpleDateFormat("_yyyy-MM-dd_HH:mm:ss").format(Date())
        try {
            var outputStream: OutputStream? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues()
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                values.put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOCUMENTS + "/Gains Goblin"
                )
                val extVolumeUri = MediaStore.Files.getContentUri("external")
                val fileUri = context.contentResolver.insert(extVolumeUri, values)
                outputStream = context.contentResolver.openOutputStream(fileUri!!)
            } else {
                val root = File(
                    Environment.getExternalStorageDirectory().toString() + File.separator + "DIRECTORY_NAME",
                    "images"
                )
                val file = File(root, fileName)
                Log.d(TAG, "saveFile: file path - " + file.absolutePath)
                outputStream = FileOutputStream(file)
            }
            outputStream!!.write("Date,Exercise,Category,Weight (kg),Reps,Comment\n".toByteArray())

            val days = currentState().workoutDays
            for (i in days.indices) {
                val exercises = days[i].Exercises!!
                for (j in exercises.indices) {
                    val exerciseComment = exercises[j].Comment
                    val sets = exercises[j].Sets!!
                    for (k in sets.indices) {
                        val date = exercises[j].Date!!
                        val exerciseName = sets[k].exerciseName!!
                        val exerciseCategory = sets[k].category!!
                        val weight = sets[k].weight!!
                        val reps = sets[k].reps!!
                        outputStream!!.write(
                            (
                                date + "," + exerciseName + "," + exerciseCategory + "," +
                                    weight + "," + reps + "," + exerciseComment + "\n"
                                ).toByteArray()
                        )
                    }
                }
            }
            outputStream!!.close()
            ExportResult(
                "Backup saved in " + Environment.DIRECTORY_DOCUMENTS + "/Gains Goblin",
                false
            )
        } catch (e: Exception) {
            ExportResult(e.toString(), true)
        }
    }

    // ---------------------------------------------------------------------------------
    // Copy helpers (never mutate their inputs)
    // ---------------------------------------------------------------------------------

    /** Creates a fresh day whose `Sets` are [sets], recomputed by [updateDay]. */
    private fun dayWithSets(sets: ArrayList<WorkoutSet>): WorkoutDay {
        val source = WorkoutDay()
        source.Sets = sets
        return updateDay(source)
    }

    private fun copyDay(source: WorkoutDay): WorkoutDay {
        val copy = WorkoutDay()
        copy.Sets = source.Sets
        copy.Exercises = source.Exercises
        copy.DayVolume = source.DayVolume
        copy.Date = source.Date
        copy.Reps = source.Reps
        return copy
    }

    private fun copySet(source: WorkoutSet): WorkoutSet {
        val copy = WorkoutSet()
        copy.date = source.date
        copy.exerciseName = source.exerciseName
        copy.category = source.category
        copy.reps = source.reps
        copy.weight = source.weight
        copy.comment = source.comment
        copy.id = source.id
        copy.user_id = source.user_id
        return copy
    }

    private fun copyExercise(source: WorkoutExercise): WorkoutExercise {
        val copy = WorkoutExercise()
        copy.Sets = source.Sets
        copy.Date = source.Date
        copy.Exercise = source.Exercise
        copy.Volume = source.Volume
        copy.EstimatedOneRepMax = source.EstimatedOneRepMax
        copy.ActualOneRepMax = source.ActualOneRepMax
        copy.MaxWeight = source.MaxWeight
        copy.MaxReps = source.MaxReps
        copy.TotalReps = source.TotalReps
        copy.TotalSets = source.TotalSets
        copy.MaxSetVolume = source.MaxSetVolume
        copy.VolumePR = source.VolumePR
        copy.ActualOneRepMaxPR = source.ActualOneRepMaxPR
        copy.EstimatedOneRepMaxPR = source.EstimatedOneRepMaxPR
        copy.MaxRepsPR = source.MaxRepsPR
        copy.MaxWeightPR = source.MaxWeightPR
        copy.HTLT = source.HTLT
        copy.Comment = source.Comment
        copy.maxVolumeSet = source.maxVolumeSet
        copy.maxRepsSet = source.maxRepsSet
        copy.maxWeightSet = source.maxWeightSet
        return copy
    }
}
