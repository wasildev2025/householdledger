package com.example.householdledger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.householdledger.ui.theme.EyebrowCaps

/**
 * Thin persistent strip at the very top of Home. Always visible, gives a
 * permanent readout of the current cycle progress without taking a card slot.
 *
 * Left: ELAPSED day-of-cycle indicator (saffron fill)
 * Right: eyebrow label ("Day 12 of 31 · 28 Apr")
 */
@Composable
fun CycleRibbon(
    dayIndex: Int,
    cycleLengthDays: Int,
    endLabel: String,
    modifier: Modifier = Modifier
) {
    val ratio = if (cycleLengthDays > 0) {
        ((dayIndex + 1).toFloat() / cycleLengthDays).coerceIn(0f, 1f)
    } else 0f
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .background(
                    MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(999.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio)
                    .fillMaxHeight()
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(999.dp)
                    )
            )
        }
        Text(
            text = "DAY ${dayIndex + 1} · ENDS $endLabel".uppercase(),
            style = EyebrowCaps,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}
