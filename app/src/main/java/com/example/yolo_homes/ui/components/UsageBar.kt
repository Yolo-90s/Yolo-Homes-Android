package com.example.yolo_homes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.yolo_homes.core.Formatters
import com.example.yolo_homes.ui.theme.ChartSky
import com.example.yolo_homes.ui.theme.ChartTeal
import com.example.yolo_homes.ui.theme.Warning

/**
 * "8,200 / 10,000 L free" text + a thin progress bar — replaces the old plain
 * "limitLabel: value" text row so the free-water-limit concept is visible at a
 * glance, not just readable. [used] over [limit] turns amber once it's fully consumed.
 */
@Composable
fun FreeLimitUsageBar(
    label: String,
    used: Double,
    limit: Double,
    modifier: Modifier = Modifier
) {
    val fraction = if (limit > 0.0) (used / limit).toFloat().coerceIn(0f, 1f) else 0f
    val overLimit = limit > 0.0 && used > limit
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "${Formatters.liters(used)} / ${Formatters.liters(limit)} free",
                style = MaterialTheme.typography.labelMedium,
                color = if (overLimit) Warning else MaterialTheme.colorScheme.onSurface
            )
        }
        Box(
            Modifier
                .padding(top = 6.dp)
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            if (overLimit) listOf(Warning, Warning) else listOf(ChartSky, ChartTeal)
                        )
                    )
            )
        }
    }
}
