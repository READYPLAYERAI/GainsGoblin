package com.readyplayerai.gainsgoblin.ui.compose.progress

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.domain.OverloadDelta
import com.readyplayerai.gainsgoblin.domain.OverloadKind
import com.readyplayerai.gainsgoblin.model.WorkoutDay
import com.readyplayerai.gainsgoblin.model.WorkoutExercise
import com.readyplayerai.gainsgoblin.model.WorkoutSet
import com.readyplayerai.gainsgoblin.ui.compose.CategoryMarker
import kotlin.math.roundToInt

/**
 * Renders the Journal lens: month-grouped, newest-first workout days with
 * expandable day cards, expandable set-by-set exercise rows, record flame chips,
 * "harder than last time" deltas and read-only comments. Days are rendered
 * newest-first regardless of the input order.
 *
 * Plain [LazyListScope] extension so the day cards and exercise rows stay lazy.
 */
fun LazyListScope.journalLens(
    days: List<WorkoutDay>,
    categoryOf: (String?) -> String?,
    query: String,
    category: String?,
    overloads: Map<String, OverloadDelta>,
    onExerciseClick: (exerciseName: String, date: String) -> Unit,
) {
    val orderedDays = days.sortedByDescending { it.Date ?: "" }
    val filtered = ArrayList<Pair<WorkoutDay, List<WorkoutExercise>>>()
    for (day in orderedDays) {
        val date = day.Date
        if (date == null || date.isBlank() || date == "0000-00-00") continue
        val exercises = day.Exercises?.filter { exercise ->
            (exercise.Exercise ?: "").contains(query, ignoreCase = true) &&
                (category == null || categoryOf(exercise.Exercise) == category)
        } ?: emptyList()
        if (exercises.isEmpty()) continue
        filtered.add(Pair(day, exercises))
    }

    if (filtered.isEmpty()) {
        item(key = "journal-empty") { JournalEmptyState() }
        return
    }

    var index = 0
    while (index < filtered.size) {
        val month = monthKey(filtered[index].first.Date)
        var end = index
        while (end < filtered.size && monthKey(filtered[end].first.Date) == month) {
            end++
        }
        val monthDays = filtered.subList(index, end).map { it.first }
        item(key = "month-$month") { MonthHeader(days = monthDays) }
        for (position in index until end) {
            val day = filtered[position].first
            val exercises = filtered[position].second
            item(key = "day-${day.Date}") {
                DayCard(
                    day = day,
                    exercises = exercises,
                    categoryOf = categoryOf,
                    overloads = overloads,
                    onExerciseClick = onExerciseClick,
                )
            }
        }
        index = end
    }
}

/** First seven characters of a `yyyy-MM-dd` string, i.e. its `yyyy-MM` bucket. */
private fun monthKey(date: String?): String = date?.take(7) ?: ""

/** Sticky, opaque month header with the month label and its filtered rollup. */
@Composable
private fun MonthHeader(days: List<WorkoutDay>) {
    val count = days.size
    val sets = days.sumOf { it.Sets?.size ?: 0 }
    val volume = days.sumOf { it.DayVolume ?: 0.0 }
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = monthLabel(days.firstOrNull()?.Date).uppercase(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    R.string.journal_month_summary,
                    count,
                    sets,
                    formatTonsOrKg(volume),
                ),
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** One workout day card: header with rollup and chevron, then the exercise rows. */
@Composable
private fun DayCard(
    day: WorkoutDay,
    exercises: List<WorkoutExercise>,
    categoryOf: (String?) -> String?,
    overloads: Map<String, OverloadDelta>,
    onExerciseClick: (exerciseName: String, date: String) -> Unit,
) {
    var expanded by rememberSaveable(day.Date) { mutableStateOf(true) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 0f else -90f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy),
        label = "dayChevron",
    )
    val recordCount = exercises.count { recordFlagCount(it) > 0 }
    val dayDate = day.Date ?: ""

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .animateContentSize(),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(start = 16.dp, end = 10.dp, top = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = weekdayShort(day.Date),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.5).sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = dayWithYear(day.Date),
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (recordCount > 0) {
                            RecordCountChip(recordCount)
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatTonsOrKg(day.DayVolume ?: 0.0),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.journal_day_sets, day.Sets?.size ?: 0),
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    painter = painterResource(R.drawable.ic_expand_more_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(22.dp)
                        .rotate(rotation),
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    exercises.forEachIndexed { position, exercise ->
                        if (position > 0) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                        ExerciseRow(
                            exercise = exercise,
                            dayDate = dayDate,
                            categoryOf = categoryOf,
                            overload = overloads["$dayDate|${exercise.Exercise}"],
                            onExerciseClick = onExerciseClick,
                        )
                    }
                }
            }
        }
    }
}

