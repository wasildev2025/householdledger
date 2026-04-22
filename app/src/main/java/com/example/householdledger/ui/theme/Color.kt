package com.example.householdledger.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
//  Visual system: warm cream + saffron accent + deep teal secondary.
//  Chosen over generic fintech grey-blue to give the app its own identity
//  and honor the cultural/household context (South Asian premium feel).
// ============================================================================

// ---- Light surfaces ----
val CreamBackground      = Color(0xFFFDFCF9)  // Even warmer cream
val CreamSurface         = Color(0xFFFFFDF8)  // Slightly tinted white
val CreamSurfaceTonal    = Color(0xFFF7F2E9)  // tonal/variant surfaces
val CreamOnBackground    = Color(0xFF1C1B19)
val CreamOnSurface       = Color(0xFF1C1B19)
val CreamOnSurfaceVariant= Color(0xFF706A62)
val CreamOutline         = Color(0xFFE5DFD4)
val CreamOutlineVariant  = Color(0xFFF0EAE0)

// ---- Dark surfaces ----
val CharcoalBackground   = Color(0xFF12110F)
val CharcoalSurface      = Color(0xFF1C1B18)
val CharcoalSurfaceTonal = Color(0xFF25231F)
val CharcoalOnBackground = Color(0xFFF5F1EA)
val CharcoalOnSurface    = Color(0xFFF5F1EA)
val CharcoalOnSurfaceVariant = Color(0xFFA09A90)
val CharcoalOutline      = Color(0xFF2C2A26)
val CharcoalOutlineVariant = Color(0xFF201E1B)

// ---- Saffron (primary) ----
val SaffronLight             = Color(0xFFE8833A)
val SaffronOnLight           = Color(0xFFFFFFFF)
val SaffronContainerLight    = Color(0xFFFDE8D3)
val SaffronOnContainerLight  = Color(0xFF6B3B12)

val SaffronDark              = Color(0xFFF39A55)
val SaffronOnDark            = Color(0xFF1A1208)
val SaffronContainerDark     = Color(0xFF3E2614)
val SaffronOnContainerDark   = Color(0xFFFDE8D3)

// ---- Deep Teal (secondary) ----
val TealLight             = Color(0xFF0F766E)
val TealOnLight           = Color(0xFFFFFFFF)
val TealContainerLight    = Color(0xFFCCFBF1)
val TealOnContainerLight  = Color(0xFF0C4A3F)

val TealDark              = Color(0xFF5EEAD4)
val TealOnDark            = Color(0xFF0C4A3F)
val TealContainerDark     = Color(0xFF115E57)
val TealOnContainerDark   = Color(0xFFCCFBF1)

// ---- Tertiary: Gold ----
val GoldLight             = Color(0xFFB08448)
val GoldContainerLight    = Color(0xFFF9EEDD) // Softer gold container
val GoldDark              = Color(0xFFE5C07B)
val GoldContainerDark     = Color(0xFF3D2E15)

// ---- Semantic: income / expense ----
val IncomeLight           = Color(0xFF2F8F6A)
val IncomeLightContainer  = Color(0xFFE6F4ED)
val IncomeOnContainerLight= Color(0xFF0E5337)
val IncomeDark            = Color(0xFF4EB387)
val IncomeDarkContainer   = Color(0xFF15402E)
val IncomeOnContainerDark = Color(0xFFBDE8D3)

val ExpenseLight          = Color(0xFFD14343)
val ExpenseLightContainer = Color(0xFFFDEDED)
val ExpenseOnContainerLight = Color(0xFF6B1414)
val ExpenseDark           = Color(0xFFEC6A6A)
val ExpenseDarkContainer  = Color(0xFF3E1818)
val ExpenseOnContainerDark= Color(0xFFFDEDED)

val WarningLight          = Color(0xFFB08448)
val WarningDark           = Color(0xFFE5C07B)

// ---- Error ----
val ErrorLight            = Color(0xFFC23A3A)
val ErrorLightContainer   = Color(0xFFFDEDED)
val ErrorOnLight          = Color(0xFFFFFFFF)
val ErrorDark             = Color(0xFFEC6A6A)
val ErrorDarkContainer    = Color(0xFF3E1818)
val ErrorOnDark           = Color(0xFF2E0C0C)

