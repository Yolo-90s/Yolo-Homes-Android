package com.example.yolo_homes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.yolo_homes.ui.theme.ChartTeal
import com.example.yolo_homes.ui.theme.LightPrimary

/**
 * The blue→teal "hero" gradient treatment — promoted from the two ad hoc gradient banners that
 * already existed (Maintenance's summary card, Water's reading-card icon) into one named,
 * consistently-applied header used at the top of Home, Flat Directory, and Reports.
 */
@Composable
fun GradientHeader(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(listOf(LightPrimary, ChartTeal)),
                shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
            )
            .padding(horizontal = 20.dp, vertical = 24.dp),
        content = content
    )
}
