package com.readyplayerai.gainsgoblin.ui.compose.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.domain.ExerciseRecord
import com.readyplayerai.gainsgoblin.ui.compose.CategoryMarker

/**
 * Read-only bottom sheet with the full all-time record breakdown for one
 * exercise: best weight, a 2x2 stat grid, a progressive-overload sparkline and
 * the most recent sessions. Has no edit, delete or favourite affordances.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordSheet(record: ExerciseRecord, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, bottom = 26.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                CategoryMarker(
                    category = record.category,
                    size = 16.dp,
                    modifier = Modifier.padding(top = 7.dp),
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(
                            R.string.sheet_session_meta,
                            record.category,
                            record.sessions.size,
                            shortDate(record.sessions.lastOrNull()?.date),
                        ),
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.sheet_best_weight).uppercase(),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = formatWeight(record.maxWeight),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1.4).sp,
                    )
                    Text(
                        text = " kg × ${formatWeight(record.maxWeightReps)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = stringResource(R.string.sheet_set_on, shortDate(record.maxWeightDate)),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatTile(
                    title = stringResource(R.string.sheet_best_set_volume),
                    value = formatVolume(record.bestSetVolume) + " kg",
                    caption = stringResource(
                        R.string.sheet_caption_set,
                        formatWeight(record.bestSetWeight),
                        formatWeight(record.bestSetReps),
                    ),
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    title = stringResource(R.string.sheet_best_reps),
                    value = stringResource(R.string.sheet_reps_value, formatWeight(record.maxReps)),
                    caption = stringResource(
                        R.string.sheet_caption_reps,
                        formatWeight(record.maxRepsWeight),
                    ),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatTile(
                    title = stringResource(R.string.sheet_estimated_1rm),
                    value = formatWeight(record.estOneRep) + " kg",
                    caption = stringResource(R.string.sheet_estimated_1rm_formula),
                    modifier = Modifier.weight(1f),
                )
                if (record.actualOneRep > 0.0) {
                    StatTile(
                        title = stringResource(R.string.sheet_actual_1rm),
                        value = formatWeight(record.actualOneRep) + " kg",
                        caption = stringResource(R.string.sheet_caption_single),
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    StatTile(
                        title = stringResource(R.string.sheet_best_day_volume),
                        value = formatVolume(
                            record.sessions.maxOfOrNull { it.volume } ?: 0.0,
                        ) + " kg",
                        caption = stringResource(R.string.sheet_caption_best_day),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.sheet_progressive_overload),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.sheet_est_1rm_per_session),
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Sparkline(
                    values = record.sessions.takeLast(10).map { it.estOneRep.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .padding(top = 10.dp),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.sheet_recent_sessions),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.sheet_sessions_total, record.sessions.size),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            record.sessions.takeLast(6).reversed().forEachIndexed { index, session ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = shortDate(session.date),
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(74.dp),
                    )
                    Text(
                        text = "${formatWeight(session.topWeight)} kg × ${formatWeight(session.topReps)}",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = formatTonsOrKg(session.volume),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** One small titled stat card used by the sheet's 2x2 grid. */
@Composable
private fun StatTile(
    title: String,
    value: String,
    caption: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(13.dp),
    ) {
        Text(
            text = title.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.6).sp,
        )
        Text(
            text = caption,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
