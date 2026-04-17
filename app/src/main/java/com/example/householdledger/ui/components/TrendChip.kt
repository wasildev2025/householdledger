package com.example.householdledger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.SouthEast
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.householdledger.ui.theme.appColors
import kotlin.math.abs

enum class TrendDirection { Up, Down, Neutral }

@Composable
fun TrendChip(
    percent: Float,
    modifier: Modifier = Modifier,
    direction: TrendDirection = when {
        percent > 0f -> TrendDirection.Up
        percent < 0f -> TrendDirection.Down
        else -> TrendDirection.Neutral
    },
    positiveIsGood: Boolean = true,
    surface: Color? = null,
    content: Color? = null
) {
    val good = (direction == TrendDirection.Up && positiveIsGood) ||
        (direction == TrendDirection.Down && !positiveIsGood)

    val resolvedContent = content ?: when (direction) {
        TrendDirection.Up -> if (positiveIsGood) appColors.income else appColors.expense
        TrendDirection.Down -> if (positiveIsGood) appColors.expense else appColors.income
        TrendDirection.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val resolvedSurface = surface ?: when {
        direction == TrendDirection.Neutral -> MaterialTheme.colorScheme.surfaceVariant
        good -> appColors.incomeContainer
        else -> appColors.expenseContainer
    }

    Row(
        modifier = modifier
            .background(resolvedSurface, RoundedCornerShape(999.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (direction) {
            TrendDirection.Up -> Icon(
                imageVector = Icons.Default.NorthEast,
                contentDescription = null,
                tint = resolvedContent,
                modifier = Modifier.size(10.dp)
            )
            TrendDirection.Down -> Icon(
                imageVector = Icons.Default.SouthEast,
                contentDescription = null,
                tint = resolvedContent,
                modifier = Modifier.size(10.dp)
            )
            TrendDirection.Neutral -> Spacer(Modifier.width(0.dp))
        }
        Spacer(Modifier.width(3.dp))
        Text(
            text = "${"%.1f".format(abs(percent))}%",
            style = MaterialTheme.typography.labelSmall,
            color = resolvedContent
        )
    }
}
