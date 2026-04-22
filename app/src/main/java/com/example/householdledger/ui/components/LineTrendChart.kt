package com.example.householdledger.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * A smooth line chart inspired by modern activity dashboards.
 * Shows daily spend trends with a Bezier curve and markers.
 */
@Composable
fun LineTrendChart(
    values: List<Double>,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    fillColor: Color = lineColor.copy(alpha = 0.15f),
    showDots: Boolean = true,
    highlightIndex: Int? = null,
    startLabel: String? = null,
    midLabel: String? = null,
    endLabel: String? = null
) {
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(values) {
        animProgress.animateTo(1f, tween(800))
    }

    if (values.isEmpty()) return

    val maxVal = max(values.maxOrNull() ?: 0.0, 1.0).toFloat()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = compactMoney(maxVal.toDouble()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = compactMoney((maxVal / 2f).toDouble()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
            )
            Text(
                text = "0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
            ) {
                val width = size.width
                val canvasHeight = size.height
                val spacing = width / (values.size - 1).coerceAtLeast(1)

                val gridLines = 4
                for (i in 0..gridLines) {
                    val y = canvasHeight - (i * (canvasHeight / gridLines))
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.1f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                val points = values.mapIndexed { index, value ->
                    val x = index * spacing
                    val y = canvasHeight - (value.toFloat() / maxVal * canvasHeight * animProgress.value)
                    Offset(x, y)
                }

                if (points.size > 1) {
                    val path = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            val p0 = points[i - 1]
                            val p1 = points[i]
                            val cp1 = Offset(p0.x + (p1.x - p0.x) / 2f, p0.y)
                            val cp2 = Offset(p0.x + (p1.x - p0.x) / 2f, p1.y)
                            cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, p1.x, p1.y)
                        }
                    }

                    val fillPath = Path().apply {
                        addPath(path)
                        lineTo(points.last().x, canvasHeight)
                        lineTo(points.first().x, canvasHeight)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(fillColor, Color.Transparent),
                            startY = points.minOf { it.y },
                            endY = canvasHeight
                        )
                    )

                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    if (showDots) {
                        points.forEachIndexed { index, point ->
                            val isHighlighted = index == highlightIndex
                            val radius = if (isHighlighted) 6.dp.toPx() else 4.dp.toPx()

                            drawCircle(
                                color = lineColor.copy(alpha = 0.3f),
                                radius = radius + 2.dp.toPx(),
                                center = point
                            )
                            drawCircle(
                                color = Color.White,
                                radius = radius,
                                center = point
                            )
                            drawCircle(
                                color = lineColor,
                                radius = radius - 1.5.dp.toPx(),
                                center = point
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ChartAxisLabel(startLabel ?: "Start")
            ChartAxisLabel(midLabel ?: "Mid")
            ChartAxisLabel(endLabel ?: "End")
        }
    }
}

@Composable
private fun ChartAxisLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1
    )
}

private fun compactMoney(value: Double): String = when {
    value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000)
    value >= 1_000 -> String.format("%.0fK", value / 1_000)
    else -> value.toInt().toString()
}
