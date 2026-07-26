package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Heavenly AI Meeting Palette — cian/índigo/violeta sobre azul marino, a juego con el
// ícono (alas + micrófono con halo). Los nombres "Codex*" se conservan porque Theme.kt y
// varias pantallas los referencian directamente; solo cambian los valores hexadecimales.
val CodexBlack = Color(0xFF0A0E17)        // Fondo principal — azul marino profundo
val CodexDarkSurface = Color(0xFF131C2E)  // Paneles
val CodexBorder = Color(0xFF2E3D60)       // Bordes sutiles
val CodexGrayLight = Color(0xFF94A3B8)    // Texto secundario
val CodexWhite = Color(0xFFF1F5F9)        // Texto e iconos principales

// Functional dark neutrals
val CodexSurfaceVariant = Color(0xFF1E2A45)
val CodexBorderLight = Color(0xFF2E3D60)

// Light-mode counterparts
val CodexLightBackground = Color(0xFFF8FAFC)
val CodexLightSurface = Color(0xFFFFFFFF)
val CodexLightBorder = Color(0xFFE2E8F0)
val CodexLightGrayText = Color(0xFF64748B)
val CodexLightSurfaceVariant = Color(0xFFE2E8F0)
val CodexBlackText = Color(0xFF0F172A)

// Semantic accent palette — used directly by several screens (Reminders, Visual IA, etc.)
val CyanPrimary = Color(0xFF00E5FF)
val CyanPrimaryDark = Color(0xFF00B8D4)
val IndigoSecondary = Color(0xFF6C5CE7)
val VioletAccent = Color(0xFFA855F7)

val DarkBackground = CodexBlack
val DarkSurface = CodexDarkSurface
val DarkSurfaceVariant = CodexSurfaceVariant
val DarkCardBorder = CodexBorder

val LightBackground = CodexLightBackground
val LightSurface = CodexLightSurface
val LightSurfaceVariant = CodexLightSurfaceVariant

val EmeraldSuccess = Color(0xFF10B981)
val AmberWarning = Color(0xFFF59E0B)
val RoseDanger = Color(0xFFEF4444)

val TextPrimaryDark = CodexWhite
val TextSecondaryDark = CodexGrayLight
val TextPrimaryLight = CodexBlackText
val TextSecondaryLight = CodexLightGrayText
