package com.readyplayerai.gainsgoblin.ui

import androidx.lifecycle.ViewModel
import com.readyplayerai.gainsgoblin.data.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Backs [CustomExerciseActivity].
 *
 * The Activity owns all view wiring; this ViewModel only forwards the save
 * request to the repository and reports whether the exercise was new.
 */
@HiltViewModel
class CustomExerciseViewModel @Inject constructor(
    private val repository: WorkoutRepository
) : ViewModel() {

    /** Returns false when an exercise named [name] already exists. */
    suspend fun addExercise(name: String, category: String?): Boolean =
        repository.addExercise(name, category)
}
