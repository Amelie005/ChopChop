package com.example.chopchoprecipeapp.ui.theme

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

/**
 * Dark Color Scheme configuration.
 */
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    secondary = SecondaryDark,
    tertiary = TertiaryDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    error = MutedRed,

    //Green container equivalents for dark mode
    primaryContainer = Color(0xFF224229),
    onPrimaryContainer = Color(0xFFD2E8D6),
    secondaryContainer = Color(0xFF2C3E30),
    onSecondaryContainer = Color(0xFFC2D5C6),
    surfaceVariant = Color(0xFF1B2B20),
    onSurfaceVariant = Color(0xFFA2B5A6),
    outline = Color(0xFF728576),

    //Bottom Bar & Surface Containers for Dark Mode
    surfaceContainer = Color(0xFF121F17),
    surfaceContainerLow = Color(0xFF0E1912),
    surfaceContainerHigh = Color(0xFF1A2B20),
    surfaceContainerHighest = Color(0xFF223528),

    onPrimary = OnPrimaryDark,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = OnBackgroundDark,
    onSurface = OnSurfaceDark
)

/**
 * Light Color Scheme configuration.
 */
private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    secondary = SecondaryLight,
    tertiary = TertiaryLight,
    background = BackgroundLight,
    surface = SurfaceLight,
    error = MutedRed,

    //Green container equivalents for light mode
    primaryContainer = Color(0xFFD5EAD8),
    onPrimaryContainer = Color(0xFF0F3016),
    secondaryContainer = Color(0xFFE2EFE4),
    onSecondaryContainer = Color(0xFF2D3E31),
    surfaceVariant = Color(0xFFDFEBE1),
    onSurfaceVariant = Color(0xFF4C5E51),
    outline = Color(0xFF7E9183),

    //Bottom Bar & Surface Containers for Light Mode
    surfaceContainer = Color(0xFFE4EBE5),
    surfaceContainerLow = Color(0xFFEAF1EC),
    surfaceContainerHigh = Color(0xFFDCE5DE),
    surfaceContainerHighest = Color(0xFFD3DDD5),

    onPrimary = OnPrimaryLight,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = OnBackgroundLight,
    onSurface = OnSurfaceLight
)

/**
 * Custom Material Theme wrapper for the ChopChop Recipe App.
 * @author Amelie Dzierzawa
 */
@Composable
fun ChopChopRecipeAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
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