package com.example.yolo_homes.feature.flats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.yolo_homes.core.Formatters
import com.example.yolo_homes.feature.maintenance.components.MaintenanceReceiptCard
import com.example.yolo_homes.feature.water.components.WaterReadingCard
import com.example.yolo_homes.ui.components.DetailRow
import com.example.yolo_homes.ui.components.EmptyState
import com.example.yolo_homes.ui.components.SectionHeader
import com.example.yolo_homes.ui.components.SurfaceCard

/**
 * A single flat's full history — every maintenance receipt and water reading ever recorded for
 * it, grouped by year. Reuses the same row components the global history screens already use.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlatDetailScreen(
    onBack: () -> Unit,
    onReceiptClick: (String) -> Unit,
    onReadingClick: (String) -> Unit,
    viewModel: FlatDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val flat = state.flat

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(flat?.displayName ?: "Flat") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                SurfaceCard {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(flat?.occupantName?.ifBlank { "—" } ?: "—", style = MaterialTheme.typography.titleMedium)
                            if (flat?.vacant == true) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text("Vacant") },
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                            if (flat?.isStaff == true) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text("Staff") },
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                        if (!flat?.ownerPhone.isNullOrBlank()) {
                            DetailRow("Phone", flat?.ownerPhone.orEmpty())
                        }
                        if (!flat?.email.isNullOrBlank()) {
                            DetailRow("Email", flat?.email.orEmpty())
                        }
                        DetailRow(
                            "Expected Maintenance",
                            Formatters.currency(state.maintenanceMonthlyRate, state.currency) + " / month"
                        )
                    }
                }
            }

            item { SectionHeader("Maintenance History") }
            if (state.receiptsByYear.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.ReceiptLong,
                        title = "No Receipts Yet",
                        message = "Maintenance payments for this flat will show up here."
                    )
                }
            } else {
                state.receiptsByYear.forEach { group ->
                    item {
                        YearSummaryRow(
                            year = group.year,
                            count = group.items.size,
                            total = Formatters.currency(group.items.sumOf { it.amount }, state.currency)
                        )
                    }
                    items(group.items, key = { "r_${it.id}" }) { r ->
                        val flatName = flat?.displayName ?: "—"
                        MaintenanceReceiptCard(
                            flatName = flatName,
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

            item { SectionHeader("Water History") }
            if (state.readingsByYear.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.WaterDrop,
                        title = "No Readings Yet",
                        message = "Meter readings for this flat will show up here."
                    )
                }
            } else {
                state.readingsByYear.forEach { group ->
                    item {
                        YearSummaryRow(
                            year = group.year,
                            count = group.items.size,
                            total = Formatters.liters(group.items.sumOf { it.usageLiters })
                        )
                    }
                    items(group.items, key = { "w_${it.id}" }) { r ->
                        WaterReadingCard(
                            flatName = flat?.displayName ?: "—",
                            owner = flat?.occupantName ?: "",
                            usageLiters = r.usageLiters,
                            excessLiters = r.excessLiters,
                            amount = r.amount,
                            currency = state.currency,
                            dateLabel = Formatters.shortDate(r.date),
                            onClick = { onReadingClick(r.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun YearSummaryRow(year: String, count: Int, total: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(year, style = MaterialTheme.typography.titleSmall)
        Text(
            "$count entries · $total",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
