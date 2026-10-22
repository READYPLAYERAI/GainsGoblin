package com.readyplayerai.gainsgoblin.ui.compose

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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp

/**
 * Flexible collapsing header shared by the Home and Exercises screens,
 * following the M3 Expressive large flexible top app bar anatomy: 152dp
 * expanded (title block bottom-anchored with 28dp bottom padding) -> 64dp
 * collapsed (title centered, 12dp bottom padding) as [collapseFraction] grows.
 * The title shrinks 38sp -> 22sp; the subtitle fades and collapses. Trailing
 * per-screen buttons supplied through the [actions] slot are vertically
 * centered against the title block.
 */
@Composable
fun FlexibleHeaderBar(
    title: String,
    subtitle: String,
    collapseFraction: Float,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val f = collapseFraction.coerceIn(0f, 1f)
    val titleSize = (38f + (22f - 38f) * f).sp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(lerp(152.dp, 64.dp, f))
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.Bottom)
                .fillMaxWidth()
                .padding(bottom = lerp(28.dp, 12.dp, f), end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 24.dp)
                    .clipToBounds(),
            ) {
                AnimatedContent(
                    targetState = title,
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
                    label = "flexibleHeaderTitle",
                ) { t ->
                    Text(
                        text = t,
                        style = TextStyle(fontSize = titleSize, fontWeight = FontWeight(650)),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                AnimatedContent(
                    targetState = subtitle,
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
                    label = "flexibleHeaderSubtitle",
                ) { s ->
                    Text(
                        text = s,
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
            Row(content = actions)
        }
    }
}
