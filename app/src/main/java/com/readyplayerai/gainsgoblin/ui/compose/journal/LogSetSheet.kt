package com.readyplayerai.gainsgoblin.ui.compose.journal

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.readyplayerai.gainsgoblin.R
import kotlin.math.roundToInt

/**
 * Reusable bottom sheet for logging a new set or editing an existing one. Wraps
 * [JournalComposer] in a [ModalBottomSheet], prefills the weight/reps from the
 * supplied initial values, validates the input, and reports the saved values
 * through [onSave].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LogSetSheet(
    exerciseName: String,
    dateLabel: String,
    initialWeight: Double?,
    initialReps: Double?,
    editingSetIndex: Int?,
    onSave: (weight: Double, reps: Double) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sourceLabel = if (editingSetIndex == null && initialWeight != null && initialReps != null) {
        context.getString(
            R.string.journal_last_trained,
            formatWeightValue(initialWeight),
            formatRepsValue(initialReps),
        )
    } else {
        ""
    }

    var state by remember(exerciseName, initialWeight, initialReps, editingSetIndex) {
        mutableStateOf(
            JournalComposerState(
                weight = formatWeightValue(initialWeight),
                reps = formatRepsValue(initialReps),
                editingSetIndex = editingSetIndex,
                originalWeight = initialWeight,
                originalReps = initialReps,
                sourceLabel = sourceLabel,
            ),
        )
    }

    fun save() {
        val weight = state.weight.toDoubleOrNull()
        val reps = state.reps.toDoubleOrNull()
        if (weight == null || weight <= 0.0 || reps == null || reps <= 0.0) {
            Toast.makeText(
                context,
                context.getString(R.string.add_exercise_invalid_inputs),
                Toast.LENGTH_SHORT,
            ).show()
            return
        }
        onSave(weight, reps)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = exerciseName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            JournalComposer(
                state = state,
                onWeightChange = { state = state.copy(weight = it) },
                onRepsChange = { state = state.copy(reps = it) },
                onStepWeight = { delta ->
                    val current = state.weight.toDoubleOrNull() ?: 0.0
                    state = state.copy(
                        weight = formatWeightValue((current + delta).coerceAtLeast(0.0)),
                    )
                },
                onStepReps = { delta ->
                    val current = state.reps.toDoubleOrNull() ?: 0.0
                    state = state.copy(
                        reps = (current.roundToInt() + delta).coerceAtLeast(0).toString(),
                    )
                },
                onLogSet = { save() },
                onCancelEdit = onDismiss,
            )
        }
    }
}

private fun formatWeightValue(value: Double?): String {
    if (value == null) return ""
    return if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}

private fun formatRepsValue(value: Double?): String {
    if (value == null) return ""
    return value.roundToInt().toString()
}
