package com.example.householdledger.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.householdledger.ui.theme.EyebrowCaps
import com.example.householdledger.ui.theme.PulseGradientBottom
import com.example.householdledger.ui.theme.PulseGradientMid
import com.example.householdledger.ui.theme.PulseGradientTop

/**
 * Branded splash. A deep purple gradient card lifts up from below as the
 * logo scales in. A soft pulse ring breathes behind the mark while boot
 * runs, echoing the Cycle Pulse hero so the first frame the user sees is
 * already in the app's visual voice.
 */
@Composable
fun SplashScreen() {
    // Logo entrance — scale + lift.
    val scale = remember { Animatable(0.72f) }
    val lift = remember { Animatable(16f) }
    val copyAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(1f, tween(durationMillis = 520, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        lift.animateTo(0f, tween(durationMillis = 520, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        copyAlpha.animateTo(1f, tween(durationMillis = 500, delayMillis = 220))
    }

    // Continuous breathing ring.
    val breathing = rememberInfiniteTransition(label = "breathing")
    val breathPhase by breathing.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "breath"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // Subtle corner glow — gives the otherwise-flat cream background depth.
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PulseGradientTop.copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.2f, size.height * 0.25f),
                    radius = size.minDimension * 0.9f
                )
            )
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BrandMark(
                scale = scale.value,
                liftDp = lift.value,
                breathPhase = breathPhase
            )

            Spacer(Modifier.height(28.dp))

            Text(
                "Household Ledger",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = androidx.compose.ui.unit.TextUnit(
                        -0.4f,
                        androidx.compose.ui.unit.TextUnitType.Sp
                    )
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.alpha(copyAlpha.value)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Money for your whole home",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alpha(copyAlpha.value)
            )
            Spacer(Modifier.height(32.dp))

            LoadingDots(alpha = copyAlpha.value)
        }

        // Subtle version line at the bottom — only when copy has faded in.
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
                .alpha(copyAlpha.value * 0.6f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "PURPLE · MODERN · INTENTIONAL",
                style = EyebrowCaps,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BrandMark(scale: Float, liftDp: Float, breathPhase: Float) {
    val wave1 = (breathPhase * 2f).coerceAtMost(1f)
    val wave2 = ((breathPhase - 0.33f + 1f) % 1f * 2f).coerceAtMost(1f)

    Box(
        modifier = Modifier
            .size(140.dp)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        // Breathing rings (outer halos).
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = PulseGradientMid.copy(alpha = 0.22f * (1f - wave1)),
                radius = (size.minDimension / 2f) * (0.55f + wave1 * 0.45f)
            )
            drawCircle(
                color = PulseGradientTop.copy(alpha = 0.14f * (1f - wave2)),
                radius = (size.minDimension / 2f) * (0.55f + wave2 * 0.45f)
            )
        }

        // The saffron medallion. Uses the Pulse gradient so the splash and the
        // home hero share the same visual DNA.
        Box(
            modifier = Modifier
                .size(104.dp)
                .offset(y = liftDp.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(30.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    PulseGradientTop,
                                    PulseGradientMid,
                                    PulseGradientBottom
                                )
                            ),
                            shape = RoundedCornerShape(30.dp)
                        )
                ) {
                    // Hand-drawn "H" + radial tick, matches the Cycle Pulse arc.
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val stroke = 8f

                        // Outer arc — recalls the Pulse ring
                        drawArc(
                            color = Color.White.copy(alpha = 0.42f),
                            startAngle = -100f,
                            sweepAngle = 280f,
                            useCenter = false,
                            topLeft = Offset(
                                cx - (size.minDimension / 2f) + 10f,
                                cy - (size.minDimension / 2f) + 10f
                            ),
                            size = androidx.compose.ui.geometry.Size(
                                size.minDimension - 20f,
                                size.minDimension - 20f
                            ),
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )

                        // "H" monogram. Two vertical posts + crossbar.
                        val postHeight = size.minDimension * 0.34f
                        val postWidth = 6f
                        val gap = size.minDimension * 0.16f
                        val postY1 = cy - postHeight / 2f
                        val postY2 = cy + postHeight / 2f

                        drawLine(
                            color = Color.White,
                            start = Offset(cx - gap, postY1),
                            end = Offset(cx - gap, postY2),
                            strokeWidth = postWidth,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = Color.White,
                            start = Offset(cx + gap, postY1),
                            end = Offset(cx + gap, postY2),
                            strokeWidth = postWidth,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = Color.White,
                            start = Offset(cx - gap, cy),
                            end = Offset(cx + gap, cy),
                            strokeWidth = postWidth,
                            cap = StrokeCap.Round
                        )

                        // "today" pin on the arc — a nod to the Cycle Pulse pointer.
                        val pinAngle = Math.toRadians(-100.0 + 280.0 * 0.72)
                        val radius = (size.minDimension / 2f) - 10f
                        val pin = Offset(
                            cx + (radius * kotlin.math.cos(pinAngle)).toFloat(),
                            cy + (radius * kotlin.math.sin(pinAngle)).toFloat()
                        )
                        drawCircle(color = Color.White, radius = 6f, center = pin)
                        drawCircle(color = PulseGradientBottom, radius = 3f, center = pin)
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingDots(alpha: Float) {
    val t = rememberInfiniteTransition(label = "dots")
    val phase by t.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dotPhase"
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.alpha(alpha)
    ) {
        repeat(3) { i ->
            val active = phase.toInt() == i
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .scale(if (active) 1.2f else 0.85f)
                    .background(
                        color = if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape
                    )
            )
        }
    }
}