val Scrim                 = Color(0x73000000)

// ---- Pulse Hero Gradients ----
val PulseGradientTop      = Color(0xFFC4B5FD)
val PulseGradientMid      = Color(0xFF7C3AED)
val PulseGradientBottom   = Color(0xFF4C1D95)

// ---- Wallet Palettes ----
val WalletPalette = listOf(
    Color(0xFF1F2937) to Color(0xFF374151),
    Color(0xFF7C2D12) to Color(0xFFC2410C),
    Color(0xFF134E4A) to Color(0xFF0F766E),
    Color(0xFF4C1D95) to Color(0xFF7C3AED),
    Color(0xFF831843) to Color(0xFFBE185D),
    Color(0xFF1E3A8A) to Color(0xFF3B82F6)
)

// ---- Back-compat aliases ----
val LightBackground        = CreamBackground
val LightOnBackground      = CreamOnBackground
val LightSurface           = CreamSurface
val LightOnSurface         = CreamOnSurface
val LightSurfaceVariant    = CreamSurfaceTonal
val LightOnSurfaceVariant  = CreamOnSurfaceVariant
val LightOutline           = CreamOutline
val LightOutlineVariant    = CreamOutlineVariant
val LightScrim             = Scrim

val LightError             = ErrorLight
val LightOnError           = ErrorOnLight
val LightErrorContainer    = ErrorLightContainer
val LightOnErrorContainer  = Color(0xFF6B1414)

val DarkBackground         = CharcoalBackground
val DarkOnBackground       = CharcoalOnBackground
val DarkSurface            = CharcoalSurface
val DarkOnSurface          = CharcoalOnSurface
val DarkSurfaceVariant     = CharcoalSurfaceTonal
val DarkOnSurfaceVariant   = CharcoalOnSurfaceVariant
val DarkOutline            = CharcoalOutline
val DarkOutlineVariant     = CharcoalOutlineVariant
val DarkScrim              = Scrim

val DarkError              = ErrorDark
val DarkOnError            = ErrorOnDark
val DarkErrorContainer     = ErrorDarkContainer
val DarkOnErrorContainer   = Color(0xFFFDEDED)

val IndigoPrimary              = Color(0xFF7C3AED)
val IndigoOnPrimary            = Color(0xFFFFFFFF)
val IndigoPrimaryContainer     = Color(0xFFF3E8FF) // Softer purple container
val IndigoOnPrimaryContainer   = Color(0xFF2E1065)

val IndigoPrimaryDark          = Color(0xFFA78BFA)
val IndigoOnPrimaryDark        = Color(0xFF1E1B4B)
val IndigoPrimaryContainerDark = Color(0xFF4C1D95)
val IndigoOnPrimaryContainerDark = Color(0xFFEDE9FE)

val SlateSecondary             = TealLight
val SlateOnSecondary           = TealOnLight
val SlateSecondaryContainer    = TealContainerLight
val SlateOnSecondaryContainer  = TealOnContainerLight

val SlateSecondaryDark         = TealDark
val SlateOnSecondaryDark       = TealOnDark
val SlateSecondaryContainerDark= TealContainerDark
val SlateOnSecondaryContainerDark = TealOnContainerDark

val CyanTertiary               = GoldLight
val CyanOnTertiary             = Color(0xFFFFFFFF)
val CyanTertiaryContainer      = GoldContainerLight
val CyanOnTertiaryContainer    = Color(0xFF5A3F1A)

val CyanTertiaryDark           = GoldDark
val CyanOnTertiaryDark         = Color(0xFF3D2E15)
val CyanTertiaryContainerDark  = GoldContainerDark
val CyanOnTertiaryContainerDark= Color(0xFFF5E4C8)

val IncomeLightContainer_Alias  = IncomeLightContainer
val ExpenseLightContainer_Alias = ExpenseLightContainer

val HeroGradientTop    = PulseGradientTop
val HeroGradientMid    = PulseGradientMid
val HeroGradientBottom = PulseGradientBottom
val HeroSheen          = Color(0x33FFFFFF)
