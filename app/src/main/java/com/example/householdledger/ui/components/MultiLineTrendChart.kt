package com.example.householdledger.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max

/**
 * A multi-line trend chart for comparing Income, Expense, and Transfers.
 * Tap the plot area to inspect a point on mobile.
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
    transferColor: Color = Color.Gray,
    startLabel: String? = null,
    midLabel: String? = null,
    endLabel: String? = null,
    detailLabels: List<String> = emptyList()
) {
    val animProgress = remember { Animatable(0f) }
    var selectedIndex by remember(incomeValues, expenseValues, transferValues) { mutableIntStateOf(-1) }
    var canvasSize by remember { androidx.compose.runtime.mutableStateOf(IntSize.Zero) }

    LaunchedEffect(incomeValues, expenseValues, transferValues) {
        selectedIndex = -1
        animProgress.animateTo(1f, tween(1000))
    }

    if (incomeValues.isEmpty() && expenseValues.isEmpty() && transferValues.isEmpty()) return

    val pointCount = max(max(incomeValues.size, expenseValues.size), transferValues.size)
    val maxVal = max(
        max(incomeValues.maxOrNull() ?: 0.0, expenseValues.maxOrNull() ?: 0.0),
        max(transferValues.maxOrNull() ?: 0.0, 1.0)
    ).toFloat()
    val selectedGuideColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)

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
                .height(height)
                .padding(top = 8.dp)
                .onSizeChanged { canvasSize = it }
                .pointerInput(pointCount, canvasSize) {
                    detectTapGestures { tap ->
                        if (pointCount == 0 || canvasSize.width == 0) return@detectTapGestures
                        val spacing = if (pointCount <= 1) 0f else canvasSize.width.toFloat() / (pointCount - 1)
                        val pointsX = (0 until pointCount).map { index ->
                            if (pointCount == 1) canvasSize.width / 2f else index * spacing
                        }
                        selectedIndex = pointsX.indices.minByOrNull { index -> abs(pointsX[index] - tap.x) } ?: -1
                    }
                }
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
            ) {
                val width = size.width
                val canvasHeight = size.height
                val spacing = width / (pointCount - 1).coerceAtLeast(1)

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

                fun pointsFor(values: List<Double>): List<Offset> = values.mapIndexed { index, value ->
                    val x = if (values.size == 1) width / 2f else index * spacing
                    val y = canvasHeight - (value.toFloat() / maxVal * canvasHeight * animProgress.value)
                    Offset(x, y)
                }

                fun drawTrendLine(values: List<Double>, color: Color) {
                    if (values.isEmpty()) return
                    val points = pointsFor(values)

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

                        drawPath(
                            path = path,
                            color = color,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    points.forEachIndexed { index, point ->
                        val selected = index == selectedIndex
                        val outerRadius = if (points.size == 1 || selected) 7.dp.toPx() else 4.dp.toPx()
                        val innerRadius = if (points.size == 1 || selected) 4.5.dp.toPx() else 2.5.dp.toPx()
                        drawCircle(color = Color.White, radius = outerRadius, center = point)
                        drawCircle(color = color, radius = innerRadius, center = point)
                    }
                }

                if (selectedIndex in 0 until pointCount) {
                    val selectedX = if (pointCount == 1) width / 2f else selectedIndex * spacing
                    drawLine(
                        color = selectedGuideColor,
                        start = Offset(selectedX, 0f),
                        end = Offset(selectedX, canvasHeight),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                drawTrendLine(transferValues, transferColor)
                drawTrendLine(expenseValues, expenseColor)
                drawTrendLine(incomeValues, incomeColor)
            }

            if (selectedIndex in 0 until pointCount) {
                TrendTooltip(
                    label = detailLabels.getOrNull(selectedIndex)
                        ?: startLabel
                        ?: "Selected",
                    income = incomeValues.getOrElse(selectedIndex) { 0.0 },
                    expense = expenseValues.getOrElse(selectedIndex) { 0.0 },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp, start = 12.dp, end = 12.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AxisLabel(startLabel ?: "Start")
            AxisLabel(midLabel ?: "Mid")
            AxisLabel(endLabel ?: "End")
        }
    }
}

@Composable
private fun TrendTooltip(
    label: String,
    income: Double,
    expense: Double,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        shadowElevation = 6.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            TooltipValue("Income", income, MoneyTone.Income)
            TooltipValue("Expense", expense, MoneyTone.Expense)
        }
    }
}

@Composable
private fun TooltipValue(label: String, amount: Double, tone: MoneyTone) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        MoneyText(
            amount = amount,
            tone = tone,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun AxisLabel(text: String) {
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
