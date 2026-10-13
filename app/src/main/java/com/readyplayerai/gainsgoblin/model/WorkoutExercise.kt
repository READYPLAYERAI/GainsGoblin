package com.readyplayerai.gainsgoblin.model

import kotlin.jvm.JvmName
import java.util.ArrayList

class WorkoutExercise {
    var Sets: ArrayList<WorkoutSet>? = null
    var Date: String? = null
    var Exercise: String? = null
    var Volume: Double? = null
    var EstimatedOneRepMax: Double? = null
    var ActualOneRepMax: Double? = null
    var MaxWeight: Double? = null
    var MaxReps: Double? = null
    var TotalReps: Double? = null
    var TotalSets: Double? = null
    var MaxSetVolume: Double? = null

    @get:JvmName("isVolumePR") var VolumePR: Boolean = false
    @get:JvmName("isActualOneRepMaxPR") var ActualOneRepMaxPR: Boolean = false
    @get:JvmName("isEstimatedOneRepMaxPR") var EstimatedOneRepMaxPR: Boolean = false
    @get:JvmName("isMaxRepsPR") var MaxRepsPR: Boolean = false
    @get:JvmName("isMaxWeightPR") var MaxWeightPR: Boolean = false
    @get:JvmName("isHTLT") var HTLT: Boolean = false

    var Comment: String? = ""
    var maxVolumeSet: WorkoutSet? = null
    var maxRepsSet: WorkoutSet? = null
    var maxWeightSet: WorkoutSet? = null
}