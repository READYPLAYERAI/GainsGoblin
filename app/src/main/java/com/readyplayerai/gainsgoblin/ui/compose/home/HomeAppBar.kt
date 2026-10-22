package com.readyplayerai.gainsgoblin.ui.compose.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.readyplayerai.gainsgoblin.R

/**
 * The flexible Home bar: 152dp expanded -> 64dp compact as the day's collapse
 * fraction grows. The title shrinks and the date subtitle/dot fade away.
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
    val f = collapseFraction.coerceIn(0f, 1f)
    val titleSize = (38f + (22f - 38f) * f).sp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(lerp(152.dp, 64.dp, f))
            .background(MaterialTheme.colorScheme.surface)
            .padding(end = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 24.dp, top = 18.dp)
                .clipToBounds(),
        ) {
            AnimatedContent(
                targetState = weekdayTitle to fullDate,
                transitionSpec = {
                    ((fadeIn() + slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                    ) { height -> height / 3 }) togetherWith
                        (fadeOut() + slideOutVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMedium,
                            ),
                        ) { height -> -height / 3 })).using(SizeTransform(clip = false))
                },
                label = "homeAppBarTitle",
            ) { (title, subtitle) ->
                Column {
                    Text(
                        text = title,
                        style = TextStyle(fontSize = titleSize, fontWeight = FontWeight(650)),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = subtitle,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(lerp(20.dp, 0.dp, f))
                            .graphicsLayer { alpha = 1f - f }
                            .clipToBounds(),
                    )
                }
            }
        }
        IconButton(
            onClick = onDateIconClick,
            modifier = Modifier
                .padding(top = 18.dp)
                .size(44.dp)
                .clip(CircleShape),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_home_pick_date),
                contentDescription = stringResource(R.string.home_pick_date),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        IconButton(
            onClick = onTodayClick,
            modifier = Modifier
                .padding(top = 18.dp)
                .size(44.dp)
                .clip(CircleShape),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_home_today),
                contentDescription = stringResource(R.string.top_bar_today),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
