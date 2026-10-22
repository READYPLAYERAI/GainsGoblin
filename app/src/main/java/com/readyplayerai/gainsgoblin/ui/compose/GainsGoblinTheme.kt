package com.readyplayerai.gainsgoblin.ui.compose

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * M3 Expressive theme for Compose islands. Follows the system dark theme:
 * Material You dynamic color (light or dark) on Android 12+, otherwise the
 * baseline dark scheme or a light scheme derived from the legacy app palette.
 * Expressive motion (springy transitions, pill indicator) comes from the
 * expressive motion scheme. System-bar icon appearance follows the same
 * dark-mode signal, keeping status and navigation bar icons legible.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GainsGoblinTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val darkTheme: Boolean = isSystemInDarkTheme()
    val colorScheme: ColorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (darkTheme) darkColorScheme() else fallbackLightColorScheme()
    }
    val view = LocalView.current
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
    ) {
        if (!view.isInEditMode) {
            SideEffect {
                val window = findActivity(view.context)?.window ?: return@SideEffect
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
        content()
    }
}

private fun fallbackLightColorScheme() = lightColorScheme(
    primary = Color(0xFF0074BD),
    secondary = Color(0xFF00AEEF),
    background = Color(0xFFF6F6F6),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF262626),
    onSurface = Color(0xFF262626),
)

private fun findActivity(context: Context): Activity? {
    var ctx: Context = context
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
