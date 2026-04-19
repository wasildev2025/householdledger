package com.example.householdledger.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.householdledger.ui.theme.EyebrowCaps
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * A tactile drag-to-fill vessel used for logging dairy quantities.
 *
 * Interaction:
 *  - Drag vertically to set the fill level (up = more, down = less).
 *  - Tap anywhere inside the vessel to jump the level to that position.
 *  - Values snap to `step` increments; haptics fire on each tick crossing.
 *  - A subtle sine-wave animates across the liquid surface so the container
 *    feels alive, not like a progress bar.
 */
@Composable
fun LiquidFill(
    label: String,
    unit: String,
    value: Double,
    max: Double,
    step: Double,
    liquidColor: Color,
    onChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 220.dp
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val animated = remember { Animatable(value.toFloat()) }
    // Track last ticked step count so haptics fire once per snap crossing.
    var lastTick by remember { mutableStateOf(stepsFor(value, step)) }

    LaunchedEffect(value) {
        animated.animateTo(value.toFloat(), tween(durationMillis = 350))
    }

    val waveTransition = rememberInfiniteTransition(label = "liquidWave")
    val wavePhase by waveTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    fun applyLevel(yInPx: Float, totalPx: Float) {
        val ratio = ((totalPx - yInPx) / totalPx).coerceIn(0f, 1f)
        val raw = ratio.toDouble() * max
        val snapped = (raw / step).roundToInt() * step
        val clamped = snapped.coerceIn(0.0, max)
        val ticks = stepsFor(clamped, step)
        if (ticks != lastTick) {
            lastTick = ticks
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        scope.launch { animated.animateTo(clamped.toFloat(), tween(120)) }
        onChange(clamped)
    }

    Column(modifier = modifier) {
        Text(
            label.uppercase(),
            style = EyebrowCaps,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = formatValue(animated.value.toDouble()),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                " $unit",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .pointerInput(max, step) {
                    detectTapGestures { offset ->
                        applyLevel(offset.y, size.height.toFloat())
                    }
                }
                .pointerInput("drag", max, step) {
                    detectDragGestures { change, _ ->
                        applyLevel(change.position.y, size.height.toFloat())
                    }
                }
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) {
                val ratio = (animated.value.toDouble() / max).toFloat().coerceIn(0f, 1f)
                val liquidTop = size.height * (1f - ratio)

                // Wavy liquid surface — amplitude tapers when near empty so it doesn't clip outside.
                val amp = if (ratio < 0.03f) 0f else 6f
                val freq = (2 * PI / size.width).toFloat()
                val wavePath = Path().apply {
                    moveTo(0f, liquidTop)
                    var x = 0f
                    while (x <= size.width) {
                        val y = liquidTop + amp * sin(freq * x + wavePhase).toFloat()
                        lineTo(x, y)
                        x += 4f
                    }
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(
                    path = wavePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            liquidColor.copy(alpha = 0.92f),
                            liquidColor
                        ),
                        startY = liquidTop,
                        endY = size.height
                    )
                )

                // Tick marks on the right at 25/50/75% intervals + top marker
                listOf(0.25f, 0.5f, 0.75f).forEach { t ->
                    val y = size.height * (1f - t)
                    drawLine(
                        color = Color.White.copy(alpha = 0.6f),
                        start = Offset(size.width - 20f, y),
                        end = Offset(size.width - 8f, y),
                        strokeWidth = 2f
                    )
                }
                drawLine(
                    color = Color.White.copy(alpha = 0.85f),
                    start = Offset(size.width - 26f, 3f),
                    end = Offset(size.width - 8f, 3f),
                    strokeWidth = 2.5f
                )

                // Subtle inner stroke for container tactility
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.05f),
                    topLeft = Offset(0f, 0f),
                    size = size,
                    cornerRadius = CornerRadius(28.dp.toPx(), 28.dp.toPx()),
                    style = Stroke(width = 1.5f)
                )
            }

            // "Drag to fill" affordance pill — hidden when empty is fine; keep always for discoverability
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Color.White.copy(alpha = 0.7f),
                            RoundedCornerShape(999.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "Drag to fill",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Black.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }
}

private fun stepsFor(value: Double, step: Double): Int =
    (abs(value) / step).roundToInt()

private fun formatValue(v: Double): String =
    if (v % 1.0 == 0.0) "%.0f".format(v) else "%.2f".format(v)
