package com.example.householdledger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.householdledger.util.resolveCategoryIcon

/**
 * Brand-style category logo. Matches common merchant names to a known brand
 * color + first-letter monogram. Unknown names fall back to the category color
 * (if any) and the provided icon, or the first letter of the name.
 *
 * Priority:
 * 1. Explicit iconName (user-selected icon) + colorHex
 * 2. Brand matching (e.g. "Netflix") + Brand color
 * 3. First letter monogram + colorHex
 */
@Composable
fun CategoryLogo(
    name: String,
    colorHex: String?,
    fallbackIcon: ImageVector? = null,
    iconName: String? = null,
    size: Dp = 40.dp
) {
    val categoryIcon = iconName?.let(::resolveCategoryIcon)
    val brand = if (categoryIcon == null) brandFor(name) else null

    val color = (if (categoryIcon != null) parseHex(colorHex) else null)
        ?: brand?.color
        ?: parseHex(colorHex)
        ?: MaterialTheme.colorScheme.primary
    
    val letter = brand?.letter ?: name.firstOrNull()?.uppercase() ?: "•"

    Box(
        modifier = Modifier.size(size)
            .background(color, RoundedCornerShape(size / 3)),
        contentAlignment = Alignment.Center
    ) {
        when {
            categoryIcon != null -> Icon(
                imageVector = categoryIcon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(size * 0.55f)
            )
            brand != null -> Text(
                letter,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            fallbackIcon != null && name.isBlank() -> Icon(
                fallbackIcon,
                null,
                tint = Color.White,
                modifier = Modifier.size(size * 0.55f)
            )
            else -> Text(
                letter,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }
    }
}

private data class Brand(val letter: String, val color: Color)

private val BrandMap: List<Pair<Regex, Brand>> = listOf(
    Regex("(?i)youtube|yt premium") to Brand("Y", Color(0xFFFF0000)),
    Regex("(?i)netflix") to Brand("N", Color(0xFFE50914)),
    Regex("(?i)spotify") to Brand("S", Color(0xFF1DB954)),
    Regex("(?i)apple|icloud|app ?store") to Brand("", Color(0xFF000000)),
    Regex("(?i)google|g ?pay|play ?store") to Brand("G", Color(0xFF4285F4)),
    Regex("(?i)amazon|prime") to Brand("a", Color(0xFFFF9900)),
    Regex("(?i)uber|careem") to Brand("U", Color(0xFF000000)),
    Regex("(?i)whatsapp|meta|facebook") to Brand("f", Color(0xFF1877F2)),
    Regex("(?i)microsoft|office|xbox") to Brand("M", Color(0xFF0078D4)),
    Regex("(?i)food|restaurant|dining|eat") to Brand("F", Color(0xFFEF4444)),
    Regex("(?i)grocer|market|mart") to Brand("G", Color(0xFF10B981)),
    Regex("(?i)transport|fuel|petrol|car") to Brand("T", Color(0xFF3B82F6)),
    Regex("(?i)health|medic|hospital|pharma") to Brand("H", Color(0xFFEC4899)),
    Regex("(?i)bill|electric|gas|water|utility") to Brand("B", Color(0xFFF59E0B)),
    Regex("(?i)school|education|course|tuition") to Brand("E", Color(0xFF8B5CF6)),
    Regex("(?i)rent|home|house|mortgage") to Brand("H", Color(0xFF6366F1)),
    Regex("(?i)dairy|milk|yogurt") to Brand("D", Color(0xFF0EA5E9)),
    Regex("(?i)salary|income|payroll") to Brand("₹", Color(0xFF059669))
)

private fun brandFor(name: String): Brand? {
    if (name.isBlank()) return null
    val match = BrandMap.firstOrNull { (r, _) -> r.containsMatchIn(name) }?.second ?: return null
    // Keep empty letter (for Apple) — we'll render an Apple glyph from first char.
    return if (match.letter.isBlank()) match.copy(letter = "") else match
}

private fun parseHex(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    return runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull()
}
