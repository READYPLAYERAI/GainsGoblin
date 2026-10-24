package com.readyplayerai.gainsgoblin.ui.compose.exerciseprogress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.readyplayerai.gainsgoblin.R

/**
 * Compact 56dp app bar for the per-exercise progress detail.
 *
 * Back, log-set, favourite and overflow actions stay fully opaque while [title]
 * fades in with [titleAlpha] as the large header scrolls away. The bar owns its
 * own status-bar inset; the hosting `Scaffold` must not apply one.
 */
@Composable
fun ExerciseProgressTopBar(
    title: String,
    favorite: Boolean,
    titleAlpha: Float,
    onBack: () -> Unit,
    onLogSets: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back_24px),
                contentDescription = stringResource(R.string.exercise_progress_back),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
                .graphicsLayer { alpha = titleAlpha.coerceIn(0f, 1f) },
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.15).sp,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(onClick = onLogSets, modifier = Modifier.size(48.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_add_24px),
                contentDescription = stringResource(R.string.exercise_progress_log_sets),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(48.dp)) {
            Icon(
                painter = painterResource(
                    if (favorite) {
                        R.drawable.ic_star_fill0_wght400_grad0_opsz24
                    } else {
                        R.drawable.ic_star_outline_24px
                    },
                ),
                contentDescription = stringResource(
                    if (favorite) {
                        R.string.exercises_cd_favourite_remove
                    } else {
                        R.string.exercises_cd_favourite_add
                    },
                    title,
                ),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        IconButton(onClick = onMore, modifier = Modifier.size(48.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_more_vert_24px),
                contentDescription = stringResource(R.string.exercise_progress_more_actions),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
