package com.readyplayerai.gainsgoblin.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Fixed brand palette for the seven body-part categories. Light values match the
// legacy single-palette map in ui/compose/home/HomeDayPage.kt; this component owns
// its own constants and never reads MaterialTheme.colorScheme.

private val LightCategoryColors: Map<String, Color> = mapOf(
    "chest" to Color(0xFF5C589D),
    "back" to Color(0xFF28B0C0),
    "shoulders" to Color(0xFF0074BD),
    "biceps" to Color(0xFFFF3232),
    "triceps" to Color(0xFFCC9A00),
    "legs" to Color(0xFFD41961),
    "abs" to Color(0xFFFF99AB),
)

private val DarkCategoryColors: Map<String, Color> = mapOf(
    "chest" to Color(0xFFA29EE0),
    "back" to Color(0xFF62D3E0),
    "shoulders" to Color(0xFF7CC0EE),
    "biceps" to Color(0xFFFF8080),
    "triceps" to Color(0xFFEBC95E),
    "legs" to Color(0xFFF27AA5),
    "abs" to Color(0xFFFFB9C6),
)

private val FallbackLight = Color(0xFF343A40)
private val FallbackDark = Color(0xFFC4C9D0)

/** Octagon marker: 30% 0, 70% 0, 100% 30%, 100% 70%, 70% 100%, 30% 100%, 0 70%, 0 30%. */
private val ShouldersShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    moveTo(0.30f * w, 0f)
    lineTo(0.70f * w, 0f)
    lineTo(w, 0.30f * h)
    lineTo(w, 0.70f * h)
    lineTo(0.70f * w, h)
    lineTo(0.30f * w, h)
    lineTo(0f, 0.70f * h)
    lineTo(0f, 0.30f * h)
    close()
}

/** Organic asymmetric blob built from four cubic Béziers, clockwise from the top edge. */
private val BicepsShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    moveTo(0.58f * w, 0f)
    cubicTo(0.73f * w, 0f, w, 0.27f * h, w, 0.40f * h)
    cubicTo(w, 0.50f * h, 0.55f * w, 0.87f * h, 0.45f * w, h)
    cubicTo(0.30f * w, h, 0f, 0.65f * h, 0f, 0.52f * h)
    cubicTo(0f, 0.42f * h, 0.48f * w, 0.13f * h, 0.58f * w, 0f)
    close()
}

/** Downward kite / horseshoe: 50% 4%, 96% 90%, 50% 100%, 4% 90%. */
private val TricepsShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    moveTo(0.50f * w, 0.04f * h)
    lineTo(0.96f * w, 0.90f * h)
    lineTo(0.50f * w, h)
    lineTo(0.04f * w, 0.90f * h)
    close()
}

/** Diamond marker: a lightly rounded square rotated 45°, filling the marker slot. */
private val AbsShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val r = 0.18f
    val cx = 0.5f * w
    val cy = 0.5f * h
    // Top corner
    moveTo(cx * (1f + r), cy * r)
    // Right corner
    lineTo(w * (1f - 0.5f * r), cy * (1f - r))
    quadraticBezierTo(w, cy, w * (1f - 0.5f * r), cy * (1f + r))
    // Bottom corner
    lineTo(cx * (1f + r), h * (1f - 0.5f * r))
    quadraticBezierTo(cx, h, cx * (1f - r), h * (1f - 0.5f * r))
    // Left corner
    lineTo(cx * r, cy * (1f + r))
    quadraticBezierTo(0f, cy, cx * r, cy * (1f - r))
    // Back to the top corner
    lineTo(cx * (1f - r), cy * r)
    quadraticBezierTo(cx, 0f, cx * (1f + r), cy * r)
    close()
}

/**
 * Marker [Shape] for [category]; unknown or null categories fall back to a small
 * circle. Matching is case-insensitive.
 */
@Composable
fun categoryShape(category: String?): Shape = when (category?.lowercase()) {
    "chest" -> RoundedCornerShape(
        topStart = CornerSize(50),
        topEnd = CornerSize(50),
        bottomEnd = CornerSize(6.dp),
        bottomStart = CornerSize(6.dp),
    )
    "back" -> RoundedCornerShape(30)
    "shoulders" -> ShouldersShape
    "biceps" -> BicepsShape
    "triceps" -> TricepsShape
    "legs" -> RoundedCornerShape(
        topStart = 2.dp,
        topEnd = 12.dp,
        bottomEnd = 2.dp,
        bottomStart = 12.dp,
    )
    "abs" -> AbsShape
    else -> CircleShape
}

/**
 * Theme-aware category [Color]: a lifted palette on dark themes, the legacy light
 * palette otherwise. Unknown or null categories return a neutral fallback.
 */
@Composable
fun categoryColor(category: String?): Color {
    val dark = isSystemInDarkTheme()
    val palette = if (dark) DarkCategoryColors else LightCategoryColors
    val fallback = if (dark) FallbackDark else FallbackLight
    val key = category?.lowercase()
    return if (key == null) fallback else palette[key] ?: fallback
}

/**
 * 40dp circular avatar showing the 22dp category marker, with the avatar wash at
 * 16% alpha. Decorative: semantics come from the accompanying label text.
 */
@Composable
fun CategoryIcon(category: String?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(categoryColor(category).copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        CategoryMarker(category = category, size = 22.dp)
    }
}

/**
 * Standalone filled category marker, e.g. for chips or dense rows. Abs renders at
 * 82% scale to match the legend. Decorative: no content description.
 */
@Composable
fun CategoryMarker(category: String?, size: Dp = 22.dp, modifier: Modifier = Modifier) {
    val isAbs = category?.lowercase() == "abs"
    Box(
        modifier = modifier
            .size(if (isAbs) size * 0.82f else size)
            .clip(categoryShape(category))
            .background(categoryColor(category)),
    )
}
