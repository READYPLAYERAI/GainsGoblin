package com.readyplayerai.gainsgoblin.ui.compose.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.model.Exercise
import com.readyplayerai.gainsgoblin.model.WorkoutDay
import com.readyplayerai.gainsgoblin.model.WorkoutSet
import com.readyplayerai.gainsgoblin.ui.compose.CategoryIcon
import com.readyplayerai.gainsgoblin.ui.compose.CategoryMarker
import kotlin.math.roundToInt

internal data class LastPerformed(val set: WorkoutSet, val day: WorkoutDay)

internal fun lastPerformed(days: List<WorkoutDay>, name: String?): LastPerformed? {
    if (name == null) return null
    var best: LastPerformed? = null
    for (day in days) {
        val exercise = day.Exercises?.firstOrNull { it.Exercise == name } ?: continue
        val set = exercise.Sets?.lastOrNull() ?: continue
        val candidate = LastPerformed(set, day)
        val bestDate = best?.day?.Date.orEmpty()
        if (day.Date.orEmpty() > bestDate) best = candidate
    }
    return best
}

/**
 * Bottom sheet for adding an exercise to a journal day: searchable list with
 * category and recent filters, recent/all sections, already-added markers and
 * an inline "create exercise" action at the bottom.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun JournalExercisePicker(
    targetDateLabel: String,
    exercises: List<Exercise>,
    days: List<WorkoutDay>,
    alreadyInDay: Set<String>,
    onPick: (Exercise) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) } // null = All
    var recentOnly by remember { mutableStateOf(false) }
    val categories = stringArrayResource(R.array.Categories)

    val q = query.trim()
    val matches = exercises.filter {
        q.isBlank() ||
            it.Name?.contains(q, true) == true ||
            it.BodyPart?.contains(q, true) == true
    }
    val filtered = matches
        .filter { exercise ->
            when {
                recentOnly -> lastPerformed(days, exercise.Name) != null
                selectedCategory != null -> exercise.BodyPart.equals(selectedCategory, ignoreCase = true)
                else -> true
            }
        }
        .sortedBy { it.Name.orEmpty() }

    val showSections = selectedCategory == null && !recentOnly && q.isBlank()
    val recentPairs = if (showSections) {
        exercises
            .mapNotNull { exercise -> lastPerformed(days, exercise.Name)?.let { exercise to it } }
            .sortedByDescending { it.second.day.Date.orEmpty() }
            .take(6)
    } else {
        emptyList()
    }
    val recentNames = recentPairs.map { it.first.Name }.toSet()
    val display = if (showSections) {
        exercises.filter { it.Name !in recentNames }.sortedBy { it.Name.orEmpty() }
    } else {
        filtered
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.journal_add_exercise),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = targetDateLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close_24px),
                        contentDescription = null,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.journal_search_exercises)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_search_24px),
                        contentDescription = null,
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                ),
            )

            Spacer(Modifier.height(12.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedCategory == null && !recentOnly,
                        onClick = {
                            selectedCategory = null
                            recentOnly = false
                        },
                        label = { Text(stringResource(R.string.journal_all)) },
                    )
                }
                item {
                    FilterChip(
                        selected = recentOnly,
                        onClick = {
                            recentOnly = true
                            selectedCategory = null
                        },
                        label = { Text(stringResource(R.string.journal_recent)) },
                    )
                }
                items(categories.toList()) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = {
                            selectedCategory = category
                            recentOnly = false
                        },
                        label = { Text(category) },
                        leadingIcon = {
                            CategoryMarker(
                                category = category,
                                size = FilterChipDefaults.IconSize,
                            )
                        },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (showSections) {
                    if (recentPairs.isNotEmpty()) {
                        item { SectionLabel(stringResource(R.string.journal_recent)) }
                        items(recentPairs, key = { it.first.Name.orEmpty() }) { pair ->
                            ExercisePickerRow(
                                exercise = pair.first,
                                days = days,
                                alreadyInDay = alreadyInDay,
                                onPick = onPick,
                            )
                        }
                    }
                    item { SectionLabel(stringResource(R.string.journal_all_exercises)) }
                }

                if (filtered.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.journal_no_exercises_match),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    items(display, key = { it.Name.orEmpty() }) { exercise ->
                        ExercisePickerRow(
                            exercise = exercise,
                            days = days,
                            alreadyInDay = alreadyInDay,
                            onPick = onPick,
                        )
                    }
                }

                item {
                    val createLabel = if (filtered.isEmpty() && q.isNotBlank()) {
                        stringResource(R.string.journal_create_named_exercise, q)
                    } else {
                        stringResource(R.string.journal_create_exercise)
                    }
                    OutlinedButton(
                        onClick = { onCreate(q) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add_24px),
                            contentDescription = null,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = createLabel,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.1.em,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ExercisePickerRow(
    exercise: Exercise,
    days: List<WorkoutDay>,
    alreadyInDay: Set<String>,
    onPick: (Exercise) -> Unit,
) {
    val last = lastPerformed(days, exercise.Name)
    val already = exercise.Name?.let { alreadyInDay.contains(it) } == true

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable { onPick(exercise) }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CategoryIcon(category = exercise.BodyPart)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = exercise.Name.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = when {
                    already -> stringResource(R.string.journal_already_added)
                    last != null -> stringResource(
                        R.string.journal_last_trained,
                        formatWeight(last.set.weight),
                        formatReps(last.set.reps),
                    )
                    else -> stringResource(R.string.journal_not_trained)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    if (already) {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(
                    if (already) R.drawable.ic_check_24px else R.drawable.ic_add_24px,
                ),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (already) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                },
            )
        }
    }
}

/** Formats a weight: null -> "?", integers without ".0". */
private fun formatWeight(value: Double?): String {
    if (value == null) return "?"
    return if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}

/** Formats a rep count: null -> "?", otherwise rounded to an int. */
private fun formatReps(value: Double?): String {
    if (value == null) return "?"
    return value.roundToInt().toString()
}
