package com.example.householdledger.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Current display currency symbol provided via CompositionLocal so MoneyText
 * and any money display can pick up user preference without explicit plumbing.
 */
val LocalCurrencySymbol = staticCompositionLocalOf { "₹" }
