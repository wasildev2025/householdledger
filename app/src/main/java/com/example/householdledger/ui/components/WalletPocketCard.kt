package com.example.householdledger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
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
    transferredIn: Double, // money admin transferred to this person this cycle
    netBalance: Double,    // transferredIn − monthSpend. Negative → admin owes them.
    allocation: Double,    // 0 if none
    identityKey: String,   // stable id for palette hash
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onTopUp: ((Double) -> Unit)? = null  // invoked when the owed pill is tapped
) {
    val (top, bottom) = paletteFor(identityKey)

    // Plain clipped Box instead of Surface(onClick). A Surface with
    // `onClick = {}, enabled = false` still installs a clickable modifier that
    // silently consumes touches intended for the inner Top-Up pill.
    val cardModifier = modifier
        .width(260.dp)
        .height(170.dp)
        .clip(RoundedCornerShape(22.dp))
        .let { base -> if (onClick != null) base.clickable(onClick = onClick) else base }

    Box(
        modifier = cardModifier
            .background(
                brush = Brush.linearGradient(colors = listOf(top, bottom))
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
                    // Transferred ↔ Spent mini-row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        WalletStat(
                            label = "TRANSFERRED",
                            amount = transferredIn,
                            modifier = Modifier.weight(1f)
                        )
                        WalletStat(
                            label = "SPENT",
                            amount = monthSpend,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    // Balance / owed strip. The sign and label flip based on whether the
                    // person has money left or admin now owes them. When owed, the strip
                    // becomes a tappable "Top Up" CTA that opens a prefilled Transfer sheet.
                    BalanceStrip(
                        netBalance = netBalance,
                        onTopUp = if (netBalance < 0 && onTopUp != null) {
                            { onTopUp(kotlin.math.abs(netBalance)) }
                        } else null
                    )
                }
            }
    }
}

@Composable
private fun WalletStat(label: String, amount: Double, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            label,
            style = EyebrowCaps,
            color = Color.White.copy(alpha = 0.72f)
        )
        Spacer(Modifier.height(2.dp))
        MoneyText(
            amount = amount,
            style = MoneyBody.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
            color = Color.White
        )
    }
}

@Composable
private fun BalanceStrip(netBalance: Double, onTopUp: (() -> Unit)? = null) {
    val owed = netBalance < 0
    val label = when {
        netBalance == 0.0 -> "SETTLED"
        owed -> "TOP UP · OWED"
        else -> "WALLET BALANCE"
    }
    val accent = if (owed) Color(0xFFFFB4A0) else Color(0xFF7CE4B4)
    val surfaceModifier = Modifier.fillMaxWidth()
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color.White.copy(alpha = if (owed && onTopUp != null) 0.22f else 0.14f),
        modifier = surfaceModifier,
        onClick = onTopUp ?: {},
        enabled = onTopUp != null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(accent, CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    label,
                    style = EyebrowCaps,
                    color = Color.White.copy(alpha = 0.92f)
                )
            }
            MoneyText(
                amount = kotlin.math.abs(netBalance),
                style = MoneyBody.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                color = accent
            )
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
