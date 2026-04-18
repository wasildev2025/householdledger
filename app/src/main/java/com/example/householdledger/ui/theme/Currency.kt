package com.example.householdledger.ui.theme

fun currencyCodeToSymbol(code: String): String = when (code.uppercase()) {
    "PKR" -> "₨"
    "INR" -> "₹"
    "USD" -> "$"
    "EUR" -> "€"
    "GBP" -> "£"
    "AED" -> "د.إ"
    "SAR" -> "﷼"
    "BDT" -> "৳"
    else -> code + " "
}
