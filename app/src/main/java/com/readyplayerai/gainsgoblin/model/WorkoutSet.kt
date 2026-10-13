package com.readyplayerai.gainsgoblin.model

import com.google.gson.annotations.SerializedName

class WorkoutSet {
    var date: String? = null
    @SerializedName("exercise_name") var exerciseName: String? = null
    var category: String? = null
    var reps: Double? = null
    var weight: Double? = null
    var comment: String? = null
    var id: Int = 0
    var user_id: Int = 0

    constructor()
    constructor(Date: String?, Exercise: String?, Category: String?, Reps: Double?, Weight: Double?) {
        date = Date
        exerciseName = Exercise
        category = Category
        reps = Reps
        weight = Weight
        comment = ""
    }
    constructor(Date: String?, Exercise: String?, Category: String?, Reps: Double?, Weight: Double?, Comment: String?) : this(Date, Exercise, Category, Reps, Weight) {
        comment = Comment
    }

    fun getVolume(): Double? = reps!! * weight!!
    fun getEplayOneRepMax(): Double? = weight!! * (1 + (reps!! / 30))
}