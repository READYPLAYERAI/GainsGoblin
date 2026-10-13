package com.readyplayerai.gainsgoblin

import com.readyplayerai.gainsgoblin.model.WorkoutSet

class ExercisePersonalStats {
    var exerciseName: String? = ""
    var exerciseCategory: String? = null
    var favorite: Boolean? = false
    var totalSets: Double? = 0.0
    var totalReps: Double? = 0.0
    var totalVolume: Double? = 0.0
    var maxVolume: Double? = 0.0
    var maxWeight: Double? = 0.0
    var maxReps: Double? = 0.0
    var maxSetVolume: Double? = 0.0
    var maxSetVolumeReps: Double? = 0.0
    var MaxSetVolumeWeight: Double? = 0.0
    var actual1RM: Double? = 0.0
    var estimated1RM: Double? = 0.0
    var averageSetsPerWeek: Double? = 0.0
    var averageSetsPerWeekLastMoth: Double? = 0.0
    var lastWeekSets: Double? = 0.0
    var maxVolumeSet: WorkoutSet? = WorkoutSet()
    var maxRepsSet: WorkoutSet? = WorkoutSet()
    var maxWeightSet: WorkoutSet? = WorkoutSet()
}