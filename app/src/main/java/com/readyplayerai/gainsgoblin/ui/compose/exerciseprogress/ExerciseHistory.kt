package com.readyplayerai.gainsgoblin.ui.compose.exerciseprogress

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.domain.ExercisePrKind
import com.readyplayerai.gainsgoblin.domain.ExerciseSession
import com.readyplayerai.gainsgoblin.domain.ExerciseSetEntry
import com.readyplayerai.gainsgoblin.ui.compose.progress.formatVolume
import com.readyplayerai.gainsgoblin.ui.compose.progress.formatWeight
import com.readyplayerai.gainsgoblin.ui.compose.progress.monthLabel
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * One row in the session history list: a month divider or a session card.
 */
sealed interface ExerciseHistoryRow {
    data class Month(
        val label: String,
        val sessionCount: Int,
        val volume: Double,
    ) : ExerciseHistoryRow

    data class Session(val session: ExerciseSession) : ExerciseHistoryRow
}

/**
 * Flattened history payload: [total] is the session count after the PR filter but
 * before the show-all limit, [shownSessions] is the count actually listed, and
 * [rows] is the month/session sequence to render.
 */
data class ExerciseHistoryData(
    val total: Int,
    val shownSessions: Int,
    val rows: List<ExerciseHistoryRow>,
)

/**
 * Builds the newest-first history rows from ascending [sessions]. History is
 * independent of the hero/chart range: every session is considered unless
 * [prOnly] filters to sessions with at least one PR. When [showAll] is false only
 * the first [limit] of the filtered sessions are kept; kept sessions are grouped
 * by [monthLabel] preserving newest-first order.
 */
fun exerciseHistoryRows(
    sessions: List<ExerciseSession>,
    prOnly: Boolean,
    showAll: Boolean,
    limit: Int = 9,
): ExerciseHistoryData {
    val newestFirst = sessions.asReversed()
    val filtered = if (prOnly) {
        newestFirst.filter { it.prKinds.isNotEmpty() }
    } else {
        newestFirst
    }
    val total = filtered.size
    val limited = if (showAll) filtered else filtered.take(limit)

    val rows = ArrayList<ExerciseHistoryRow>()
    limited.groupBy { monthLabel(it.date) }.forEach { (label, group) ->
        rows.add(ExerciseHistoryRow.Month(label = label, sessionCount = group.size, volume = group.sumOf { it.volume }))
        group.forEach { session -> rows.add(ExerciseHistoryRow.Session(session)) }
    }
    return ExerciseHistoryData(total = total, shownSessions = limited.size, rows = rows)
}

/** All/PR filter chips above the history list. */
@Composable
fun ExerciseHistoryFilterRow(prOnly: Boolean, onPrOnlyChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FilterChip(
            selected = !prOnly,
            onClick = { onPrOnlyChange(false) },
            modifier = Modifier.height(31.dp),
            shape = RoundedCornerShape(999.dp),
            label = {
                Text(
                    text = stringResource(R.string.exercise_progress_filter_all),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
        )
        FilterChip(
            selected = prOnly,
            onClick = { onPrOnlyChange(true) },
            modifier = Modifier.height(31.dp),
            shape = RoundedCornerShape(999.dp),
            label = {
                Text(
                    text = stringResource(R.string.exercise_progress_filter_pr),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_emoji_events_24px),
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
        )
    }
}

/** Month divider: uppercase label plus session count and volume meta. */
@Composable
fun ExerciseHistoryMonth(label: String, sessionCount: Int, volume: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.1.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.alignByBaseline(),
        )
        Text(
            text = pluralStringResource(
                R.plurals.exercise_progress_history_month_meta,
                sessionCount,
                sessionCount,
                formatVolume(volume),
            ),
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            modifier = Modifier.alignByBaseline(),
        )
    }
}

