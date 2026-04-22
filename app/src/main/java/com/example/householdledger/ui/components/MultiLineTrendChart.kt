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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * A multi-line trend chart for comparing Income, Expense, and Transfers.
 */
@Composable
fun MultiLineTrendChart(
    incomeValues: List<Double>,
    expenseValues: List<Double>,
    transferValues: List<Double>,
    modifier: Modifier = Modifier,
    height: Dp = 200.dp,
    incomeColor: Color = com.example.householdledger.ui.theme.IncomeLight,
    expenseColor: Color = com.example.householdledger.ui.theme.ExpenseLight,
    transferColor: Color = Color.Gray
) {
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(incomeValues, expenseValues, transferValues) {
        animProgress.animateTo(1f, tween(1000))
    }

    if (incomeValues.isEmpty() && expenseValues.isEmpty() && transferValues.isEmpty()) return

    val maxVal = max(
        max(incomeValues.maxOrNull() ?: 0.0, expenseValues.maxOrNull() ?: 0.0),
        max(transferValues.maxOrNull() ?: 0.0, 1.0)
    ).toFloat()

    Canvas(modifier = modifier
        .fillMaxWidth()
        .height(height)
    ) {
        val width = size.width
        val canvasHeight = size.height
        val pointCount = max(max(incomeValues.size, expenseValues.size), transferValues.size)
        val spacing = width / (pointCount - 1).coerceAtLeast(1)

        // Draw grid
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

        fun drawTrendLine(values: List<Double>, color: Color) {
            if (values.size < 2) return
            val points = values.mapIndexed { index, value ->
                val x = index * spacing
                val y = canvasHeight - (value.toFloat() / maxVal * canvasHeight * animProgress.value)
                Offset(x, y)
            }

            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    val p0 = points[i - 1]
                    val p1 = points[i]
                    // Smooth curve
                    val cp1 = Offset(p0.x + (p1.x - p0.x) / 2f, p0.y)
                    val cp2 = Offset(p0.x + (p1.x - p0.x) / 2f, p1.y)
                    cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, p1.x, p1.y)
                }
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
            
            // Markers
            points.forEach { point ->
                drawCircle(color = Color.White, radius = 4.dp.toPx(), center = point)
                drawCircle(color = color, radius = 2.5.dp.toPx(), center = point)
            }
        }

        drawTrendLine(transferValues, transferColor)
        drawTrendLine(expenseValues, expenseColor)
        drawTrendLine(incomeValues, incomeColor)
    }
}
