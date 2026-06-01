package com.example.yolo_homes.feature.maintenance.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.yolo_homes.core.Formatters
import com.example.yolo_homes.data.model.MaintenanceReceipt
import com.example.yolo_homes.ui.theme.ChartGreen
import com.example.yolo_homes.ui.theme.LightPrimary

/**
 * Maintenance-only components. The module's visual identity is a blue→green
 * gradient summary banner and receipt-styled list rows with a leading icon chip.
 */

@Composable
fun MaintenanceSummaryHeader(
    title: String,
    amount: Double,
    currency: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(LightPrimary, ChartGreen)))
                .padding(20.dp)
        ) {
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f)
                )
                Text(
                    Formatters.currency(amount, currency),
                    style = MaterialTheme.typography.displaySmall,
                    color = androidx.compose.ui.graphics.Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
fun MaintenanceReceiptCard(
    flatName: String,
    owner: String,
    period: String,
    paymentMethod: String,
    amount: Double,
    currency: String,
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
                    .background(LightPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.ReceiptLong, contentDescription = null, tint = LightPrimary)
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
                    "${Formatters.monthLabel(period)} · $paymentMethod",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                Formatters.currency(amount, currency),
                style = MaterialTheme.typography.titleMedium,
                color = ChartGreen,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
