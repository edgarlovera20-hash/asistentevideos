package com.example.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = CodexWhite,
    onPrimary = CodexBlack,
    primaryContainer = CodexDarkSurface,
    onPrimaryContainer = CodexWhite,
    secondary = CodexGrayLight,
    onSecondary = CodexBlack,
    secondaryContainer = CodexSurfaceVariant,
    tertiary = CodexWhite,
    background = CodexBlack,
    onBackground = CodexWhite,
    surface = CodexDarkSurface,
    onSurface = CodexWhite,
    surfaceVariant = CodexSurfaceVariant,
    onSurfaceVariant = CodexGrayLight,
    outline = CodexBorder
)

private val LightColorScheme = lightColorScheme(
    primary = CodexBlackText,
    onPrimary = CodexLightSurface,
    primaryContainer = CodexLightSurfaceVariant,
    onPrimaryContainer = CodexBlackText,
    secondary = CodexLightGrayText,
    onSecondary = CodexLightSurface,
    secondaryContainer = CodexLightSurfaceVariant,
    tertiary = CodexBlackText,
    background = CodexLightBackground,
    onBackground = CodexBlackText,
    surface = CodexLightSurface,
    onSurface = CodexBlackText,
    surfaceVariant = CodexLightSurfaceVariant,
    onSurfaceVariant = CodexLightGrayText,
    outline = CodexLightBorder
)

@Composable
fun HeavenlyAIMeetingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our handcrafted palette
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

