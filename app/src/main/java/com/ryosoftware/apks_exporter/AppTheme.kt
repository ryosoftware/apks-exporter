package com.ryosoftware.apks_exporter

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.ryosoftware.utilities.StatusBarUtilities

private val ExpressDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF9242),
    onPrimary = Color(0xFF4A1C00),
    primaryContainer = Color(0xFF6B3100),
    onPrimaryContainer = Color(0xFFFFDBC4),
    secondary = Color(0xFFFFB786),
    onSecondary = Color(0xFF4E2500),
    secondaryContainer = Color(0xFF6E3700),
    onSecondaryContainer = Color(0xFFFFDBC4),
    tertiary = Color(0xFF00B8B8),
    onTertiary = Color(0xFF003737),
    tertiaryContainer = Color(0xFF005050),
    onTertiaryContainer = Color(0xFFB2DFDF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1C100A),
    onBackground = Color(0xFFFFDBC4),
    surface = Color(0xFF1C100A),
    onSurface = Color(0xFFFFDBC4),
    surfaceVariant = Color(0xFF503F35),
    onSurfaceVariant = Color(0xFFD5BEB1),
    outline = Color(0xFF9D897C),
    outlineVariant = Color(0xFF503F35),
    inverseSurface = Color(0xFFFFDBC4),
    inverseOnSurface = Color(0xFF32251C),
    inversePrimary = Color(0xFF8F4700),
    surfaceTint = Color(0xFFFF9242),
    scrim = Color.Black
)

private val ExpressLightColorScheme = lightColorScheme(
    primary = Color(0xFF8F4700),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBC4),
    onPrimaryContainer = Color(0xFF2E1200),
    secondary = Color(0xFF965A28),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDBC4),
    onSecondaryContainer = Color(0xFF341800),
    tertiary = Color(0xFF006B6B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFB2DFDF),
    onTertiaryContainer = Color(0xFF002020),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFF8F5),
    onBackground = Color(0xFF221A14),
    surface = Color(0xFFFFF8F5),
    onSurface = Color(0xFF221A14),
    surfaceVariant = Color(0xFFF2DFD4),
    onSurfaceVariant = Color(0xFF51443B),
    outline = Color(0xFF84746A),
    outlineVariant = Color(0xFFD5C3B9),
    inverseSurface = Color(0xFF382E28),
    inverseOnSurface = Color(0xFFFEEADC),
    inversePrimary = Color(0xFFFFB786),
    surfaceTint = Color(0xFF8F4700),
    scrim = Color.Black
)

@Composable
fun AppTheme(
    activity: Activity,
    content: @Composable () -> Unit
) {
    val theme by ApplicationPreferences.observe(
        ApplicationPreferences.THEME_KEY,
        ApplicationPreferences.THEME_DEFAULT
    ).collectAsState(initial = ApplicationPreferences.get(
        ApplicationPreferences.THEME_KEY,
        ApplicationPreferences.THEME_DEFAULT
    ))

    val useSystemAccentColor by ApplicationPreferences.observe(
        ApplicationPreferences.USE_SYSTEM_ACCENT_COLOR_KEY,
        ApplicationPreferences.USE_SYSTEM_ACCENT_COLOR_DEFAULT
    ).collectAsState(initial = ApplicationPreferences.get(
        ApplicationPreferences.USE_SYSTEM_ACCENT_COLOR_KEY,
        ApplicationPreferences.USE_SYSTEM_ACCENT_COLOR_DEFAULT
    ))

    val blackBackground by ApplicationPreferences.observe(
        ApplicationPreferences.BLACK_BACKGROUND_KEY,
        ApplicationPreferences.BLACK_BACKGROUND_DEFAULT
    ).collectAsState(initial = ApplicationPreferences.get(
        ApplicationPreferences.BLACK_BACKGROUND_KEY,
        ApplicationPreferences.BLACK_BACKGROUND_DEFAULT
    ))

    val isDarkTheme =
        theme == ApplicationPreferences.THEME_DARK ||
                (theme == ApplicationPreferences.THEME_SYSTEM && isSystemInDarkTheme())

    var colorScheme: ColorScheme = if (useSystemAccentColor) {
        if (isDarkTheme) dynamicDarkColorScheme(LocalContext.current)
        else dynamicLightColorScheme(LocalContext.current)
    } else {
        if (isDarkTheme) ExpressDarkColorScheme else ExpressLightColorScheme
    }

    if (blackBackground && isDarkTheme) {
        colorScheme = colorScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceVariant = Color(0xFF1C1C1C),
            surfaceContainerLowest = Color(0xFF050505),
            surfaceContainerLow = Color(0xFF0D0D0D),
            surfaceContainer = Color.Black,
            surfaceContainerHigh = Color(0xFF1A1A1A),
            surfaceContainerHighest = Color(0xFF2C2C2C),
            surfaceTint = colorScheme.primary,
            outline = Color(0xFF444444),
            outlineVariant = Color(0xFF2C2C2C),
            onSurface = Color(0xFFE0E0E0),
            onSurfaceVariant = Color(0xFFB0B0B0)
        )
    }

    LaunchedEffect(colorScheme) {
        StatusBarUtilities.setColor(activity, colorScheme.primary.toArgb())
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes
    ) {
        content()
    }
}
