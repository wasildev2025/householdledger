package com.example.householdledger.ui.theme

import androidx.compose.ui.graphics.Color

// Palette v2 — indigo primary with deep navy surfaces for the hero card.
// Inspired by modern fintech references (clean white canvas, saturated indigo
// action color, cinematic dark-navy balance hero).

// ---- Primary / Indigo ----
val IndigoPrimary = Color(0xFF4F46E5)          // indigo-600
val IndigoOnPrimary = Color(0xFFFFFFFF)
val IndigoPrimaryContainer = Color(0xFFE0E7FF) // indigo-100
val IndigoOnPrimaryContainer = Color(0xFF3730A3)

// Secondary is the "dark pill" selected state — near black slate.
val SlateSecondary = Color(0xFF0F172A)          // slate-900
val SlateOnSecondary = Color(0xFFFFFFFF)
val SlateSecondaryContainer = Color(0xFFE2E8F0) // slate-200
val SlateOnSecondaryContainer = Color(0xFF0F172A)

// Tertiary — cyan accent for chips and small highlights
val CyanTertiary = Color(0xFF06B6D4)
val CyanOnTertiary = Color(0xFFFFFFFF)
val CyanTertiaryContainer = Color(0xFFCFFAFE)
val CyanOnTertiaryContainer = Color(0xFF164E63)

// Light neutrals
val LightBackground = Color(0xFFFAFAFA)
val LightOnBackground = Color(0xFF0F172A)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF0F172A)
val LightSurfaceVariant = Color(0xFFF1F5F9)
val LightOnSurfaceVariant = Color(0xFF64748B)
val LightOutline = Color(0xFFCBD5E1)
val LightOutlineVariant = Color(0xFFE2E8F0)
val LightScrim = Color(0x52000000)

val LightError = Color(0xFFDC2626)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFEE2E2)
val LightOnErrorContainer = Color(0xFF7F1D1D)

// ---- Dark scheme ----
val IndigoPrimaryDark = Color(0xFF818CF8)       // indigo-400
val IndigoOnPrimaryDark = Color(0xFF1E1B4B)
val IndigoPrimaryContainerDark = Color(0xFF3730A3)
val IndigoOnPrimaryContainerDark = Color(0xFFE0E7FF)

val SlateSecondaryDark = Color(0xFFE2E8F0)
val SlateOnSecondaryDark = Color(0xFF0F172A)
val SlateSecondaryContainerDark = Color(0xFF1E293B)
val SlateOnSecondaryContainerDark = Color(0xFFE2E8F0)

val CyanTertiaryDark = Color(0xFF67E8F9)
val CyanOnTertiaryDark = Color(0xFF164E63)
val CyanTertiaryContainerDark = Color(0xFF0E7490)
val CyanOnTertiaryContainerDark = Color(0xFFCFFAFE)

val DarkBackground = Color(0xFF020617)          // slate-950
val DarkOnBackground = Color(0xFFF1F5F9)
val DarkSurface = Color(0xFF0F172A)
val DarkOnSurface = Color(0xFFF1F5F9)
val DarkSurfaceVariant = Color(0xFF1E293B)
val DarkOnSurfaceVariant = Color(0xFF94A3B8)
val DarkOutline = Color(0xFF334155)
val DarkOutlineVariant = Color(0xFF1E293B)
val DarkScrim = Color(0x52000000)

val DarkError = Color(0xFFF87171)
val DarkOnError = Color(0xFF7F1D1D)
val DarkErrorContainer = Color(0xFF450A0A)
val DarkOnErrorContainer = Color(0xFFFECACA)

// ---- Semantic income/expense ----
val IncomeLight = Color(0xFF15803D)
val IncomeLightContainer = Color(0xFFDCFCE7)
val IncomeDark = Color(0xFF4ADE80)
val IncomeDarkContainer = Color(0xFF14532D)

val ExpenseLight = Color(0xFFDC2626)
val ExpenseLightContainer = Color(0xFFFEE2E2)
val ExpenseDark = Color(0xFFF87171)
val ExpenseDarkContainer = Color(0xFF450A0A)

val WarningLight = Color(0xFFB45309)
val WarningDark = Color(0xFFFCD34D)

// ---- Hero (balance card) gradient ----
// Cinematic dark navy → royal blue, used behind the big balance display.
val HeroGradientTop = Color(0xFF0B1437)    // very dark blue-black
val HeroGradientMid = Color(0xFF1E3A8A)    // indigo-900
val HeroGradientBottom = Color(0xFF2B3BB5) // brighter indigo
val HeroSheen = Color(0xFF6366F1)          // diagonal sheen overlay
