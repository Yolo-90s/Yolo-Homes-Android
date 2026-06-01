package com.example.yolo_homes.feature.water.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.yolo_homes.core.Formatters
import com.example.yolo_homes.ui.theme.ChartSky
import com.example.yolo_homes.ui.theme.LightSecondary

/**
 * Water-only components. The module's identity is a sky/teal palette, a droplet
 * leading chip, a horizontal month-filter bar, and a flat-consumption row with a
 * usage progress bar relative to the heaviest consumer.
 */

/** Horizontal scrolling month selector used on water screens. */
@Composable
fun MonthFilterBar(
    periods: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        periods.forEach { p ->
            FilterChip(
                selected = p == selected,
                onClick = { onSelect(p) },
                label = { Text(Formatters.monthLabel(p)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ChartSky.copy(alpha = 0.18f),
                    selectedLabelColor = ChartSky
                )
            )
        }
    }
}

@Composable
fun WaterReadingCard(
    flatName: String,
    owner: String,
    usageLiters: Double,
    excessLiters: Double,
    amount: Double,
    currency: String,
    dateLabel: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(ChartSky, LightSecondary))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.WaterDrop, contentDescription = null, tint = Color.White)
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Flat $flatName", style = MaterialTheme.typography.titleSmall)
                    if (owner.isNotBlank()) {
                        Text(
                            "  •  $owner",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    "Used ${Formatters.liters(usageLiters)} · billable ${Formatters.liters(excessLiters)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    dateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                Formatters.currency(amount, currency),
                style = MaterialTheme.typography.titleMedium,
                color = ChartSky,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * One flat's consumption for the selected month, with a usage bar scaled to [maxLiters].
 * Renders even when the flat has no reading (shows "No reading").
 */
@Composable
fun FlatConsumptionRow(
    rank: Int,
    flatName: String,
    occupant: String,
    liters: Double,
    amount: Double,
    currency: String,
    hasReading: Boolean,
    maxLiters: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ChartSky.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$rank", style = MaterialTheme.typography.labelLarge, color = ChartSky)
                }
                Column(Modifier.weight(1f)) {
                    Text(flatName, style = MaterialTheme.typography.titleSmall)
                    Text(
                        occupant.ifBlank { "—" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        if (hasReading) Formatters.liters(liters) else "No reading",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (hasReading) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (hasReading) {
                        Text(
                            Formatters.currency(amount, currency),
                            style = MaterialTheme.typography.bodySmall,
                            color = ChartSky
                        )
                    }
                }
            }
            // usage bar
            val fraction = if (maxLiters > 0.0) (liters / maxLiters).toFloat().coerceIn(0f, 1f) else 0f
            Box(
                Modifier
                    .padding(top = 10.dp)
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Brush.horizontalGradient(listOf(ChartSky, LightSecondary)))
                )
            }
        }
    }
}
