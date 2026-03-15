package com.openbiblescholar.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── Parchment / Warm Study Palette ──────────────────────────────────────────
object OBSColors {
    val ParchmentLight   = Color(0xFFFDF6E3)
    val ParchmentMedium  = Color(0xFFF5E6C8)
    val InkDark          = Color(0xFF2C1810)
    val InkMedium        = Color(0xFF4A2C17)
    val AccentGold       = Color(0xFFC8922A)
    val AccentGoldDark   = Color(0xFFE8B84B)
    val ChapterBlue      = Color(0xFF1565C0)
    val VerseGray        = Color(0xFF757575)
    val HighlightYellow  = Color(0xFFFFEE58)
    val HighlightGreen   = Color(0xFFA5D6A7)
    val HighlightBlue    = Color(0xFF90CAF9)
    val HighlightPink    = Color(0xFFF48FB1)
    val HighlightPurple  = Color(0xFFCE93D8)
    val NightBg          = Color(0xFF121212)
    val NightSurface     = Color(0xFF1E1E1E)
    val NightOnSurface   = Color(0xFFE8D5B0)
    val NightAccent      = Color(0xFFE8B84B)
    val SepiaBase        = Color(0xFFF4ECD8)
    val ErrorRed         = Color(0xFFB71C1C)
}

private val LightColorScheme = lightColorScheme(
    primary          = OBSColors.AccentGold,
    onPrimary        = Color.White,
    primaryContainer = OBSColors.ParchmentMedium,
    onPrimaryContainer = OBSColors.InkDark,
    secondary        = OBSColors.ChapterBlue,
    onSecondary      = Color.White,
    background       = OBSColors.ParchmentLight,
    onBackground     = OBSColors.InkDark,
    surface          = Color.White,
    onSurface        = OBSColors.InkMedium,
    surfaceVariant   = OBSColors.ParchmentMedium,
    onSurfaceVariant = OBSColors.InkMedium,
    error            = OBSColors.ErrorRed,
    outline          = OBSColors.AccentGold.copy(alpha = 0.5f)
)

private val DarkColorScheme = darkColorScheme(
    primary          = OBSColors.NightAccent,
    onPrimary        = OBSColors.InkDark,
    primaryContainer = Color(0xFF3E2723),
    onPrimaryContainer = OBSColors.NightOnSurface,
    secondary        = Color(0xFF64B5F6),
    onSecondary      = OBSColors.InkDark,
    background       = OBSColors.NightBg,
    onBackground     = OBSColors.NightOnSurface,
    surface          = OBSColors.NightSurface,
    onSurface        = OBSColors.NightOnSurface,
    surfaceVariant   = Color(0xFF2D2D2D),
    onSurfaceVariant = OBSColors.NightOnSurface.copy(alpha = 0.8f),
    error            = Color(0xFFEF9A9A),
    outline          = OBSColors.NightAccent.copy(alpha = 0.4f)
)

// Reader theme enum
enum class ReaderTheme { LIGHT, DARK, SEPIA, HIGH_CONTRAST }

@Composable
fun OpenBibleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = OBSTypography,
        content = content
    )
}
