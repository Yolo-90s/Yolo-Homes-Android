package com.example.yolo_homes.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Formal corner-radius tokens for the visual-redesign pass. Most existing screens set
 * `RoundedCornerShape(_.dp)` ad hoc per call site (left as-is — a low-value, high-risk mass
 * find/replace); new shared components (GradientHeader, SoftCard, Community Board, Flat
 * Directory) use these instead, so future screens have one place to match going forward.
 */
object AppShapes {
    val chip = RoundedCornerShape(10.dp)
    val small = RoundedCornerShape(14.dp)
    val medium = RoundedCornerShape(20.dp)
    val large = RoundedCornerShape(28.dp)
}