/** Small count chip shown next to the date when the day holds records. */
@Composable
private fun RecordCountChip(count: Int) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier.height(20.dp),
    ) {
        Text(
            text = pluralStringResource(R.plurals.progress_record_count, count, count),
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

/** One exercise row: marker, meta line with badges, records button and chevron. */
@Composable
private fun ExerciseRow(
    exercise: WorkoutExercise,
    dayDate: String,
    categoryOf: (String?) -> String?,
    overload: OverloadDelta?,
    onExerciseClick: (exerciseName: String, date: String) -> Unit,
) {
    var open by rememberSaveable(dayDate, exercise.Exercise) { mutableStateOf(false) }
    val name = exercise.Exercise ?: ""
    val sets = exercise.Sets ?: emptyList()
    val recordFlags = recordFlags(exercise)
    val recordCount = recordFlags.count { it }
    val firstTrueIndex = recordFlags.indexOfFirst { it }
    val comment = exercise.Comment
    val hasComment = !comment.isNullOrBlank()
    val chevronRotation by animateFloatAsState(
        targetValue = if (open) 180f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy),
        label = "exerciseChevron",
    )

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { open = !open }
                .padding(start = 16.dp, end = 8.dp, top = 11.dp, bottom = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            CategoryMarker(category = categoryOf(exercise.Exercise), size = 15.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(
                            R.string.journal_exercise_meta,
                            sets.size,
                            formatWeight(exercise.MaxWeight ?: 0.0),
                            formatTonsOrKg(exercise.Volume ?: 0.0),
                        ),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (recordCount > 0) {
                        RecordFlameChip(count = recordCount, firstTrueIndex = firstTrueIndex)
                    } else if (overload != null) {
                        OverloadBadge(overload)
                    }
                    if (hasComment) {
                        Icon(
                            painter = painterResource(R.drawable.ic_comment_24px),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
            }
            IconButton(
                onClick = { onExerciseClick(name, dayDate) },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_leaderboard_24px),
                    contentDescription = stringResource(R.string.journal_records_for, name),
                    modifier = Modifier.size(19.dp),
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_expand_more_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(22.dp)
                    .rotate(chevronRotation),
            )
        }
        AnimatedVisibility(visible = open) {
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                val prSet = when {
                    exercise.MaxWeightPR -> exercise.maxWeightSet
                    exercise.MaxRepsPR -> exercise.maxRepsSet
                    else -> null
                }
                sets.forEachIndexed { index, set ->
                    SetRow(
                        set = set,
                        index = index,
                        isPr = set === prSet,
                    )
                }
                if (hasComment) {
                    Row(
                        modifier = Modifier.padding(start = 42.dp, end = 16.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_comment_24px),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = "\"${comment.orEmpty()}\"",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** Tertiary flame chip naming a single record, or counting several. */
@Composable
private fun RecordFlameChip(count: Int, firstTrueIndex: Int) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier.height(24.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_whatshot_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = if (count == 1) {
                    stringResource(recordLabelRes(firstTrueIndex))
                } else {
                    pluralStringResource(R.plurals.progress_record_count, count, count)
                },
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

/** Harder-than-last-time delta badge: weight, reps or volume. */
@Composable
private fun OverloadBadge(delta: OverloadDelta) {
    Text(
        text = when (delta.kind) {
            OverloadKind.WEIGHT ->
                stringResource(R.string.journal_overload_weight, formatWeight(delta.amount))
            OverloadKind.REPS -> {
                val reps = delta.amount.roundToInt()
                pluralStringResource(R.plurals.progress_overload_reps, reps, reps)
            }
            OverloadKind.VOLUME -> stringResource(R.string.journal_overload_symbol)
        },
        color = MaterialTheme.colorScheme.primary,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Bold,
    )
}

/** One set line; [isPr] tints the record set. */
@Composable
private fun SetRow(
    set: WorkoutSet,
    index: Int,
    isPr: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isPr) {
                    Modifier
                        .padding(horizontal = 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f))
                } else {
                    Modifier
                },
            )
            .padding(
                start = if (isPr) 12.dp else 42.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${index + 1}",
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(16.dp),
        )
        Text(
            text = "${formatWeight(set.weight ?: 0.0)} kg",
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.widthIn(min = 64.dp),
        )
        Text(
            text = "× ${formatWeight(set.reps ?: 0.0)}",
            fontSize = 13.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = formatVolume((set.reps ?: 0.0) * (set.weight ?: 0.0)),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Centred empty state for a Journal with no matching rows. */
@Composable
private fun JournalEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.progress_journal_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.progress_journal_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Record flags in the display priority order; `HTLT` is intentionally excluded. */
private fun recordFlags(exercise: WorkoutExercise): List<Boolean> = listOf(
    exercise.MaxWeightPR,
    exercise.MaxRepsPR,
    exercise.VolumePR,
    exercise.EstimatedOneRepMaxPR,
    exercise.ActualOneRepMaxPR,
)

private fun recordFlagCount(exercise: WorkoutExercise): Int =
    recordFlags(exercise).count { it }

@StringRes
private fun recordLabelRes(index: Int): Int = when (index) {
    0 -> R.string.journal_record_weight
    1 -> R.string.journal_record_reps
    2 -> R.string.journal_record_volume
    3 -> R.string.journal_record_est_1rm
    else -> R.string.journal_record_1rm
}
