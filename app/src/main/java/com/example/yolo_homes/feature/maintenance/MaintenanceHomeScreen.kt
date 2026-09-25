package com.example.yolo_homes.feature.maintenance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.yolo_homes.core.Formatters
import com.example.yolo_homes.feature.maintenance.components.MaintenanceReceiptCard
import com.example.yolo_homes.feature.maintenance.components.MaintenanceSummaryHeader
import com.example.yolo_homes.ui.components.BarChart
import com.example.yolo_homes.ui.components.EmptyState
import com.example.yolo_homes.ui.components.SectionHeader
import com.example.yolo_homes.ui.components.SurfaceCard

@Composable
fun MaintenanceHomeScreen(
    onViewHistory: () -> Unit,
    onReceiptClick: (String) -> Unit,
    viewModel: MaintenanceViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            MaintenanceSummaryHeader(
                title = "Current Month Collection",
                amount = state.currentMonthTotal,
                currency = state.currency,
                subtitle = "${state.totalEntries} total receipts"
            )
        }

        // The app only records payments after the fact — there's no "Pay"
        // button here on purpose. This tells residents how to actually pay,
        // instead of them expecting one that doesn't exist.
        if (state.payInstructions.isNotBlank()) {
            item {
                SurfaceCard {
                    Row {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(
                                "This is a record of payments already made — it doesn't take payments itself.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "To pay: ${state.payInstructions}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        item { SectionHeader("Collection Trend") }
        item {
            SurfaceCard {
                if (state.trend.all { it.value == 0.0 }) {
                    Text(
                        "No collection data yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    BarChart(entries = state.trend, barColor = MaterialTheme.colorScheme.primary)
                }
            }
        }

        item {
            SectionHeader("Recent Receipts") {
                TextButton(onClick = onViewHistory) { Text("View all") }
            }
        }

        if (state.receipts.isEmpty() && !state.loading) {
            item {
                EmptyState(
                    icon = Icons.Outlined.ReceiptLong,
                    title = "No Receipts Found",
                    message = "Tap the + button to record your first maintenance payment."
                )
            }
        } else {
            items(state.receipts.take(10)) { r ->
                val flat = state.flatsById[r.flatId]
                MaintenanceReceiptCard(
                    flatName = flat?.displayName ?: "Unknown flat",
                    owner = flat?.ownerName ?: "",
                    period = r.period,
                    paymentMethod = r.paymentMethod,
                    amount = r.amount,
                    currency = state.currency,
                    onClick = { onReceiptClick(r.id) }
                )
            }
        }
    }
}
