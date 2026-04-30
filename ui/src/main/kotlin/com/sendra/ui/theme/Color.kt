package com.sendra.ui.theme

import androidx.compose.ui.graphics.Color

// Sendra Brand Colors - Electric Blue & Vibrant Purple
// Light theme colors
val md_theme_light_primary = Color(0xFF2563EB)          // Electric Blue
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFFDBEAFE) // Light blue container
val md_theme_light_onPrimaryContainer = Color(0xFF1E40AF)
val md_theme_light_secondary = Color(0xFF7C3AED)      // Vibrant Purple
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = Color(0xFFEDE9FE) // Light purple container
val md_theme_light_onSecondaryContainer = Color(0xFF5B21B6)
val md_theme_light_tertiary = Color(0xFF059669)         // Teal/Success
val md_theme_light_onTertiary = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer = Color(0xFFD1FAE5) // Light teal container
val md_theme_light_onTertiaryContainer = Color(0xFF065F46)
val md_theme_light_error = Color(0xFFDC2626)            // Red
val md_theme_light_errorContainer = Color(0xFFFEE2E2)
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_onErrorContainer = Color(0xFF991B1B)
val md_theme_light_background = Color(0xFFF8FAFC)     // True light background
val md_theme_light_onBackground = Color(0xFF0F172A)     // Dark slate text
val md_theme_light_surface = Color(0xFFFFFFFF)         // Pure white surface
val md_theme_light_onSurface = Color(0xFF0F172A)
val md_theme_light_surfaceVariant = Color(0xFFF1F5F9)   // Light gray variant
val md_theme_light_onSurfaceVariant = Color(0xFF475569) // Medium gray text
val md_theme_light_outline = Color(0xFF94A3B8)        // Border color
val md_theme_light_inverseOnSurface = Color(0xFFF1F5F9)
val md_theme_light_inverseSurface = Color(0xFF1E293B)
val md_theme_light_inversePrimary = Color(0xFF60A5FA)
val md_theme_light_surfaceTint = Color(0xFF2563EB)
val md_theme_light_outlineVariant = Color(0xFFE2E8F0)
val md_theme_light_scrim = Color(0xFF000000)

// Dark theme colors
val md_theme_dark_primary = Color(0xFF60A5FA)           // Light blue
val md_theme_dark_onPrimary = Color(0xFF1E3A8A)
val md_theme_dark_primaryContainer = Color(0xFF1E40AF)
val md_theme_dark_onPrimaryContainer = Color(0xFFBFDBFE)
val md_theme_dark_secondary = Color(0xFFA78BFA)       // Light purple
val md_theme_dark_onSecondary = Color(0xFF5B21B6)
val md_theme_dark_secondaryContainer = Color(0xFF6D28D9)
val md_theme_dark_onSecondaryContainer = Color(0xFFDDD6FE)
val md_theme_dark_tertiary = Color(0xFF34D399)        // Light teal
val md_theme_dark_onTertiary = Color(0xFF065F46)
val md_theme_dark_tertiaryContainer = Color(0xFF059669)
val md_theme_dark_onTertiaryContainer = Color(0xFFA7F3D0)
val md_theme_dark_error = Color(0xFFFCA5A5)         // Light red
val md_theme_dark_errorContainer = Color(0xFF991B1B)
val md_theme_dark_onError = Color(0xFF450A0A)
val md_theme_dark_onErrorContainer = Color(0xFFFECACA)
val md_theme_dark_background = Color(0xFF0F172A)      // True dark background
val md_theme_dark_onBackground = Color(0xFFF1F5F9)
val md_theme_dark_surface = Color(0xFF1E293B)       // Dark slate surface
val md_theme_dark_onSurface = Color(0xFFF1F5F9)
val md_theme_dark_surfaceVariant = Color(0xFF334155) // Dark gray variant
val md_theme_dark_onSurfaceVariant = Color(0xFF94A3B8)
val md_theme_dark_outline = Color(0xFF475569)
val md_theme_dark_inverseOnSurface = Color(0xFF0F172A)
val md_theme_dark_inverseSurface = Color(0xFFF1F5F9)
val md_theme_dark_inversePrimary = Color(0xFF2563EB)
val md_theme_dark_surfaceTint = Color(0xFF60A5FA)
val md_theme_dark_outlineVariant = Color(0xFF334155)
val md_theme_dark_scrim = Color(0xFF000000)

// Sendra-specific accent colors
val SendraBlue = Color(0xFF2563EB)
val SendraBlueLight = Color(0xFF60A5FA)
val SendraPurple = Color(0xFF7C3AED)
val SendraPurpleLight = Color(0xFFA78BFA)
val SendraTeal = Color(0xFF059669)
val SendraTealLight = Color(0xFF34D399)
val SendraOrange = Color(0xFFF59E0B)
val SendraPink = Color(0xFFEC4899)

// Radar-specific colors
val RadarRingColor = SendraBlue.copy(alpha = 0.3f)
val RadarSweepColor = SendraBlueLight.copy(alpha = 0.2f)
val DeviceTrustedColor = SendraTeal
val DeviceNewColor = SendraOrange
val DeviceNormalColor = SendraPurple
