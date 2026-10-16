package com.readyplayerai.gainsgoblin

import android.content.ContentValues
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.Pair
import android.widget.Toast

import com.readyplayerai.gainsgoblin.model.Exercise
import com.readyplayerai.gainsgoblin.model.WorkoutDay
import com.readyplayerai.gainsgoblin.model.WorkoutExercise
import com.readyplayerai.gainsgoblin.model.WorkoutSet
import com.readyplayerai.gainsgoblin.ui.MainActivity

import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import kotlin.collections.MutableList

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Collections
import java.util.Comparator
import java.util.Date
import java.util.HashMap
import java.util.Iterator
import java.util.List
import java.util.Set
import java.util.TreeSet
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit

private val Context.dataStore by preferencesDataStore(name = "gainsgoblin_data")
private val WORKOUTS_KEY = stringPreferencesKey("workouts")
private val KNOWN_EXERCISES_KEY = stringPreferencesKey("known_exercises")

// NOTE: Deliberate deviation from Java source:
// Java had `import static androidx.constraintlayout.motion.utils.Oscillator.TAG`
// which is an internal Android constant misused as a log tag.
// Replaced with a private constant in this class.
class DataStorage {

    private val TAG = "DataStorage"

    private val json = Json {
        explicitNulls = false
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    var days = TreeSet<String>()
    var sets: ArrayList<WorkoutSet> = ArrayList()
    var workoutDays: ArrayList<WorkoutDay> = ArrayList()
    var knownExercises: ArrayList<Exercise> = ArrayList()
    var volumePRs: HashMap<String, Double> = HashMap()
    var setVolumePRs: HashMap<String, Pair<Double, Double>> = HashMap()
    var actualOneRepMaxPRs: HashMap<String, Double> = HashMap()
    var estimatedOneRMPRs: HashMap<String, Double> = HashMap()
    var maxRepsPRs: HashMap<String, Double> = HashMap()
    var maxWeightPRs: HashMap<String, Double> = HashMap()
    var lastTimeVolume: HashMap<String, Double> = HashMap()
    var infiniteWorkoutDays: ArrayList<WorkoutDay> = ArrayList()
    var maxVolumeSetPRs: HashMap<String, WorkoutSet> = HashMap()
    var maxRepsSetPRs: HashMap<String, WorkoutSet> = HashMap()
    var maxWeightSetPRs: HashMap<String, WorkoutSet> = HashMap()

    // Synchronous bridge over DataStore's Flow API; the planned MVVM refactor will replace runBlocking with Flow consumption.
    private fun readStringWithMigration(context: Context, key: Preferences.Key<String>): String? {
        val legacy = context.getSharedPreferences("shared preferences", MODE_PRIVATE)
        return runBlocking {
            val current = context.dataStore.data.first()[key]
            if (current != null) {
                current
            } else {
                val legacyValue = legacy.getString(key.name, null)
                if (legacyValue != null) {
                    context.dataStore.edit { it[key] = legacyValue }
                }
                legacyValue
            }
        }
    }

    // Returns index of day
    fun getDayPosition(Date: String?): Int {
        for (i in 0 until workoutDays.size) {
            if (workoutDays[i].Date == Date) {
                return i
            }
        }
        return -1
    }

    // Returns index of exercise
    fun getExercisePosition(Date: String?, exerciseName: String?): Int {
        val day_position = getDayPosition(Date)

        // The day doesn't even have an exercise
        if (day_position == -1) {
            return -1
        }

        val Exercises = workoutDays[day_position].Exercises!!

        for (i in 0 until Exercises.size) {
            if (Exercises[i].Exercise == exerciseName) {
                return i
            }
        }
        return -1
    }

    // Converts CSV file to Internally used Data Structure
    fun csvToSets(csvList: MutableList<Array<String>>) {
        // Remove potential Duplicates
        sets.clear()

        // i = 1 since first row is only Strings
        for (i in 1 until csvList.size) {
            val row = csvList[i]

            val Date = row[0]
            val Exercise = row[1]
            val Category = row[2]
            val Reps = row[3]
            val Weight = row[4]

            var Comment = ""

            if (row.size == 6) {
                Comment = row[5]
            }

            val workoutSet = WorkoutSet(Date, Exercise, Category, java.lang.Double.parseDouble(Weight), java.lang.Double.parseDouble(Reps), Comment)
            sets.add(workoutSet)
        }
    }

    // Updates All other Data Structures
    fun setsToEverything() {
        // Clear Data Structures
        days.clear()
        workoutDays.clear()

        // i = 1 since first row is only Strings
        for (i in 0 until sets.size) {
            days.add(sets[i].date!!)
        }

        val it = days.iterator()

        // Construct Workout_Days Array List
        while (it.hasNext()) {
            val Date = it.next()
            val temp_day = WorkoutDay()

            for (i in 0 until sets.size) {
                // If Date matches add Set Object to Workout_Day Object
                if (Date == sets[i].date) {
                    temp_day.addSet(sets[i])
                }
            }
            workoutDays.add(temp_day)
        }
    }

    // Initialized KnownExercises ArrayList with some hardcoded exercises
    fun initKnownExercises() {
        knownExercises.clear()
        // Some hardcoded Exercises
        knownExercises.add(Exercise("Flat Barbell Bench Press", "Chest"))
        knownExercises.add(Exercise("Incline Barbell Bench Press", "Chest"))
        knownExercises.add(Exercise("Decline Barbell Bench Press", "Chest"))
        knownExercises.add(Exercise("Flat Dumbbell Bench Press", "Chest"))
        knownExercises.add(Exercise("Incline Dumbbell Bench Press", "Chest"))
        knownExercises.add(Exercise("Decline Dumbbell Bench Press", "Chest"))
        knownExercises.add(Exercise("Chin Up", "Back"))
        knownExercises.add(Exercise("Seated Dumbbell Press", "Shoulders"))
        knownExercises.add(Exercise("Ring Dip", "Chest"))
        knownExercises.add(Exercise("Lateral Cable Raise", "Shoulders"))
        knownExercises.add(Exercise("Lateral Dumbbell Raise", "Shoulders"))
        knownExercises.add(Exercise("Barbell Curl", "Biceps"))
        knownExercises.add(Exercise("Tricep Extension", "Triceps"))
        knownExercises.add(Exercise("Squat", "Legs"))
        knownExercises.add(Exercise("Leg Extension", "Legs"))
        knownExercises.add(Exercise("Hammstring Leg Curl", "Legs"))
        knownExercises.add(Exercise("Deadlift", "Back"))
        knownExercises.add(Exercise("Sumo Deadlift", "Back"))
        knownExercises.add(Exercise("Seated Machine Chest Press", "Chest"))
        knownExercises.add(Exercise("Seated Machine Shoulder Press", "Shoulders"))
        knownExercises.add(Exercise("Seated Calf Raise", "Legs"))
        knownExercises.add(Exercise("Donkey Calf Raise", "Legs"))
        knownExercises.add(Exercise("Standing Calf Raise", "Legs"))
        knownExercises.add(Exercise("Seated Machine Curl", "Biceps"))
        knownExercises.add(Exercise("Lat Pulldown", "Back"))
        knownExercises.add(Exercise("Pull Up", "Back"))
        knownExercises.add(Exercise("Push Up", "Chest"))
        knownExercises.add(Exercise("Leg Press", "Legs"))
        knownExercises.add(Exercise("Push Press", "Shoulders"))
        knownExercises.add(Exercise("Dumbbell Curl", "Biceps"))
        knownExercises.add(Exercise("Decline Hammer Strength Chest Press", "Chest"))
        knownExercises.add(Exercise("Leg Extension Machine", "Legs"))
        knownExercises.add(Exercise("Seated Calf Raise Machine", "Legs"))
        knownExercises.add(Exercise("Lying Triceps Extension", "Triceps"))
        knownExercises.add(Exercise("Cable Curl", "Biceps"))
        knownExercises.add(Exercise("Hammer Strength Shoulder Press", "Shoulders"))
    }

    fun setFavoriteExercise(exerciseName: String?, isFavorite: Boolean?) {
        // Initialize Volume Record Hashmap
        for (i in 0 until knownExercises.size) {
            if (knownExercises[i].Name == exerciseName) {
                knownExercises[i].favorite = isFavorite
            }
        }
    }

    // Calculate all Volume Personal Records from scratch
    fun calculatePersonalRecords() {
        volumePRs.clear()
        setVolumePRs.clear()
        actualOneRepMaxPRs.clear()
        estimatedOneRMPRs.clear()
        maxRepsPRs.clear()
        maxWeightPRs.clear()
        lastTimeVolume.clear()

        // Initialize Volume Record Hashmap
        for (i in 0 until knownExercises.size) {
            volumePRs[knownExercises[i].Name!!] = 0.0
            setVolumePRs[knownExercises[i].Name!!] = Pair(0.0, 0.0)
            actualOneRepMaxPRs[knownExercises[i].Name!!] = 0.0
            estimatedOneRMPRs[knownExercises[i].Name!!] = 0.0
            maxRepsPRs[knownExercises[i].Name!!] = 0.0
            maxWeightPRs[knownExercises[i].Name!!] = 0.0
            lastTimeVolume[knownExercises[i].Name!!] = 0.0
        }

        // Calculate Volume PRs
        for (i in 0 until knownExercises.size) {
            for (j in 0 until workoutDays.size) {
                val exercises = workoutDays[j].Exercises ?: continue
                for (k in 0 until exercises.size) {
                    if (exercises[k].Exercise == knownExercises[i].Name) {
                        // Per Exercise Volume Personal Records
                        if (volumePRs[knownExercises[i].Name!!]!! < exercises[k].Volume ?: 0.0) {
                            exercises[k].VolumePR = true
                            volumePRs[knownExercises[i].Name!!] = exercises[k].Volume ?: 0.0
                        }

                        val setVolume = setVolumePRs[knownExercises[i].Name!!]!!.first * setVolumePRs[knownExercises[i].Name!!]!!.second

                        // Per Set Volume Personal Records
                        if (setVolume < exercises[k].MaxSetVolume ?: 0.0) {
                            val maxReps = exercises[k].MaxReps ?: 0.0
                            val maxWeight = exercises[k].MaxWeight ?: 0.0

                            val pair = Pair(maxReps, maxWeight)

                            setVolumePRs[knownExercises[i].Name!!] = pair

                            val temp_set = WorkoutSet()
                            temp_set.reps = maxReps
                            temp_set.weight = maxWeight
                            maxVolumeSetPRs[knownExercises[i].Name!!] = temp_set
                        }

                        // Actual One Repetition Maximum
                        if (actualOneRepMaxPRs[knownExercises[i].Name!!]!! < exercises[k].ActualOneRepMax ?: 0.0) {
                            exercises[k].ActualOneRepMaxPR = true
                            actualOneRepMaxPRs[knownExercises[i].Name!!] = exercises[k].ActualOneRepMax ?: 0.0
                        }

                        // Estimated One Repetition Maximum
                        if (estimatedOneRMPRs[knownExercises[i].Name!!]!! < exercises[k].EstimatedOneRepMax ?: 0.0) {
                            exercises[k].EstimatedOneRepMaxPR = true
                            estimatedOneRMPRs[knownExercises[i].Name!!] = exercises[k].EstimatedOneRepMax ?: 0.0
                        }

                        // Max Repetitions Personal Records
                        if (maxRepsPRs[knownExercises[i].Name!!]!! < exercises[k].MaxReps ?: 0.0) {
                            exercises[k].MaxRepsPR = true
                            maxRepsPRs[knownExercises[i].Name!!] = exercises[k].MaxReps ?: 0.0
                            exercises[k].maxRepsSet?.let { maxRepsSetPRs[knownExercises[i].Name!!] = it }
                        }

                        // Max Weight Personal Records
                        if (maxWeightPRs[knownExercises[i].Name!!]!! < exercises[k].MaxWeight ?: 0.0) {
                            exercises[k].MaxWeightPR = true
                            maxWeightPRs[knownExercises[i].Name!!] = exercises[k].MaxWeight ?: 0.0
                            exercises[k].maxWeightSet?.let { maxWeightSetPRs[knownExercises[i].Name!!] = it }
                        }

                        // Harder Than Last Time!
                        if (lastTimeVolume[knownExercises[i].Name!!]!! < exercises[k].Volume ?: 0.0) {
                            exercises[k].HTLT = true
                            lastTimeVolume[knownExercises[i].Name!!] = exercises[k].Volume ?: 0.0
                        }
                        // This needs to be updates since we are dealing with last time and not overall maximums
                        else {
                            lastTimeVolume[knownExercises[i].Name!!] = exercises[k].Volume ?: 0.0
                        }
                    }
                }
            }
        }
    }

    // Saves Workout_Days Array List in DataStore (migrated from SharedPreferences)
    // For some reason when I pass the context it works so let's roll with it :D
    fun saveWorkoutData(ct: Context) {
        val listSerializer = ListSerializer(WorkoutDay.serializer())
        val jsonStr = json.encodeToString(listSerializer, workoutDays)
        runBlocking {
            ct.dataStore.edit { it[WORKOUTS_KEY] = jsonStr }
        }
    }

    // Backfills null aggregation fields on persisted WorkoutExercise entries
    // from the day's raw Sets. Repairs data persisted by older versions or
    // imported from partial backups where these fields were absent.
    fun repairWorkoutData() {
        for (day in workoutDays) {
            val rawSets = day.Sets ?: continue
            val exercises = day.Exercises ?: continue
            for (ex in exercises) {
                val exSets = rawSets.filter { it.exerciseName == ex.Exercise }
                if (exSets.isEmpty()) continue
                if (ex.Volume == null) ex.Volume = exSets.sumOf { (it.reps ?: 0.0) * (it.weight ?: 0.0) }
                if (ex.TotalReps == null) ex.TotalReps = exSets.sumOf { it.reps ?: 0.0 }
                if (ex.TotalSets == null) ex.TotalSets = exSets.size.toDouble()
                if (ex.MaxWeight == null) ex.MaxWeight = exSets.maxOf { it.weight ?: 0.0 }
                if (ex.MaxReps == null) ex.MaxReps = exSets.maxOf { it.reps ?: 0.0 }
                if (ex.MaxSetVolume == null) ex.MaxSetVolume = exSets.maxOf { (it.reps ?: 0.0) * (it.weight ?: 0.0) }
                if (ex.EstimatedOneRepMax == null) ex.EstimatedOneRepMax = exSets.maxOf { (it.weight ?: 0.0) * (1 + (it.reps ?: 0.0) / 30.0) }
                if (ex.ActualOneRepMax == null) ex.ActualOneRepMax = exSets.filter { it.reps == 1.0 }.maxOfOrNull { it.weight ?: 0.0 } ?: 0.0
                if (ex.Date == null) ex.Date = exSets.first().date
                if (ex.maxWeightSet == null) ex.maxWeightSet = exSets.maxByOrNull { it.weight ?: 0.0 }
                if (ex.maxRepsSet == null) ex.maxRepsSet = exSets.maxByOrNull { it.reps ?: 0.0 }
                if (ex.maxVolumeSet == null) ex.maxVolumeSet = exSets.maxByOrNull { (it.reps ?: 0.0) * (it.weight ?: 0.0) }
            }
        }
    }

    // Loads Workout_Days Array List from DataStore (with migration from SharedPreferences)
    fun loadWorkoutData(context: Context) {
        if (workoutDays.isEmpty()) {
            val rawJson = readStringWithMigration(context, WORKOUTS_KEY)
            val listSerializer = ListSerializer(WorkoutDay.serializer())
            // If there are no previously saved entries make a new object
            val decoded = if (rawJson == null) ArrayList() else ArrayList(json.decodeFromString(listSerializer, rawJson))
            workoutDays = decoded
            repairWorkoutData()
        }
    }

    // Saves known exercises in DataStore (migrated from SharedPreferences)
    // For some reason when I pass the context it works so let's roll with it :D
    fun saveKnownExerciseData(ct: Context) {
        val listSerializer = ListSerializer(Exercise.serializer())
        val jsonStr = json.encodeToString(listSerializer, knownExercises)
        runBlocking {
            ct.dataStore.edit { it[KNOWN_EXERCISES_KEY] = jsonStr }
        }
    }

    // Loads known exercises from DataStore (with migration from SharedPreferences)
    fun loadKnownExercisesData(context: Context) {
        if (knownExercises.isEmpty()) {
            val rawJson = readStringWithMigration(context, KNOWN_EXERCISES_KEY)
            val listSerializer = ListSerializer(Exercise.serializer())
            // If there are no previously saved entries make a new object
            val decoded = if (rawJson == null) ArrayList() else ArrayList(json.decodeFromString(listSerializer, rawJson))
            knownExercises = decoded
            if (knownExercises.isEmpty()) {
                knownExercises = ArrayList()
                initKnownExercises()
            }
        }

        // Those who have previously saved entries will have null in this case
        for (i in 0 until knownExercises.size) {
            if (knownExercises[i].favorite == null) {
                knownExercises[i].favorite = false
            }
        }
    }

    // Read CSV from internal storage
    fun readFile(uri: Uri, context: Context): Boolean {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)!!
            val csvFile = CSVFile(inputStream)
            val csvList = csvFile.read()

            // Here is where the magic happens
            csvToSets(csvList) // Read File and Construct Local Objects
            setsToEverything() // Convert Set Objects to Day Objects
            csvToKnownExercises() // Find all Exercises in CSV and add them to known exercises
            saveKnownExerciseData(context) // Save KnownExercises in CSV
            saveWorkoutData(context) // Save WorkoutDays in DataStore
            return true
        } catch (e: IOException) {
            println(e.message)
            Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show()
            Toast.makeText(context, "Could not locate file " + uri.path, Toast.LENGTH_SHORT).show()
            clearDataStructures(context)
            return false
        }
    }

    fun readFromSets(sets: ArrayList<WorkoutSet>, context: Context) {
        clearDataStructures(context)

        this.sets = sets
        initKnownExercises()
        modifySets() // bad design requires this
        setsToEverything()
        csvToKnownExercises()
    }

    fun modifySets() {
        for (i in 0 until sets.size) {
            val date = sets[i].date!!.substring(0, 10)
            sets[i].date = date
        }
    }

    // Export backup function using Storage Access Framework
    fun writeFile(context: Context) {
        val fileName = MainActivity.EXPORT_FILENAME

        try {
            var outputStream: OutputStream? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues()
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName) // file name required to contain extestion file mime
                values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/Gains Goblin") //DIRECTORY
                val extVolumeUri = MediaStore.Files.getContentUri("external")
                val fileUri = context.contentResolver.insert(extVolumeUri, values)
                outputStream = context.contentResolver.openOutputStream(fileUri!!)
            } else {
                val root = File(Environment.getExternalStorageDirectory().toString() + File.separator + "DIRECTORY_NAME", "images")
                val file = File(root, fileName)
                Log.d(TAG, "saveFile: file path - " + file.absolutePath)
                outputStream = FileOutputStream(file)
            }
            outputStream!!.write("Date,Exercise,Category,Weight (kg),Reps,Comment\n".toByteArray())

            for (i in 0 until workoutDays.size) {
                for (j in 0 until workoutDays[i].Exercises!!.size) {
                    val exerciseComment = workoutDays[i].Exercises!![j].Comment
                    for (k in 0 until workoutDays[i].Exercises!![j].Sets!!.size) {
                        val Date = workoutDays[i].Exercises!![j].Date!!
                        val exerciseName = workoutDays[i].Exercises!![j].Sets!![k].exerciseName!!
                        val exerciseCategory = workoutDays[i].Exercises!![j].Sets!![k].category!!
                        val Weight = workoutDays[i].Exercises!![j].Sets!![k].weight!!
                        val Reps = workoutDays[i].Exercises!![j].Sets!![k].reps!!
                        outputStream!!.write((Date + "," + exerciseName + "," + exerciseCategory + "," + Weight + "," + Reps + "," + exerciseComment + "\n").toByteArray())
                    }
                }
            }
            outputStream!!.close()
            Toast.makeText(context, "Backup saved in " + Environment.DIRECTORY_DOCUMENTS + "/Gains Goblin", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            println(e.toString())
            Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show()
        }
    }

    // Clears all locally used data structures
    fun clearDataStructures(context: Context) {
        // Clear everything just in case
        workoutDays.clear()
        knownExercises.clear() // This removes all known exercises
        sets.clear()
        days.clear()
        saveWorkoutData(context)
        saveKnownExerciseData(context)
    }

    // Inefficient bubble sort but does the job
    fun sortWorkoutDaysDate() {
        Collections.sort(workoutDays, object : Comparator<WorkoutDay> {
            override fun compare(workoutDay: WorkoutDay, t1: WorkoutDay): Int {
                val date1 = workoutDay.Date
                val date2 = t1.Date
                var date_object1 = Date()
                var date_object2 = Date()

                try {
                    date_object1 = SimpleDateFormat("yyyy-MM-dd").parse(date1!!)!!
                    date_object2 = SimpleDateFormat("yyyy-MM-dd").parse(date2!!)!!
                } catch (e: Exception) {
                    println(e.message)
                }
                return date_object1.compareTo(date_object2)
            }
        })
    }

    // Just Check if an exercise is known
    fun doesExerciseExist(exercise_name: String?): Boolean {
        for (i in 0 until knownExercises.size) {
            if (knownExercises[i].Name == exercise_name) {
                return true
            }
        }
        return false
    }

    // Returns the exercise category if exists, else it returns an empty string
    fun getExerciseCategory(Exercise: String?): String? {
        for (i in 0 until knownExercises.size) {
            if (knownExercises[i].Name == Exercise) {
                return knownExercises[i].BodyPart
            }
        }
        return ""
    }

    // Returns the exercise category if exists, else it returns an empty string
    fun isExerciseFavorite(Exercise: String?): Boolean? {
        for (i in 0 until knownExercises.size) {
            if (knownExercises[i].Name == Exercise) {
                return knownExercises[i].favorite
            }
        }
        return false
    }

    // Deletes exercise from KnownExercises and from WorkoutDays
    fun deleteExercise(exercise_name: String?) {
        // Iterate Workout Days
        val dayIterator = workoutDays.iterator()
        while (dayIterator.hasNext()) {
            val currentDay = dayIterator.next()

            // Iterate Workout Exercises
            val exerciseIterator = currentDay.Exercises!!.iterator()
            while (exerciseIterator.hasNext()) {
                val current_exercise = exerciseIterator.next()
                if (current_exercise.Exercise == exercise_name) {
                    exerciseIterator.remove()
                }
            }

            // Iterate Workout Sets
            val setIterator = currentDay.Sets!!.iterator()
            while (setIterator.hasNext()) {
                val current_set = setIterator.next()
                if (current_set.exerciseName == exercise_name) {
                    setIterator.remove()
                }
            }

            if (currentDay.Sets!!.isEmpty()) {
                dayIterator.remove()
            }
        }

        // Iterate Known Exercises data structure
        val exerciseIterator2 = knownExercises.iterator()
        while (exerciseIterator2.hasNext()) {
            val current_exercise = exerciseIterator2.next()
            if (current_exercise.Name == exercise_name) {
                exerciseIterator2.remove()
            }
        }
    }

    // Returns all the sets to be deleted if we were to run deleteExercise
    fun deleteExerciseGetSets(exercise_name: String?): ArrayList<WorkoutSet> {
        val to_be_delete_sets: ArrayList<WorkoutSet> = ArrayList()

        // Iterate Workout Days
        val dayIterator = workoutDays.iterator()
        while (dayIterator.hasNext()) {
            val currentDay = dayIterator.next()

            // Iterate Workout Sets
            val setIterator = currentDay.Sets!!.iterator()
            while (setIterator.hasNext()) {
                val current_set = setIterator.next()
                if (current_set.exerciseName == exercise_name) {
                    to_be_delete_sets.add(current_set)
                }
            }
        }
        return to_be_delete_sets
    }

    // Changes exercise name and body part
    fun editExercise(exercise_name: String?, new_exercise_name: String?, new_exercise_bodypart: String?) {
        for (i in 0 until knownExercises.size) {
            if (knownExercises[i].Name == exercise_name) {
                knownExercises[i].Name = new_exercise_name
                knownExercises[i].BodyPart = new_exercise_bodypart
            }
        }

        for (i in 0 until workoutDays.size) {
            for (j in 0 until workoutDays[i].Sets!!.size) {
                if (workoutDays[i].Sets!![j].exerciseName == exercise_name) {
                    workoutDays[i].Sets!![j].exerciseName = new_exercise_name
                    workoutDays[i].Sets!![j].category = new_exercise_bodypart
                }
            }

            for (j in 0 until workoutDays[i].Exercises!!.size) {
                if (workoutDays[i].Exercises!![j].Exercise == exercise_name) {
                    workoutDays[i].Exercises!![j].Exercise = new_exercise_name

                    for (k in 0 until workoutDays[i].Exercises!![j].Sets!!.size) {
                        if (workoutDays[i].Exercises!![j].Sets!![k].exerciseName == exercise_name) {
                            workoutDays[i].Exercises!![j].Sets!![k].exerciseName = new_exercise_name
                            workoutDays[i].Exercises!![j].Sets!![k].category = new_exercise_bodypart
                        }
                    }
                }
            }
        }
    }

    // Changes exercise name and body part
    fun editExerciseGetSets(exercise_name: String?, new_exercise_name: String?, new_exercise_bodypart: String?): ArrayList<WorkoutSet> {
        val to_be_updated_sets: ArrayList<WorkoutSet> = ArrayList()

        for (i in 0 until workoutDays.size) {
            for (j in 0 until workoutDays[i].Sets!!.size) {
                if (workoutDays[i].Sets!![j].exerciseName == exercise_name) {
                    val temp_set = workoutDays[i].Sets!![j]
                    temp_set.exerciseName = new_exercise_name
                    temp_set.category = new_exercise_bodypart
                    to_be_updated_sets.add(temp_set)
                }
            }

            for (j in 0 until workoutDays[i].Exercises!!.size) {
                if (workoutDays[i].Exercises!![j].Exercise == exercise_name) {
                    for (k in 0 until workoutDays[i].Exercises!![j].Sets!!.size) {
                        if (workoutDays[i].Exercises!![j].Sets!![k].exerciseName == exercise_name) {
                            val temp_set = workoutDays[i].Exercises!![j].Sets!![k]
                            temp_set.exerciseName = new_exercise_name
                            temp_set.category = new_exercise_bodypart
                            to_be_updated_sets.add(temp_set)
                        }
                    }
                }
            }
        }
        return to_be_updated_sets
    }

    // Add all exercises found in the csv to the Known Exercises local data structure
    fun csvToKnownExercises() {
        // Make new ArrayList which will hold duplicates
        val DuplicateKnownExercises = ArrayList<Exercise>()

        for (i in 0 until workoutDays.size) {
            for (j in 0 until workoutDays[i].Sets!!.size) {
                val Name = workoutDays[i].Sets!![j].exerciseName!!
                val Bodypart = workoutDays[i].Sets!![j].category!!
                DuplicateKnownExercises.add(Exercise(Name, Bodypart))
            }
        }

        // Manual Implementation "borrowed" from stack overflow
        for (event in DuplicateKnownExercises) {
            var isFound = false
            // check if the event name exists in noRepeat
            for (e in knownExercises) {
                if (e.Name == event.Name || e === event) {
                    isFound = true
                    break
                }
            }
            if (!isFound) knownExercises.add(event)
        }
    }
}