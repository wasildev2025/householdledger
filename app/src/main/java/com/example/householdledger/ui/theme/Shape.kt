package com.example.householdledger.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),   // tags / micro pills
    small      = RoundedCornerShape(12.dp),  // inputs
    medium     = RoundedCornerShape(14.dp),  // buttons
    large      = RoundedCornerShape(20.dp),  // cards
    extraLarge = RoundedCornerShape(28.dp)   // hero / wallet cards
)

val PillShape = RoundedCornerShape(999.dp)
