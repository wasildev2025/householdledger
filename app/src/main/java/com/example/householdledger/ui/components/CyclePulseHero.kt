package com.example.householdledger.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.householdledger.ui.theme.EyebrowCaps
import com.example.householdledger.ui.theme.MoneyHero
import com.example.householdledger.ui.theme.PillShape
import com.example.householdledger.ui.theme.PulseGradientBottom
import com.example.householdledger.ui.theme.PulseGradientMid
import com.example.householdledger.ui.theme.PulseGradientTop
import kotlin.math.cos
import kotlin.math.sin

/**
 * Signature home hero. Replaces the generic "net balance" card with a radial
 * progress ring that shows:
 *   - Actual spend as a filled saffron arc (0° = top)
 *   - Projected spend as a translucent ghost arc extending from it
 *   - A pin at the current day-of-cycle position on the track
 *   - Centered hero number = projected end-of-cycle total
 *   - A sub-line predicting over/under budget status
 *
 * Designed for at-a-glance answer to: *"Am I going to blow my budget?"*
 */
@Composable
fun CyclePulseHero(
    cycleLabel: String,
    daysLeft: Int,
    dayIndex: Int,                  // 0..lengthDays-1
    cycleLengthDays: Int,
    expenseSoFar: Double,
    budgetCap: Double,              // 0 if not set → ring shows spend-vs-income instead
    projectedExpense: Double,
    projectedOverrunPercent: Float, // signed — positive = over, negative = under
    income: Double,
    transfers: Double = 0.0,
    modifier: Modifier = Modifier
) {
    val spendRatio = if (budgetCap > 0) (expenseSoFar / budgetCap).toFloat().coerceIn(0f, 1f) else 0f
    val projectedRatio = if (budgetCap > 0) (projectedExpense / budgetCap).toFloat().coerceIn(0f, 1.4f) else 0f
    val dayRatio = if (cycleLengthDays > 0) (dayIndex.toFloat() / cycleLengthDays).coerceIn(0f, 1f) else 0f
    val heroShape = RoundedCornerShape(28.dp)
    val heroText = Color(0xFFFFFBF5)

    val ringAnim = remember { Animatable(0f) }
    val projectedAnim = remember { Animatable(0f) }
    LaunchedEffect(spendRatio, projectedRatio) {
        ringAnim.animateTo(spendRatio, tween(520))
    }
    LaunchedEffect(projectedRatio) {
        projectedAnim.animateTo(projectedRatio.coerceAtMost(1f), tween(700))
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = heroShape,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(heroShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(PulseGradientBottom, PulseGradientMid, PulseGradientTop)
                    ),
                    shape = heroShape
                )
                .border(1.dp, heroText.copy(alpha = 0.10f), heroShape)
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Eyebrow(text = "CYCLE PULSE", color = heroText.copy(alpha = 0.8f))
                    DaysLeftPill(daysLeft)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    cycleLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = heroText.copy(alpha = 0.74f)
                )

                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        PulseRing(
                            progress = ringAnim.value,
                            projectedProgress = projectedAnim.value,
                            dayMarkerRatio = dayRatio,
                            modifier = Modifier.size(156.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Projected total",
                            style = MaterialTheme.typography.labelSmall,
                            color = heroText.copy(alpha = 0.76f)
                        )
                        MoneyText(
                            amount = projectedExpense.takeIf { it > 0 } ?: expenseSoFar,
                            style = MoneyHero.copy(fontSize = androidx.compose.ui.unit.TextUnit.Unspecified),
                            color = heroText,
                            currencySymbol = "Rs"
                        )
                        Text(
                            text = "At current pace",
                            style = MaterialTheme.typography.bodySmall,
                            color = heroText.copy(alpha = 0.66f)
                        )
                        PaceVerdict(projectedOverrunPercent, budgetCap)
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = heroText.copy(alpha = 0.14f))
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MiniStat(label = "Spent", amount = expenseSoFar, modifier = Modifier.weight(1f))
                    MiniStat(label = "Income", amount = income, modifier = Modifier.weight(1f))
                    MiniStat(label = "Transfers", amount = transfers, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun Eyebrow(text: String, color: Color) {
    Text(
        text = text,
        style = EyebrowCaps,
        color = color
    )
}

@Composable
private fun DaysLeftPill(daysLeft: Int) {
    val text = when {
        daysLeft <= 0 -> "Cycle resets today"
        daysLeft == 1 -> "1 day left"
        else -> "$daysLeft days left"
    }
    Surface(
        shape = PillShape,
        color = Color.White.copy(alpha = 0.14f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun PulseRing(
    progress: Float,           // 0..1 — solid saffron-on-white ring
    projectedProgress: Float,  // 0..1 — translucent extension ("ghost")
    dayMarkerRatio: Float,     // 0..1 — where today is on the full track
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val stroke = 14.dp.toPx()
        val inset = stroke / 2f
        val rectSize = Size(size.width - stroke, size.height - stroke)
        val topLeft = Offset(inset, inset)

        // Track
        drawArc(
            color = Color.White.copy(alpha = 0.14f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = rectSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )

        // Projected arc (ghost) — drawn first so the solid sits above it
        if (projectedProgress > 0f && projectedProgress > progress) {
            drawArc(
                color = Color.White.copy(alpha = 0.32f),
                startAngle = -90f,
                sweepAngle = 360f * projectedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = rectSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }

        // Actual spend arc
        if (progress > 0f) {
            drawArc(
                color = Color.White,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = topLeft,
                size = rectSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }

        // Day-of-cycle pin (a small dot on the track where "today" sits)
        val angleRad = Math.toRadians((dayMarkerRatio * 360.0) - 90.0)
        val radius = (size.minDimension / 2f) - inset
        val center = Offset(size.width / 2f, size.height / 2f)
        val pin = Offset(
            center.x + (radius * cos(angleRad)).toFloat(),
            center.y + (radius * sin(angleRad)).toFloat()
        )
        drawCircle(color = Color.White, radius = 7.dp.toPx(), center = pin)
        drawCircle(color = PulseGradientBottom, radius = 3.5.dp.toPx(), center = pin)
    }
}

@Composable
private fun PaceVerdict(overrunPercent: Float, budgetCap: Double) {
    if (budgetCap <= 0) {
        Text(
            text = "Set a monthly budget in Settings to unlock pace tracking.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.82f)
        )
        return
    }
    val (label, tone) = when {
        overrunPercent > 0.05f  -> "Trending ${(overrunPercent * 100).toInt()}% over budget" to PaceTone.Over
        overrunPercent < -0.05f -> "Trending ${(-overrunPercent * 100).toInt()}% under budget" to PaceTone.Under
        else                    -> "On pace with your budget" to PaceTone.OnTrack
    }
    Surface(
        shape = PillShape,
        color = Color.White.copy(alpha = 0.14f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(
                        color = when (tone) {
                            PaceTone.OnTrack -> Color(0xFF7CE4B4)
                            PaceTone.Under   -> Color(0xFF7CE4B4)
                            PaceTone.Over    -> Color(0xFFFFB4A0)
                        },
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
        }
    }
}

private enum class PaceTone { OnTrack, Under, Over }

@Composable
private fun MiniStat(label: String, amount: Double, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.72f),
            maxLines = 1
        )
        Spacer(Modifier.height(4.dp))
        // Titlesmall keeps three money columns readable on narrow phones where
        // titleMedium used to overflow and clip the transfers value.
        MoneyText(
            amount = amount,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            ),
            color = Color.White,
            currencySymbol = "Rs",
            modifier = Modifier.then(Modifier)
        )
    }
}
