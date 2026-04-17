package com.example.householdledger.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A horizontal progress-style bar representing a category's share of spend.
 * Uses the category color if provided, otherwise primary.
 */
@Composable
fun CategoryBar(
    label: String,
    icon: String,
    share: Float,
    amount: Double,
    colorHex: String?,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(
        targetValue = share.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "category-share"
    )
    val barColor = parseHex(colorHex) ?: MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryPill(icon = icon, colorHex = colorHex, size = 32.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            MoneyText(
                amount = amount,
                style = com.example.householdledger.ui.theme.MoneyBody,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(3.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(color = barColor, shape = RoundedCornerShape(3.dp))
            )
        }
    }
}

private fun parseHex(hex: String?): Color? {
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
