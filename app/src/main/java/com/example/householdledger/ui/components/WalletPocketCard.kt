package com.example.householdledger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.householdledger.ui.theme.EyebrowCaps
import com.example.householdledger.ui.theme.MoneyBody
import com.example.householdledger.ui.theme.PillShape
import com.example.householdledger.ui.theme.WalletPalette
import kotlin.math.abs

/**
 * Apple-Pay-style "pocket" card used for Servant / Member wallets on Home.
 * Duotone gradient assigned deterministically per identity, subtle chip-style
 * glyph, budget meter bottom-right. Meant to be placed in a LazyRow with
 * generous horizontal spacing (12–14dp).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletPocketCard(
    name: String,
    kindLabel: String,     // "Servant" | "Member"
    monthSpend: Double,
    allocation: Double,    // 0 if none
    identityKey: String,   // stable id for palette hash
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val (top, bottom) = paletteFor(identityKey)
    val ratio = if (allocation > 0) (monthSpend / allocation).toFloat().coerceIn(0f, 1.2f) else 0f
    val overBudget = ratio > 1f

    Surface(
        modifier = modifier
            .width(240.dp)
            .height(140.dp),
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent,
        onClick = onClick ?: {},
        enabled = onClick != null
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(colors = listOf(top, bottom)),
                    shape = RoundedCornerShape(22.dp)
                )
        ) {
            // Subtle radial "sheen" in upper right for depth
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            center = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, 0f),
                            radius = 450f
                        ),
                        shape = RoundedCornerShape(22.dp)
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            kindLabel.uppercase(),
                            style = EyebrowCaps,
                            color = Color.White.copy(alpha = 0.72f)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            name,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                    IdentityGlyph(name = name)
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                "SPEND THIS CYCLE",
                                style = EyebrowCaps,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Spacer(Modifier.height(2.dp))
                            MoneyText(
                                amount = monthSpend,
                                style = MoneyBody.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                        if (allocation > 0) {
                            BudgetMeter(ratio = ratio, overBudget = overBudget)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IdentityGlyph(name: String) {
    val letter = name.firstOrNull()?.uppercase() ?: "?"
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(Color.White.copy(alpha = 0.22f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            letter,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
    }
}

@Composable
private fun BudgetMeter(ratio: Float, overBudget: Boolean) {
    Column(horizontalAlignment = Alignment.End) {
        Text(
            "${(ratio * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (overBudget) Color(0xFFFFB4A0) else Color.White
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(4.dp)
                .background(Color.White.copy(alpha = 0.22f), PillShape)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio.coerceAtMost(1f))
                    .fillMaxHeight()
                    .background(
                        if (overBudget) Color(0xFFFFB4A0) else Color.White,
                        PillShape
                    )
            )
        }
    }
}

private fun paletteFor(key: String): Pair<Color, Color> {
    val hash = abs(key.hashCode())
    return WalletPalette[hash % WalletPalette.size]
}
