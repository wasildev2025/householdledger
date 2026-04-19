package com.example.householdledger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.householdledger.util.resolveCategoryIcon

/**
 * Renders a colored circular badge with an emoji/letter for a category.
 * The `colorHex` can be a "#RRGGBB" or null (falls back to primaryContainer).
 * The `icon` is a short string — typically an emoji like "🍔" or first letter.
 */
@Composable
fun CategoryPill(
    icon: String,
    modifier: Modifier = Modifier,
    colorHex: String? = null,
    size: Dp = 44.dp
) {
    val parsed = parseHexColor(colorHex)
    val background = parsed ?: MaterialTheme.colorScheme.primaryContainer
    val onBackground = if (parsed != null) {
        if (parsed.luminance() > 0.55f) Color.Black.copy(alpha = 0.85f) else Color.White
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }

    val vector = resolveCategoryIcon(icon)
    Box(
        modifier = modifier
            .size(size)
            .background(background, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (vector != null) {
            Icon(
                imageVector = vector,
                contentDescription = null,
                tint = onBackground,
                modifier = Modifier.size(size * 0.52f)
            )
        } else {
            Text(
                text = icon.ifBlank { "•" },
                style = TextStyle(fontSize = (size.value * 0.45f).sp, fontWeight = FontWeight.SemiBold),
                color = onBackground
            )
        }
    }
}

private fun parseHexColor(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    return try {
        val clean = hex.removePrefix("#")
        val value = when (clean.length) {
            6 -> 0xFF000000 or clean.toLong(16)
            8 -> clean.toLong(16)
            else -> return null
        }
        Color(value)
    } catch (_: Exception) {
        null
    }
}

private fun Color.luminance(): Float {
    fun channel(c: Float) = if (c <= 0.03928f) c / 12.92f else Math.pow(((c + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
    return 0.2126f * channel(red) + 0.7152f * channel(green) + 0.0722f * channel(blue)
}