/** One collapsible session card: header with date tile, top set, PR chips and deltas. */
@Composable
fun SessionCard(
    session: ExerciseSession,
    expanded: Boolean,
    onToggle: () -> Unit,
    onSetClick: (Int) -> Unit,
) {
    Surface(
        onClick = onToggle,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DateTile(date = session.date)
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "${formatWeight(session.topWeight)} kg × ${formatWeight(session.topReps)}",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        modifier = Modifier.padding(top = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(
                                R.string.exercise_progress_session_meta,
                                session.sets.size,
                                formatVolume(session.volume),
                                session.totalReps.roundToInt(),
                            ),
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (session.comment.isNotBlank()) {
                            Spacer(Modifier.width(5.dp))
                            Icon(
                                painter = painterResource(R.drawable.ic_comment_24px),
                                contentDescription = stringResource(R.string.exercise_progress_comment_cd),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(11.dp),
                            )
                        }
                    }
                    if (session.prKinds.isNotEmpty()) {
                        Row(
                            modifier = Modifier.padding(top = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            val kinds = PrChipOrder.filter { it in session.prKinds }
                            kinds.take(2).forEach { kind ->
                                PrChip(label = stringResource(prChipLabelRes(kind)))
                            }
                            if (kinds.size > 2) {
                                PrChip(
                                    label = stringResource(
                                        R.string.exercise_progress_pr_chip_more,
                                        kinds.size - 2,
                                    ),
                                )
                            }
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    HistoryDeltaChip(
                        weightDelta = session.topWeightDelta,
                        repsDelta = session.topRepsDelta,
                    )
                    val rotation by animateFloatAsState(
                        targetValue = if (expanded) 180f else 0f,
                        label = "historyChevron",
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_expand_more_24px),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .size(17.dp)
                            .graphicsLayer { rotationZ = rotation },
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    SetTableHeader()
                    session.sets.forEachIndexed { index, set ->
                        SetRow(set = set, onClick = { onSetClick(index) })
                    }
                    if (session.comment.isNotBlank()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                        ) {
                            Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_comment_24px),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(12.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = session.comment,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 3.dp, end = 3.dp, top = 8.dp, bottom = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(
                                R.string.exercise_progress_set_est_1rm,
                                formatWeight(session.estimatedOneRepMax),
                            ),
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = session.date.take(4),
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** Centred empty state shown when the PR filter has no matching sessions. */
@Composable
fun ExerciseHistoryEmptyState(onShowAll: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_emoji_events_24px),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(36.dp),
        )
        Text(
            text = stringResource(R.string.exercise_progress_pr_empty_title),
            modifier = Modifier.padding(top = 10.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.exercise_progress_pr_empty_body),
            modifier = Modifier.padding(top = 4.dp),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        TextButton(
            onClick = onShowAll,
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text(stringResource(R.string.exercise_progress_filter_all))
        }
    }
}

/** Full-width pill toggling between the limited and complete history list. */
@Composable
fun ExerciseShowAllButton(showAll: Boolean, total: Int, onToggle: () -> Unit) {
    OutlinedButton(
        onClick = onToggle,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .height(44.dp),
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text = stringResource(
                if (showAll) R.string.exercise_progress_show_fewer else R.string.exercise_progress_show_all,
                total,
            ),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** 45×50 surface tile carrying the day number and short month. */
@Composable
private fun DateTile(date: String) {
    val parsed = try {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date)
    } catch (e: ParseException) {
        null
    }
    val dayText = if (parsed != null) SimpleDateFormat("d", Locale.US).format(parsed) else date
    val monthText = if (parsed != null) {
        SimpleDateFormat("MMM", Locale.US).format(parsed).uppercase(Locale.US)
    } else {
        ""
    }

    Column(
        modifier = Modifier
            .size(width = 45.dp, height = 50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.height(2.dp))
        Text(
            text = dayText,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.51).sp,
            lineHeight = 18.sp,
        )
        Text(
            text = monthText,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.68.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 10.sp,
        )
    }
}

/** Small tertiary label naming one PR kind. */
@Composable
private fun PrChip(label: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_whatshot_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(9.dp),
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

/** Delta badge comparing the session's top set with the previous one. */
@Composable
private fun HistoryDeltaChip(weightDelta: Double?, repsDelta: Double?) {
    val weight = weightDelta ?: 0.0
    val reps = repsDelta ?: 0.0
    val weightUp = weight > 0.0001
    val weightDown = weight < -0.0001
    val repsUp = !weightUp && !weightDown && reps > 0.0001
    val repsDown = !weightUp && !weightDown && reps < -0.0001

    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val text: String
    val background: Color
    val textColor: Color
    when {
        weightUp -> {
            text = stringResource(R.string.exercise_progress_delta_weight_up, formatWeight(weight))
            background = primary.copy(alpha = 0.13f)
            textColor = primary
        }

        weightDown -> {
            text = stringResource(R.string.exercise_progress_delta_weight_down, formatWeight(abs(weight)))
            background = surface
            textColor = onSurfaceVariant
        }

        repsUp -> {
            val count = reps.roundToInt()
            text = pluralStringResource(R.plurals.exercise_progress_delta_reps_up, count, count)
            background = primary.copy(alpha = 0.13f)
            textColor = primary
        }

        repsDown -> {
            val count = abs(reps).roundToInt()
            text = pluralStringResource(R.plurals.exercise_progress_delta_reps_down, count, count)
            background = surface
            textColor = onSurfaceVariant
        }

        else -> {
            text = stringResource(R.string.exercise_progress_same_top_set)
            background = surface
            textColor = onSurfaceVariant
        }
    }

    Surface(shape = RoundedCornerShape(999.dp), color = background) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            maxLines = 1,
        )
    }
}

/** Column captions above the expanded set table. */
@Composable
private fun SetTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 3.dp, end = 3.dp, top = 9.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderCell(
            text = stringResource(R.string.exercise_progress_set_index),
            modifier = Modifier.width(18.dp),
        )
        HeaderCell(
            text = stringResource(R.string.exercise_progress_set_weight),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
        )
        HeaderCell(
            text = stringResource(R.string.exercise_progress_set_reps),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
        )
        HeaderCell(
            text = stringResource(R.string.exercise_progress_set_volume),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
        )
        Spacer(Modifier.width(18.dp))
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier, textAlign: TextAlign = TextAlign.Start) {
    Text(
        text = text,
        modifier = modifier,
        fontSize = 9.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.72.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
        textAlign = textAlign,
    )
}

/** One set line, tinted when it counted as a personal record. */
@Composable
private fun SetRow(set: ExerciseSetEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .then(
                if (set.isPr) {
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f))
                } else {
                    Modifier
                },
            )
            .padding(vertical = 6.dp, horizontal = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = set.index.toString(),
            modifier = Modifier.width(18.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
        )
        Text(
            text = formatWeight(set.weight) + " kg",
            modifier = Modifier.weight(1f),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
        )
        Text(
            text = "× " + formatWeight(set.reps),
            modifier = Modifier.weight(1f),
            fontSize = 12.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
        )
        Text(
            text = formatVolume(set.volume),
            modifier = Modifier.weight(1f),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = TextAlign.End,
        )
        Box(
            modifier = Modifier.size(18.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (set.isPr) {
                Icon(
                    painter = painterResource(R.drawable.ic_whatshot_24px),
                    contentDescription = stringResource(R.string.exercise_progress_pr_set_cd),
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(11.dp),
                )
            }
        }
    }
}

/** Fixed display order for PR chips. */
private val PrChipOrder = listOf(
    ExercisePrKind.EST_ONE_REP,
    ExercisePrKind.MAX_WEIGHT,
    ExercisePrKind.MAX_REPS,
    ExercisePrKind.SET_VOLUME,
    ExercisePrKind.VOLUME,
    ExercisePrKind.ACTUAL_ONE_REP,
)

@StringRes
private fun prChipLabelRes(kind: ExercisePrKind): Int = when (kind) {
    ExercisePrKind.EST_ONE_REP -> R.string.exercise_progress_pr_chip_1rm
    ExercisePrKind.MAX_WEIGHT -> R.string.exercise_progress_pr_chip_weight
    ExercisePrKind.MAX_REPS -> R.string.exercise_progress_pr_chip_reps
    ExercisePrKind.SET_VOLUME -> R.string.exercise_progress_pr_chip_set
    ExercisePrKind.VOLUME -> R.string.exercise_progress_pr_chip_volume
    ExercisePrKind.ACTUAL_ONE_REP -> R.string.exercise_progress_pr_chip_1rm
}
