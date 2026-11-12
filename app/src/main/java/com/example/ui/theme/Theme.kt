package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val CyberObsidianDarkScheme = darkColorScheme(
    primary = ZaiPrimaryCyan,
    onPrimary = Color(0xFF042F2E),
    primaryContainer = ElectricCyanContainer,
    onPrimaryContainer = OnElectricCyanContainer,
    secondary = ZaiSecondaryBlue,
    onSecondary = Color(0xFF06162E),
    secondaryContainer = NeuralVioletContainer,
    onSecondaryContainer = OnNeuralVioletContainer,
    tertiary = ZaiSuccessGreen,
    onTertiary = Color(0xFF022C22),
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = Color(0xFFA7F3D0),
    background = ZaiMainBackground,
    onBackground = ZaiPrimaryText,
    surface = ZaiHeaderSurface,
    onSurface = ZaiPrimaryText,
    surfaceVariant = ZaiFeatureCard,
    onSurfaceVariant = ZaiSecondaryText,
    outline = ZaiBorder,
    error = CrimsonAlert,
    onError = Color.White
)

private val DaylightCleanScheme = lightColorScheme(
    primary = ZaiLightPrimaryCyan,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0C4A6E),
    secondary = ZaiLightSecondaryBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF4C1D95),
    tertiary = ZaiLightSuccessGreen,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF064E3B),
    background = ZaiLightMainBackground,
    onBackground = ZaiLightPrimaryText,
    surface = ZaiLightHeaderSurface,
    onSurface = ZaiLightPrimaryText,
    surfaceVariant = ZaiLightFeatureCard,
    onSurfaceVariant = ZaiLightSecondaryText,
    outline = ZaiLightBorder,
    error = Color(0xFFDC2626),
    onError = Color.White
)

@Composable
fun OmniWorkspaceTheme(
    darkTheme: Boolean = true,
    themeMode: AppThemeMode? = null,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val resolvedDark = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> systemDark
        null -> darkTheme
    }

    val colorScheme = if (resolvedDark) CyberObsidianDarkScheme else DaylightCleanScheme
    val zaiTokens = if (resolvedDark) DarkZaiTokens else LightZaiTokens

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                // Dark background -> white status bar icons (!resolvedDark = false)
                controller.isAppearanceLightStatusBars = !resolvedDark
                controller.isAppearanceLightNavigationBars = !resolvedDark
            }
        }
    }

    CompositionLocalProvider(LocalZaiTokens provides zaiTokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    OmniWorkspaceTheme(darkTheme = darkTheme, content = content)
}
