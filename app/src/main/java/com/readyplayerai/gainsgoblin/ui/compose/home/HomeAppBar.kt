package com.readyplayerai.gainsgoblin.ui.compose.home

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.ui.compose.FlexibleHeaderBar

/**
 * Home's header: day-of-week title + full date over the shared [FlexibleHeaderBar];
 * delegates all layout/animation.
 */
@Composable
fun HomeAppBar(
    weekdayTitle: String,
    fullDate: String,
    collapseFraction: Float,
    onTodayClick: () -> Unit,
    onDateIconClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    FlexibleHeaderBar(
        title = weekdayTitle,
        subtitle = fullDate,
        collapseFraction = collapseFraction,
        modifier = modifier,
        actions = {
            IconButton(
                onClick = onDateIconClick,
                modifier = Modifier.size(44.dp).clip(CircleShape),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_home_pick_date),
                    contentDescription = stringResource(R.string.home_pick_date),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(
                onClick = onTodayClick,
                modifier = Modifier.size(44.dp).clip(CircleShape),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_home_today),
                    contentDescription = stringResource(R.string.top_bar_today),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
    )
}
