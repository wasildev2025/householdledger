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
    highlightIndex: Int? = null
) {
    val peak = max(values.maxOrNull() ?: 0.0, 1.0)
    val animProgress by animateFloatAsState(
        targetValue = if (values.isNotEmpty()) 1f else 0f,
        animationSpec = tween(durationMillis = 450),
        label = "bar-chart-grow"
    )
    Canvas(modifier = modifier.height(height)) {
        if (values.isEmpty()) return@Canvas
        val n = values.size
        val totalGap = (n - 1).coerceAtLeast(0) * 3f
        val barWidth = (size.width - totalGap) / n
        values.forEachIndexed { i, v ->
            val rel = (v / peak).toFloat().coerceIn(0f, 1f) * animProgress
            val barHeight = size.height * rel
            val x = i * (barWidth + 3f)
            val y = size.height - barHeight
            val color = when {
                v == 0.0 -> emptyBarColor
                highlightIndex == i -> barColor
                else -> barColor.copy(alpha = 0.75f)
            }
            val trackColor = emptyBarColor.copy(alpha = 0.35f)
            // faint full-height track
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(x, 0f),
                size = Size(barWidth, size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx, cornerRadiusPx)
            )
            if (barHeight > 0f) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx, cornerRadiusPx)
                )
            }
        }
    }
}

