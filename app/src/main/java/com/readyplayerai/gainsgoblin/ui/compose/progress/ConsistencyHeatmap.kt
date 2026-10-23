package com.readyplayerai.gainsgoblin.ui.compose.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.domain.ConsistencyCell

private val WeekdayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

/**
 * Renders the last [weeks] Monday-start weeks as a weekday grid: one row per
 * weekday and one column per week, oldest on the left. Cell colour intensifies
 * with the day's [ConsistencyCell.level], and today's cell is outlined.
 * [cells] must be date-ascending with exactly `weeks * 7` entries starting on a
 * Monday, where the index is `weekIndex * 7 + weekdayIndex`.
 */
@Composable
fun ConsistencyHeatmap(
    cells: List<ConsistencyCell>,
    weeks: Int,
    today: String,
    modifier: Modifier = Modifier,
) {
    val surface = MaterialTheme.colorScheme.surfaceContainer
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier.fillMaxWidth()) {
        for (weekdayIndex in 0 until 7) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = WeekdayLabels[weekdayIndex],
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(13.dp),
                )
                Spacer(Modifier.width(5.dp))
                for (weekIndex in 0 until weeks) {
                    val index = weekIndex * 7 + weekdayIndex
                    val cell = cells.getOrNull(index)
                    val color = when (cell?.level) {
                        1 -> lerp(surface, primary, 0.26f)
                        2 -> lerp(surface, primary, 0.50f)
                        3 -> lerp(surface, primary, 0.74f)
                        else -> surface
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.5.dp)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(7.dp))
                            .background(color)
                            .then(
                                if (cell?.date == today) {
                                    Modifier.border(2.dp, onSurface, RoundedCornerShape(7.dp))
                                } else {
                                    Modifier
                                }
                            ),
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.insights_legend_oldest),
                fontSize = 11.5.sp,
                color = onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val swatchColors = listOf(
                    surface,
                    lerp(surface, primary, 0.26f),
                    lerp(surface, primary, 0.50f),
                    lerp(surface, primary, 0.74f),
                    primary,
                )
                for (color in swatchColors) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(color),
                    )
                }
            }
            Text(
                text = stringResource(R.string.insights_legend_newest),
                fontSize = 11.5.sp,
                color = onSurfaceVariant,
            )
        }
    }
}
