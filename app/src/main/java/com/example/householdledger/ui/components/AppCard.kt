package com.example.householdledger.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    tonal: Boolean = false,
    containerColor: Color = if (tonal) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
    borderColor: Color? = if (tonal) null else MaterialTheme.colorScheme.outlineVariant,
    cornerRadius: Dp = 20.dp,
    elevation: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val border = borderColor?.let { BorderStroke(1.dp, it) }
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
            border = border
        ) {
            androidx.compose.foundation.layout.Box(Modifier.padding(contentPadding)) { content() }
        }
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
            border = border
        ) {
            androidx.compose.foundation.layout.Box(Modifier.padding(contentPadding)) { content() }
        }
    }
}
