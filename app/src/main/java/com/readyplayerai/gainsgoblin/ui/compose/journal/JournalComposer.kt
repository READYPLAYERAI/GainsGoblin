package com.readyplayerai.gainsgoblin.ui.compose.journal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.readyplayerai.gainsgoblin.R
import kotlin.math.roundToInt

internal data class JournalComposerState(
    val weight: String = "",
    val reps: String = "",
    val editingSetIndex: Int? = null,
    val originalWeight: Double? = null,
    val originalReps: Double? = null,
    val sourceLabel: String = "",
)

internal data class JournalCardState(
    val expanded: Boolean = false,
    val isDraft: Boolean = false,
    val composer: JournalComposerState = JournalComposerState(),
    val note: String = "",
)

internal data class JournalCardCallbacks(
    val onToggle: () -> Unit,
    val onSetClick: (Int) -> Unit,
    val onSetMore: (Int) -> Unit,
    val onNoteChange: (String) -> Unit,
    val onNoteSave: () -> Unit,
    val onWeightChange: (String) -> Unit,
    val onRepsChange: (String) -> Unit,
    val onStepWeight: (Int) -> Unit,
    val onStepReps: (Int) -> Unit,
    val onLogSet: () -> Unit,
    val onCancelEdit: () -> Unit,
)

internal data class ExerciseKey(val date: String, val name: String)

internal data class JournalDraft(
    val date: String,
    val name: String,
    val category: String,
)

/**
 * Inline composer for logging a new set or editing an existing one directly on
 * an expanded journal exercise card: a titled panel holding weight and reps
 * steppers plus a Log set (or Update set + Cancel) action row.
 */
@Composable
internal fun JournalComposer(
    state: JournalComposerState,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onStepWeight: (Int) -> Unit,
    onStepReps: (Int) -> Unit,
    onLogSet: () -> Unit,
    onCancelEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val editIndex = state.editingSetIndex
    val editing = editIndex != null

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(modifier = Modifier.padding(13.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 4.dp, bottom = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = (
                        if (editing) {
                            stringResource(R.string.journal_editing_set, editIndex + 1)
                        } else {
                            stringResource(R.string.journal_new_set)
                        }
                        ).uppercase(),
                    modifier = Modifier.weight(1f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.09.em,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (editing) {
                        stringResource(
                            R.string.journal_set_was,
                            formatWeight(state.originalWeight),
                            formatReps(state.originalReps),
                        )
                    } else {
                        state.sourceLabel
                    },
                    fontSize = 11.5.sp,
                    style = TextStyle(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ComposerStepper(
                    value = state.weight,
                    unit = stringResource(R.string.add_exercise_kg),
                    keyboardType = KeyboardType.Decimal,
                    onValueChange = onWeightChange,
                    onDecrease = { onStepWeight(-1) },
                    onIncrease = { onStepWeight(1) },
                )
                ComposerStepper(
                    value = state.reps,
                    unit = stringResource(R.string.add_exercise_reps_unit),
                    keyboardType = KeyboardType.Number,
                    onValueChange = onRepsChange,
                    onDecrease = { onStepReps(-1) },
                    onIncrease = { onStepReps(1) },
                )
            }

            if (editing) {
                Row(
                    modifier = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    ComposerPrimaryButton(
                        text = stringResource(R.string.journal_update_set),
                        onClick = onLogSet,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(
                        onClick = onCancelEdit,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.exercises_cancel),
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight(650),
                        )
                    }
                }
            } else {
                ComposerPrimaryButton(
                    text = stringResource(R.string.journal_log_set),
                    onClick = onLogSet,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun ComposerPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(46.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        contentPadding = PaddingValues(horizontal = 8.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_add_24px),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = text,
            fontSize = 14.5.sp,
            fontWeight = FontWeight(650),
        )
    }
}

/** A 56dp pill stepper: [-] [centered field] [unit] [+], unit inside the pill. */
@Composable
private fun ComposerStepper(
    value: String,
    unit: String,
    keyboardType: KeyboardType,
    onValueChange: (String) -> Unit,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton(
            iconRes = R.drawable.ic_remove_24px,
            onClick = onDecrease,
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f).height(48.dp),
            textStyle = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                fontFeatureSettings = "tnum",
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { inner ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    inner()
                }
            },
        )
        Text(
            text = unit,
            modifier = Modifier
                .width(36.dp)
                .padding(end = 4.dp),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Start,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        StepButton(
            iconRes = R.drawable.ic_add_24px,
            onClick = onIncrease,
        )
    }
}

@Composable
private fun StepButton(iconRes: Int, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(34.dp),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
    }
}

/** Formats an original weight: null -> "?", integers without ".0". */
private fun formatWeight(value: Double?): String {
    if (value == null) return "?"
    return if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}

/** Formats an original rep count: null -> "?", otherwise rounded to an int. */
private fun formatReps(value: Double?): String {
    if (value == null) return "?"
    return value.roundToInt().toString()
}
