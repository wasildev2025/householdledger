package com.example.householdledger.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
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

/**
 * A modern, soft-styled card used across the application.
 * Implements a "Hybrid Soft UI" style with larger corner radii and subtle elevation
 * to create depth without sacrificing accessibility.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    tonal: Boolean = false,
    // Using White for non-tonal cards to make them "pop" against tinted backgrounds
    containerColor: Color = if (tonal) MaterialTheme.colorScheme.surfaceVariant else Color.White,
    // Defaulting to null (no border) for the clean, modern look seen in the reference
    borderColor: Color? = null,
    cornerRadius: Dp = 28.dp, // Increased from 20dp for a friendlier, modern feel
    elevation: Dp = 4.dp,    // Increased from 0dp to provide soft depth shadows
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
            Box(Modifier.padding(contentPadding)) { content() }
        }
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
            border = border
        ) {
            Box(Modifier.padding(contentPadding)) { content() }
        }
    }
}
