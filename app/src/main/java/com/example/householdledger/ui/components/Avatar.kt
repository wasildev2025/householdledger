package com.example.householdledger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/**
 * Deterministic circular avatar for a person. Derives a stable hue from their name
 * so the same person always looks the same color — predictability over randomness.
 */
@Composable
fun Avatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val initials = remember(name) { initialsFor(name) }
    val seed = remember(name) { name.trim().lowercase().hashCode() }
    val palette = avatarPalette()
    val bg = palette[abs(seed) % palette.size]
    val fg = if (bg.luminance() > 0.55f) Color.Black.copy(alpha = 0.85f) else Color.White

    Box(
        modifier = modifier
            .size(size)
            .background(bg, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = TextStyle(
                fontSize = (size.value * 0.38f).sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = fg
        )
    }
}

@Composable
private fun avatarPalette(): List<Color> = listOf(
    MaterialTheme.colorScheme.primary,
    MaterialTheme.colorScheme.secondary,
    MaterialTheme.colorScheme.tertiary,
    MaterialTheme.colorScheme.primaryContainer,
    MaterialTheme.colorScheme.secondaryContainer,
    MaterialTheme.colorScheme.tertiaryContainer
)

private fun initialsFor(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
    }
}

private fun Color.luminance(): Float {
    fun channel(c: Float) =
        if (c <= 0.03928f) c / 12.92f
        else Math.pow(((c + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
    return 0.2126f * channel(red) + 0.7152f * channel(green) + 0.0722f * channel(blue)
}

