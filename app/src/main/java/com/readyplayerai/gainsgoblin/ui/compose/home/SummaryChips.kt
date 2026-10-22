package com.readyplayerai.gainsgoblin.ui.compose.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.model.WorkoutDay
import java.text.DecimalFormat

private val TNUM = TextStyle(fontFeatureSettings = "tnum")

private val volumeFormatter = DecimalFormat("#,##0.#")

/**
 * Per-day summary chips row.
 *
 * Training days ([realDay] with exercises) show two equal-width pills (volume
 * and "reps × exercises"); rest days show a single content-width rest pill.
 */
@Composable
fun SummaryChips(day: WorkoutDay, realDay: WorkoutDay?, modifier: Modifier = Modifier) {
    val exercises = realDay?.Exercises
    val isTrainingDay = !exercises.isNullOrEmpty()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (isTrainingDay) {
            BigChip(
                modifier = Modifier.weight(1f),
                iconRes = R.drawable.ic_assessment_24px,
            ) {
                Text(
                    text = volumeFormatter.format(realDay?.DayVolume ?: 0.0),
                    style = TNUM.copy(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "kg",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            BigChip(
                modifier = Modifier.weight(1f),
                iconRes = R.drawable.ic_poll_24px,
            ) {
                Text(
                    text = "${realDay?.Reps ?: 0} × ${exercises?.size ?: 0}",
                    style = TNUM.copy(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .height(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_alarm_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.home_rest_day_chip),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }
}

@Composable
private fun BigChip(
    modifier: Modifier,
    iconRes: Int,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        content()
    }
}
