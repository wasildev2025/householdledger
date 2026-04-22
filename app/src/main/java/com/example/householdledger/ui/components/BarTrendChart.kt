package com.example.householdledger.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * Minimal bar chart for daily expense trends. No axis labels — this is a sparkline-style
 * widget meant to live below a summary. Pair with a text legend if context is needed.
 */
@Composable
fun BarTrendChart(
    values: List<Double>,
    modifier: Modifier = Modifier,
    height: Dp = 120.dp,
    barColor: Color = MaterialTheme.colorScheme.primary,
    emptyBarColor: Color = MaterialTheme.colorScheme.outlineVariant,
    cornerRadiusPx: Float = 6f,
    highlightIndex: Int? = null,
    secondaryValues: List<Double>? = null,
    secondaryColor: Color = MaterialTheme.colorScheme.secondary,
    labels: List<String>? = null
) {
    val peak = max(
        max(values.maxOrNull() ?: 0.0, secondaryValues?.maxOrNull() ?: 0.0),
        1.0
    )
    val animProgress by animateFloatAsState(
        targetValue = if (values.isNotEmpty()) 1f else 0f,
        animationSpec = tween(durationMillis = 450),
        label = "bar-chart-grow"
    )
    Canvas(modifier = modifier.height(height)) {
        if (values.isEmpty()) return@Canvas
        val n = values.size
        val totalGap = (n - 1).coerceAtLeast(0) * 8f
        val groupWidth = (size.width - totalGap) / n
        val hasSecondary = secondaryValues != null && secondaryValues.size == n
        val barWidth = if (hasSecondary) groupWidth / 2f - 1f else groupWidth

        values.forEachIndexed { i, v ->
            val xGroupStart = i * (groupWidth + 8f)
            
            // Draw primary bar
            val rel = (v / peak).toFloat().coerceIn(0f, 1f) * animProgress
            val barHeight = size.height * rel
            val y = size.height - barHeight
            val color = when {
                v == 0.0 -> emptyBarColor
                highlightIndex == i -> barColor
                else -> barColor.copy(alpha = 0.75f)
            }
            
            drawRoundRect(
                color = color,
                topLeft = Offset(xGroupStart, y),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx, cornerRadiusPx)
            )

            // Draw secondary bar
            if (secondaryValues != null && secondaryValues.size == n) {
                val v2 = secondaryValues[i]
                val rel2 = (v2 / peak).toFloat().coerceIn(0f, 1f) * animProgress
                val barHeight2 = size.height * rel2
                val y2 = size.height - barHeight2
                drawRoundRect(
                    color = secondaryColor.copy(alpha = 0.75f),
                    topLeft = Offset(xGroupStart + barWidth + 2f, y2),
                    size = Size(barWidth, barHeight2),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx, cornerRadiusPx)
                )
            }
        }
    }
}
