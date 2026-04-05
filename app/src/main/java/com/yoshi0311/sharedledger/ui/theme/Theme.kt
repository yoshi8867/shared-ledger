package com.yoshi0311.sharedledger.ui.theme

import android.app.Activity
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
    primary = IndigoLightDark,
    onPrimary = IndigoContainerDark,
    primaryContainer = IndigoContainerDark,
    onPrimaryContainer = IndigoLightDark,

    secondary = TealLightDark,
    onSecondary = TealContainerDark,
    secondaryContainer = TealContainerDark,
    onSecondaryContainer = TealLightDark,

    tertiary = PurpleLightDark,
    onTertiary = PurpleContainerDark,
    tertiaryContainer = PurpleContainerDark,
    onTertiaryContainer = PurpleLightDark,

    error = ErrorDark,
    background = BackgroundDark,
    onBackground = Color.White,
    surface = SurfaceDark,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = IndigoLight,
    onPrimary = OnIndigoLight,
    primaryContainer = IndigoLightContainer,
    onPrimaryContainer = IndigoLight,

    secondary = TealLight,
    onSecondary = OnTealLight,
    secondaryContainer = TealLightContainer,
    onSecondaryContainer = TealLight,

    tertiary = PurpleLight,
    onTertiary = OnPurpleLight,
    tertiaryContainer = PurpleLightContainer,
    onTertiaryContainer = PurpleLight,

    error = ErrorLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight
)

@Composable
fun SharedLedgerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
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
        typography = SharedLedgerTypography,
        content = content
    )
}