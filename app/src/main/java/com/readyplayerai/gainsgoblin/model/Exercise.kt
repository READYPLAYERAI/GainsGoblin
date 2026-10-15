package com.readyplayerai.gainsgoblin.model

import kotlinx.serialization.Serializable

@Serializable
class Exercise(var Name: String?, var BodyPart: String?) {
    var favorite: Boolean? = false
}