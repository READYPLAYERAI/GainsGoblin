package com.readyplayerai.gainsgoblin.model

import kotlinx.serialization.Serializable
import java.util.TreeSet
import java.util.ArrayList

@Serializable
class WorkoutDay {
    var Sets: ArrayList<WorkoutSet>? = ArrayList()
    var Exercises: ArrayList<WorkoutExercise>? = ArrayList()
    var DayVolume: Double? = 0.0
    var Date: String? = "0000-00-00"
    var Reps: Int = 0

    fun addSet(Set: WorkoutSet) {
        this.Sets!!.add(Set)
        UpdateData()
    }

    fun removeSet(Set: WorkoutSet) {
        // Deliberate deviation: Java had `assert this.Sets.size() > 1` which is
        // dropped because JVM assert is disabled on Android devices.
        this.Sets!!.remove(Set)
        UpdateData()
    }

    fun UpdateData() {
        if (Sets!!.isEmpty()) {
            Sets!!.clear()
            Exercises!!.clear()
            DayVolume = 0.0
            Date = "0000-00-00"
            Reps = 0
            return
        }

        Date = Sets!![0].date

        val day_sets: ArrayList<WorkoutSet> = Sets!!
        val Day_Exercises = ArrayList<WorkoutExercise>()
        val Exercises_Set = TreeSet<String>()
        var Day_Volume = 0.0

        for (temp_set in day_sets) {
            Day_Volume = Day_Volume + temp_set.getVolume()!!
            Exercises_Set.add(temp_set.exerciseName!!)
        }

        val itt = Exercises_Set.iterator()
        while (itt.hasNext()) {
            val exercise_name = itt.next()
            val day_exercise = WorkoutExercise()

            day_exercise.Exercise = exercise_name
            var exercise_volume = 0.0
            var exercise_one_rep_max = 0.0
            var exercise_max_reps = 0.0
            var exercise_max_weight = 0.0
            var exercise_total_reps = 0.0
            var exercise_total_sets = 0.0
            var exercise_actual_one_rep_max = 0.0
            var exercise_date = ""
            var exercise_max_set_volume = 0.0
            var exercise_comment: String? = ""

            var exercise_max_volume_set: WorkoutSet = WorkoutSet()
            var exercise_max_reps_set: WorkoutSet = WorkoutSet()
            var exercise_max_weight_set: WorkoutSet = WorkoutSet()

            val exercise_sets = ArrayList<WorkoutSet>()

            for (s in day_sets) {
                if (s.exerciseName == exercise_name) {
                    exercise_volume = exercise_volume + s.getVolume()!!
                    exercise_total_reps = exercise_total_reps + s.reps!!
                    exercise_total_sets = exercise_total_sets + 1
                    exercise_sets.add(s)
                    exercise_date = s.date!!
                    exercise_comment = s.comment

                    if (s.getEplayOneRepMax()!! > exercise_one_rep_max) {
                        exercise_one_rep_max = s.getEplayOneRepMax()!!
                    }
                    if (s.reps!! == 1.0 && s.weight!! > exercise_actual_one_rep_max) {
                        exercise_actual_one_rep_max = s.weight!!
                    }
                    if (s.reps!! > exercise_max_reps) {
                        exercise_max_reps = s.reps!!
                        exercise_max_reps_set = s
                    }
                    if (s.weight!! > exercise_max_weight) {
                        exercise_max_weight = s.weight!!
                        exercise_max_weight_set = s
                    }
                    if (s.reps!! * s.weight!! > exercise_max_set_volume) {
                        exercise_max_set_volume = s.reps!! * s.weight!!
                        exercise_max_volume_set = s
                    }
                }

                // Update exercise object — placed inside the inner loop
                // (mirroring the exact position in Java's source), so it runs on every
                // iteration regardless of name match.
                day_exercise.Volume = exercise_volume
                day_exercise.EstimatedOneRepMax = exercise_one_rep_max
                day_exercise.MaxReps = exercise_max_reps
                day_exercise.MaxWeight = exercise_max_weight
                day_exercise.TotalReps = exercise_total_reps
                day_exercise.TotalSets = exercise_total_sets
                day_exercise.Sets = exercise_sets
                day_exercise.Date = exercise_date
                day_exercise.MaxSetVolume = exercise_max_set_volume
                day_exercise.ActualOneRepMax = exercise_actual_one_rep_max
                day_exercise.Comment = exercise_comment
                day_exercise.maxVolumeSet = exercise_max_volume_set
                day_exercise.maxWeightSet = exercise_max_weight_set
                day_exercise.maxRepsSet = exercise_max_reps_set
            }
            Day_Exercises.add(day_exercise)
        }

        Exercises = Day_Exercises
        DayVolume = Day_Volume
        Reps = 0

        for (ex in Exercises!!) {
            Reps = Reps + Math.round(ex.TotalReps!!).toInt()
        }
    }
}