package dev.remgr.f1.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import dev.remgr.f1.core.settings.SettingsRepository

// Fallback palette — Material You dynamic color used when API >= 31.
private val F1Red = Color(0xFFE8002D)
private val F1RedDark = Color(0xFFFFB3B0)

private val DarkColors = darkColorScheme(
    primary          = F1Red,
    onPrimary        = Color.White,
    primaryContainer = Color(0xFF5B0013),
    secondary        = Color(0xFFCFB26A),
    onSecondary      = Color(0xFF3B2C00),
    surface          = Color(0xFF151515),
    onSurface        = Color(0xFFF0F0F0),
    surfaceVariant   = Color(0xFF1E1E1E),
    background       = Color(0xFF0F0F0F),
    onBackground     = Color(0xFFF0F0F0),
    error            = Color(0xFFFF4C4C),
)

private val LightColors = lightColorScheme(
    primary          = F1Red,
    onPrimary        = Color.White,
    primaryContainer = Color(0xFFFFDAD9),
    secondary        = Color(0xFF7A5900),
    onSecondary      = Color.White,
    surface          = Color(0xFFFAF8FF),
    onSurface        = Color(0xFF1A1B1E),
    surfaceVariant   = Color(0xFFEFEFF4),
    background       = Color(0xFFFAF8FF),
    error            = Color(0xFFBA1A1A),
)

@Composable
fun F1Theme(
    themeMode: SettingsRepository.ThemeMode = SettingsRepository.ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val isSystemDark = isSystemInDarkTheme()
    
    val (useDark, useDynamic) = when (themeMode) {
        SettingsRepository.ThemeMode.SYSTEM       -> isSystemDark to false
        SettingsRepository.ThemeMode.LIGHT        -> false to false
        SettingsRepository.ThemeMode.DARK         -> true to false
        SettingsRepository.ThemeMode.MATERIAL_YOU -> isSystemDark to true
    }

    val colorScheme = when {
        useDynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (useDark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        useDark -> DarkColors
        else    -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = F1Typography,
        content     = content,
    )
}
