package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// High Density Theme Color Palette
val CyberObsidian = Color(0xFF0A0D14)
val CyberCardBg = Color(0xFF111726)
val CyberCardBorder = Color(0xFF1E2A3E)
val CyberSurfaceHover = Color(0xFF1A2338)
val CyberSurfaceInput = Color(0xFF0E1422)

/**
 * Dynamic Theme-Aware colors that automatically adapt when switching between Light and Dark mode.
 */
val AppSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surface

val AppCardBg: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surface

val AppCardBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surfaceVariant

val AppInputBg: Color
    @Composable
    @ReadOnlyComposable
    get() = if (MaterialTheme.colorScheme.background == LightBackground) Color(0xFFF1F5F9) else CyberSurfaceInput

val AppTextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurface

val AppTextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurfaceVariant

val AppSubtleBg: Color
    @Composable
    @ReadOnlyComposable
    get() = if (MaterialTheme.colorScheme.background == LightBackground) Color(0xFFF1F5F9) else Color(0xFF161E31)

val AppDivider: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surfaceVariant

val AppBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.background

val AgnesViolet = Color(0xFF7C3AED)
val AgnesVioletDark = Color(0xFF5B21B6)
val AgnesVioletLight = Color(0xFFA78BFA)

val AgnesCyan = Color(0xFF06B6D4)
val AgnesCyanGlow = Color(0xFF38BDF8)

val AgnesEmerald = Color(0xFF10B981)
val AgnesAmber = Color(0xFFF59E0B)
val AgnesRose = Color(0xFFF43F5E)

val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

// M3 standard palette mapping
val DarkPrimary = AgnesViolet
val DarkOnPrimary = Color(0xFFFFFFFF)
val DarkPrimaryContainer = Color(0xFF2E1065)
val DarkOnPrimaryContainer = Color(0xFFDDD6FE)

val DarkSecondary = AgnesCyan
val DarkOnSecondary = Color(0xFF032B36)
val DarkSecondaryContainer = Color(0xFF164E63)
val DarkOnSecondaryContainer = Color(0xFFBAE6FD)

val DarkBackground = CyberObsidian
val DarkOnBackground = TextPrimary
val DarkSurface = CyberCardBg
val DarkOnSurface = TextPrimary
val DarkSurfaceVariant = CyberCardBorder
val DarkOnSurfaceVariant = TextSecondary

// Light Theme Palette
val LightBackground = Color(0xFFF8FAFC)       // Crisp soft white
val LightOnBackground = Color(0xFF0F172A)     // Deep slate navy
val LightSurface = Color(0xFFFFFFFF)          // Pure white card
val LightOnSurface = Color(0xFF0F172A)
val LightSurfaceVariant = Color(0xFFE2E8F0)   // Soft border
val LightOnSurfaceVariant = Color(0xFF475569) // Secondary slate
val LightPrimaryContainer = Color(0xFFEDE9FE)
val LightOnPrimaryContainer = Color(0xFF5B21B6)
val LightSecondaryContainer = Color(0xFFCFFAFE)
val LightOnSecondaryContainer = Color(0xFF0E7490)

