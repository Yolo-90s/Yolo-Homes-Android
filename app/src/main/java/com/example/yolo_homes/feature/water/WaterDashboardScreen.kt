package com.example.yolo_homes.feature.water

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.yolo_homes.core.Formatters
import com.example.yolo_homes.feature.water.components.WaterReadingCard
import com.example.yolo_homes.ui.components.BarChart
import com.example.yolo_homes.ui.components.EmptyState
import com.example.yolo_homes.ui.components.LineChart
import com.example.yolo_homes.ui.components.SectionHeader
import com.example.yolo_homes.ui.components.StatCard
import com.example.yolo_homes.ui.components.SurfaceCard
import com.example.yolo_homes.ui.theme.ChartAmber
import com.example.yolo_homes.ui.theme.ChartGreen
import com.example.yolo_homes.ui.theme.ChartSky

@Composable
fun WaterDashboardScreen(
    onViewHistory: () -> Unit,
    onReadingClick: (String) -> Unit,
    onViewConsumption: () -> Unit,
    viewModel: WaterViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Consumption", state.totalConsumption, Icons.Rounded.Bolt, ChartSky, Modifier.weight(1f)) {
                    Formatters.liters(it)
                }
                StatCard("Revenue", state.revenue, Icons.Rounded.Payments, ChartGreen, Modifier.weight(1f)) {
                    Formatters.currency(it, state.currency)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    "Highest", state.highest?.liters ?: 0.0, Icons.Rounded.TrendingUp, ChartAmber, Modifier.weight(1f)
                ) { Formatters.liters(it) }
                StatCard(
                    "Lowest", state.lowest?.liters ?: 0.0, Icons.Rounded.TrendingDown, ChartGreen, Modifier.weight(1f)
                ) { Formatters.liters(it) }
            }
        }

        item { SectionHeader("Monthly Trend") }
        item {
            SurfaceCard {
                if (state.trend.all { it.value == 0.0 }) {
                    Text("No consumption data yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LineChart(entries = state.trend, lineColor = ChartSky)
                }
            }
        }

        item {
            SectionHeader("Top Consumers") {
                TextButton(onClick = onViewConsumption) { Text("Flat-wise →") }
            }
        }
        if (state.topConsumers.isEmpty()) {
            item {
                Text("No readings this month yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            item {
                SurfaceCard {
                    BarChart(
                        entries = state.topConsumers.map {
                            com.example.yolo_homes.ui.components.ChartEntry(it.flat.flatNo, it.liters)
                        },
                        barColor = ChartSky
                    )
                }
            }
        }

        item {
            SectionHeader("Recent Readings") {
                TextButton(onClick = onViewHistory) { Text("View all") }
            }
        }
        if (state.readings.isEmpty() && !state.loading) {
            item {
                EmptyState(
                    icon = Icons.Outlined.WaterDrop,
                    title = "No Readings Available",
                    message = "Add a meter reading to start tracking water consumption."
                )
            }
        } else {
            items(state.readings.take(10)) { reading ->
                val flat = state.flatsById[reading.flatId]
                WaterReadingCard(
                    flatName = flat?.displayName ?: reading.flatId,
                    owner = flat?.occupantName ?: "",
                    usageLiters = reading.usageLiters,
                    excessLiters = reading.excessLiters,
                    amount = reading.amount,
                    currency = state.currency,
                    dateLabel = Formatters.shortDate(reading.date),
                    onClick = { onReadingClick(reading.id) }
                )
            }
        }
    }
}
