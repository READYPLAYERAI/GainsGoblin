package com.readyplayerai.gainsgoblin.data

import kotlinx.serialization.json.Json

val persistenceJson: Json = Json {
    explicitNulls = false
    ignoreUnknownKeys = true
    encodeDefaults = true
}
