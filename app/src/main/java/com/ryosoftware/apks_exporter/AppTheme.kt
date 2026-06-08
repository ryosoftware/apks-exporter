package com.ryosoftware.apks_exporter

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import com.ryosoftware.utilities.StatusBarUtilities

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

    var colorScheme: ColorScheme = when {
        isDarkTheme -> dynamicDarkColorScheme(LocalContext.current)
        else -> dynamicLightColorScheme(LocalContext.current)
    }

    if (!useSystemAccentColor) {
        colorScheme = colorScheme.copy(
            primary = colorResource(R.color.primary),
            onPrimary = colorResource(R.color.on_primary),

            secondary = colorResource(R.color.secondary),
            onSecondary = colorResource(R.color.on_secondary),

            primaryContainer = colorResource(R.color.primary),
            onPrimaryContainer = colorResource(R.color.on_primary),

            secondaryContainer = colorResource(R.color.secondary),
            onSecondaryContainer = colorResource(R.color.on_secondary)
        )
    }

    if (blackBackground && isDarkTheme) {
        colorScheme = colorScheme.copy(
            background = Color.Black,
            surface = Color.Black
        )
    }

    LaunchedEffect(colorScheme) {
        StatusBarUtilities.setColor(activity, colorScheme.primary.toArgb())
    }

    MaterialTheme(colorScheme = colorScheme) {
        content()
    }
}
