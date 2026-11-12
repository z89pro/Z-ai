package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Exact Z AI Assistant Design Token Specification (Dark Mode Default)
val ZaiMainBackground = Color(0xFF050B18)
val ZaiHeaderSurface = Color(0xFF0B1426)
val ZaiWelcomeCard = Color(0xFF112440)
val ZaiWelcomeCardBottom = Color(0xFF0B182E)
val ZaiFeatureCard = Color(0xFF111E34)
val ZaiInputSurface = Color(0xFF050D1E)
val ZaiPrimaryCyan = Color(0xFF06DDF5)
val ZaiSecondaryBlue = Color(0xFF299EFF)
val ZaiPrimaryText = Color(0xFFF5F8FF)
val ZaiSecondaryText = Color(0xFFA4B8D9)
val ZaiSuccessGreen = Color(0xFF10DFA0)
val ZaiBorder = Color(0xFF203B5F)

// Matching Light Mode Tokens (Preserving exact proportions, structure & hierarchy)
val ZaiLightMainBackground = Color(0xFFF3F7FD)
val ZaiLightHeaderSurface = Color(0xFFFFFFFF)
val ZaiLightWelcomeCard = Color(0xFFE6F0FA)
val ZaiLightWelcomeCardBottom = Color(0xFFDCEAFB)
val ZaiLightFeatureCard = Color(0xFFFFFFFF)
val ZaiLightInputSurface = Color(0xFFF8FAFF)
val ZaiLightPrimaryCyan = Color(0xFF0099B8)
val ZaiLightSecondaryBlue = Color(0xFF1A73E8)
val ZaiLightPrimaryText = Color(0xFF0B1528)
val ZaiLightSecondaryText = Color(0xFF4B6385)
val ZaiLightSuccessGreen = Color(0xFF059669)
val ZaiLightBorder = Color(0xFFCBD9ED)

// Legacy aliases used across existing workspace screens
val ObsidianBackground = ZaiMainBackground
val ObsidianSurface = ZaiHeaderSurface
val ObsidianCard = ZaiFeatureCard
val ObsidianCardElevated = ZaiWelcomeCard
val ObsidianBorder = ZaiBorder

val ElectricCyan = ZaiPrimaryCyan
val ElectricCyanBright = ZaiPrimaryCyan
val ElectricCyanContainer = Color(0xFF083344)
val OnElectricCyanContainer = Color(0xFFA5F3FC)

val NeuralViolet = Color(0xFF8B5CF6)
val NeuralVioletBright = Color(0xFFA78BFA)
val NeuralVioletContainer = Color(0xFF2E1065)
val OnNeuralVioletContainer = Color(0xFFDDD6FE)

val EmeraldEnclave = ZaiSuccessGreen
val EmeraldContainer = Color(0xFF064E3B)
val AmberConflict = Color(0xFFF59E0B)
val AmberContainer = Color(0xFF451A03)
val CrimsonAlert = Color(0xFFEF4444)

val SlateTextPrimary = ZaiPrimaryText
val SlateTextSecondary = ZaiSecondaryText
val SlateTextMuted = Color(0xFF64748B)

val DaylightBackground = ZaiLightMainBackground
val DaylightSurface = ZaiLightHeaderSurface
val DaylightSurfaceVariant = ZaiLightWelcomeCard
val DaylightPrimary = ZaiLightPrimaryCyan
val DaylightSecondary = ZaiLightSecondaryBlue
val DaylightTextPrimary = ZaiLightPrimaryText
val DaylightTextSecondary = ZaiLightSecondaryText

enum class AppThemeMode(val label: String) {
    DARK("Dark Mode"),
    LIGHT("Light Mode"),
    SYSTEM("System Default")
}

@Immutable
data class ZaiDesignTokens(
    val isDark: Boolean,
    val mainBackground: Color,
    val headerSurface: Color,
    val welcomeCardTop: Color,
    val welcomeCardBottom: Color,
    val featureCardSurface: Color,
    val inputSurface: Color,
    val primaryCyan: Color,
    val secondaryBlue: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val successGreen: Color,
    val border: Color
)

val DarkZaiTokens = ZaiDesignTokens(
    isDark = true,
    mainBackground = ZaiMainBackground,
    headerSurface = ZaiHeaderSurface,
    welcomeCardTop = ZaiWelcomeCard,
    welcomeCardBottom = ZaiWelcomeCardBottom,
    featureCardSurface = ZaiFeatureCard,
    inputSurface = ZaiInputSurface,
    primaryCyan = ZaiPrimaryCyan,
    secondaryBlue = ZaiSecondaryBlue,
    primaryText = ZaiPrimaryText,
    secondaryText = ZaiSecondaryText,
    successGreen = ZaiSuccessGreen,
    border = ZaiBorder
)

val LightZaiTokens = ZaiDesignTokens(
    isDark = false,
    mainBackground = ZaiLightMainBackground,
    headerSurface = ZaiLightHeaderSurface,
    welcomeCardTop = ZaiLightWelcomeCard,
    welcomeCardBottom = ZaiLightWelcomeCardBottom,
    featureCardSurface = ZaiLightFeatureCard,
    inputSurface = ZaiLightInputSurface,
    primaryCyan = ZaiLightPrimaryCyan,
    secondaryBlue = ZaiLightSecondaryBlue,
    primaryText = ZaiLightPrimaryText,
    secondaryText = ZaiLightSecondaryText,
    successGreen = ZaiLightSuccessGreen,
    border = ZaiLightBorder
)

val LocalZaiTokens = staticCompositionLocalOf { DarkZaiTokens }
