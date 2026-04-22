package com.example.householdledger.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
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
    highlightIndex: Int? = null
) {
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(values) {
        animProgress.animateTo(1f, tween(800))
    }

    if (values.isEmpty()) return

    val maxVal = max(values.maxOrNull() ?: 0.0, 1.0).toFloat()
    
    Canvas(modifier = modifier
        .fillMaxWidth()
        .height(height)
    ) {
        val width = size.width
        val canvasHeight = size.height
        val spacing = width / (values.size - 1).coerceAtLeast(1)

        // Draw horizontal grid lines (subtle)
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

            // Fill path
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

            // Draw line
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw points
            if (showDots) {
                points.forEachIndexed { index, point ->
                    val isHighlighted = index == highlightIndex
                    val radius = if (isHighlighted) 6.dp.toPx() else 4.dp.toPx()
                    
                    // Outer dot glow
                    drawCircle(
                        color = lineColor.copy(alpha = 0.3f),
                        radius = radius + 2.dp.toPx(),
                        center = point
                    )
                    
                    // Main dot
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
