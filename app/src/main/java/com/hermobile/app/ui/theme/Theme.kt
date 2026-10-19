package com.hermobile.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val PocketOrange = Color(0xFF0096FF)
val PocketBlue = Color(0xFF00D4FF)
val PocketGreen = Color(0xFF69D69E)
val PocketBackground = Color(0xFF0A1222)
val PocketSurface = Color(0xFF141E33)
val PocketSurfaceVariant = Color(0xFF1A2338)
val PocketOutline = Color(0xFF2A3854)
val ShopGradientStart = Color(0xFF0096FF)
val ShopGradientEnd = Color(0xFF00D4FF)

private val DarkColors = darkColorScheme(
    primary = PocketOrange,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF0B3B66),
    onPrimaryContainer = Color(0xFFD6EFFF),
    secondary = PocketBlue,
    onSecondary = Color(0xFF002233),
    tertiary = PocketGreen,
    onTertiary = Color(0xFF00391E),
    background = PocketBackground,
    onBackground = Color(0xFFE6EDF3),
    surface = PocketSurface,
    onSurface = Color(0xFFE6EDF3),
    surfaceVariant = PocketSurfaceVariant,
    onSurfaceVariant = Color(0xFF8B98AD),
    outline = PocketOutline,
    outlineVariant = Color(0xFF35466A),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0077CC),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6EFFF),
    onPrimaryContainer = Color(0xFF0B3B66),
    secondary = Color(0xFF0096FF),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF1B8A5A),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF6F8FA),
    onBackground = Color(0xFF1F2328),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1F2328),
    surfaceVariant = Color(0xFFEAEFF5),
    onSurfaceVariant = Color(0xFF57606A),
    outline = Color(0xFFD0D7DE),
    outlineVariant = Color(0xFFD8DEE4),
)

enum class AppThemeMode { SYSTEM, DARK, LIGHT }

@Composable
fun PocketTheme(themeMode: AppThemeMode = AppThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val isDark = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDark
            insetsController.isAppearanceLightNavigationBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = if (isDark) DarkColors else LightColors,
        content = content,
    )
}
