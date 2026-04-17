package com.example.householdledger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.householdledger.ui.theme.HeroGradientBottom
import com.example.householdledger.ui.theme.HeroGradientMid
import com.example.householdledger.ui.theme.HeroGradientTop
import com.example.householdledger.ui.theme.HeroSheen

/**
 * The signature dark-navy balance hero. A diagonal gradient with a faint
 * highlight sheen line running top-left → bottom-right, mimicking polished
 * metal / premium fintech card mockups.
 */
@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(24.dp),
    cornerRadius: androidx.compose.ui.unit.Dp = 28.dp,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(cornerRadius))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            HeroGradientTop,
                            HeroGradientMid,
                            HeroGradientBottom
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
        ) {
            // Diagonal sheen overlay: a faint lighter stripe cutting across the card
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                HeroSheen.copy(alpha = 0.14f),
                                Color.Transparent,
                                Color.Transparent
                            ),
                            start = Offset(0f, Float.POSITIVE_INFINITY),
                            end = Offset(Float.POSITIVE_INFINITY, 0f)
                        )
                    )
            )
            Box(modifier = Modifier.padding(contentPadding)) {
                content()
            }
        }
    }
}
