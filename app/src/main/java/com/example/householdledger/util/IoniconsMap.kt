package com.example.householdledger.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsBus
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.MovieCreation
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PhoneIphone
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Train
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Resolve a category icon name to a drawable vector.
 *
 * The legacy React app stored Ionicons name strings (e.g. `"cart-outline"`,
 * `"fast-food"`, `"home-sharp"`) in `categories.icon`. When rendered with Text
 * they showed as literal strings — this mapping turns them back into icons.
 *
 * Returns null for:
 *   - Blank strings
 *   - Emojis (single codepoint, non-ASCII) — caller should render as Text
 *   - Unmapped Ionicons — caller can fall back to a monogram
 */
fun resolveCategoryIcon(raw: String?): ImageVector? {
    if (raw.isNullOrBlank()) return null
    // Emojis / already-glyph strings: leave to the caller to render as Text.
    if (!raw.all { it.isLetterOrDigit() || it == '-' || it == '_' }) return null

    val key = raw
        .lowercase()
        .removeSuffix("-outline")
        .removeSuffix("-sharp")
        .removeSuffix("-filled")
        .replace('_', '-')

    return IconMap[key] ?: aliasMap(key)
}

private val IconMap: Map<String, ImageVector> = mapOf(
    // Shopping / commerce
    "cart" to Icons.Outlined.ShoppingCart,
    "basket" to Icons.Outlined.ShoppingCart,
    "bag" to Icons.Outlined.ShoppingCart,
    "bag-handle" to Icons.Outlined.ShoppingCart,
    "gift" to Icons.Outlined.CardGiftcard,
    // Food
    "fast-food" to Icons.Outlined.Restaurant,
    "restaurant" to Icons.Outlined.Restaurant,
    "pizza" to Icons.Outlined.Restaurant,
    "cafe" to Icons.Outlined.LocalCafe,
    "beer" to Icons.Outlined.Coffee,
    "wine" to Icons.Outlined.Coffee,
    // Home / utilities
    "home" to Icons.Outlined.Home,
    "bed" to Icons.Outlined.Home,
    "flash" to Icons.Outlined.Bolt,
    "bolt" to Icons.Outlined.Bolt,
    "water" to Icons.Outlined.WaterDrop,
    "flame" to Icons.Outlined.LocalFireDepartment,
    "wifi" to Icons.Outlined.Wifi,
    "phone-portrait" to Icons.Outlined.PhoneIphone,
    "call" to Icons.Outlined.PhoneIphone,
    // Transport
    "car" to Icons.Outlined.DirectionsCar,
    "car-sport" to Icons.Outlined.DirectionsCar,
    "bus" to Icons.AutoMirrored.Outlined.DirectionsBus,
    "train" to Icons.Outlined.Train,
    "airplane" to Icons.Outlined.Flight,
    // Health
    "medkit" to Icons.Outlined.LocalHospital,
    "medical" to Icons.Outlined.LocalHospital,
    "fitness" to Icons.Outlined.FitnessCenter,
    "barbell" to Icons.Outlined.FitnessCenter,
    "heart" to Icons.Outlined.LocalHospital,
    // Education
    "book" to Icons.AutoMirrored.Outlined.MenuBook,
    "school" to Icons.Outlined.School,
    "library" to Icons.AutoMirrored.Outlined.MenuBook,
    // Entertainment
    "musical-notes" to Icons.Outlined.MusicNote,
    "headset" to Icons.Outlined.Headphones,
    "film" to Icons.Outlined.MovieCreation,
    "videocam" to Icons.Outlined.MovieCreation,
    "game-controller" to Icons.Outlined.SportsEsports,
    // Personal
    "shirt" to Icons.Outlined.Checkroom,
    "brush" to Icons.Outlined.Brush,
    "paw" to Icons.Outlined.Pets,
    // Money
    "cash" to Icons.Outlined.AttachMoney,
    "wallet" to Icons.Outlined.AccountBalanceWallet,
    "card" to Icons.Outlined.AccountBalanceWallet,
    "receipt" to Icons.AutoMirrored.Outlined.ReceiptLong,
    "pricetag" to Icons.Outlined.Star,
    // Dairy (used in Household Ledger)
    "milk" to Icons.Outlined.LocalDrink,
    "water-drop" to Icons.Outlined.WaterDrop,
    "drink" to Icons.Outlined.LocalDrink
)

/** Last-chance fuzzy lookup for Ionicons we didn't enumerate. */
private fun aliasMap(key: String): ImageVector? = when {
    key.contains("food") -> Icons.Outlined.Restaurant
    key.contains("shop") -> Icons.Outlined.ShoppingCart
    key.contains("car") -> Icons.Outlined.DirectionsCar
    key.contains("home") -> Icons.Outlined.Home
    key.contains("gift") -> Icons.Outlined.CardGiftcard
    key.contains("bill") || key.contains("receipt") -> Icons.AutoMirrored.Outlined.ReceiptLong
    key.contains("health") || key.contains("med") -> Icons.Outlined.LocalHospital
    key.contains("school") || key.contains("edu") -> Icons.Outlined.School
    key.contains("music") || key.contains("audio") -> Icons.Outlined.MusicNote
    key.contains("dairy") || key.contains("milk") -> Icons.Outlined.LocalDrink
    key.contains("phone") -> Icons.Outlined.PhoneIphone
    else -> null
}
