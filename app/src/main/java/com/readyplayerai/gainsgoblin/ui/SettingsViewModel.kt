package com.readyplayerai.gainsgoblin.ui

import androidx.lifecycle.ViewModel
import com.readyplayerai.gainsgoblin.data.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Backs [SettingsActivity].
 *
 * Settings only needs the destructive "delete data" action; all other preferences
 * are read directly from the preference XML by the fragment.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: WorkoutRepository
) : ViewModel() {

    /** Settings "delete data" semantics: workouts ONLY (known exercises stay). */
    suspend fun clearWorkouts() = repository.clearWorkouts()
}
