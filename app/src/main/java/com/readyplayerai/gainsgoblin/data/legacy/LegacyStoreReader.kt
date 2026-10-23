package com.readyplayerai.gainsgoblin.data.legacy

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

data class LegacyRaw(
    val rawWorkouts: String?,
    val rawKnown: String?
)

@Singleton
class LegacyStoreReader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>
) {
    suspend fun read(): LegacyRaw {
        val preferences = try {
            dataStore.data.first()
        } catch (e: Exception) {
            null
        }
        val legacy = context.getSharedPreferences("shared preferences", Context.MODE_PRIVATE)

        val workouts = preferences?.get(stringPreferencesKey("workouts"))
            ?: legacy.getString("workouts", null)
        val known = preferences?.get(stringPreferencesKey("known_exercises"))
            ?: legacy.getString("known_exercises", null)

        return LegacyRaw(workouts, known)
    }
}
