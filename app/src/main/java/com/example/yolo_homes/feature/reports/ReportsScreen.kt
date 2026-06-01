package com.example.yolo_homes.feature.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.yolo_homes.core.Formatters
import com.example.yolo_homes.core.PdfExporter
import com.example.yolo_homes.core.PdfRow
import com.example.yolo_homes.ui.components.BarChart
import com.example.yolo_homes.ui.components.LineChart
import com.example.yolo_homes.ui.components.PrimaryButton
import com.example.yolo_homes.ui.components.SectionHeader
import com.example.yolo_homes.ui.components.StatCard
import com.example.yolo_homes.ui.components.SurfaceCard
import com.example.yolo_homes.ui.theme.ChartGreen
import com.example.yolo_homes.ui.theme.ChartSky
import com.example.yolo_homes.ui.theme.LightPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onBack: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports") },
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
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { SectionHeader("Overview (12 months)") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Maintenance", state.totalMaintenance, Icons.Rounded.Payments, LightPrimary, Modifier.weight(1f)) {
                    Formatters.currency(it, state.currency)
                }
                StatCard("Water Revenue", state.totalWaterRevenue, Icons.Rounded.WaterDrop, ChartSky, Modifier.weight(1f)) {
                    Formatters.currency(it, state.currency)
                }
            }
        }
        item {
            StatCard("Total Consumption", state.totalConsumption, Icons.Rounded.Bolt, ChartGreen, Modifier.fillMaxWidth()) {
                Formatters.liters(it)
            }
        }

        item { SectionHeader("Maintenance Collection") }
        item {
            SurfaceCard {
                if (state.maintenanceByMonth.all { it.value == 0.0 }) {
                    Text("No data yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    BarChart(entries = state.maintenanceByMonth, barColor = LightPrimary)
                }
            }
        }

        item { SectionHeader("Water Revenue") }
        item {
            SurfaceCard {
                if (state.waterByMonth.all { it.value == 0.0 }) {
                    Text("No data yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LineChart(entries = state.waterByMonth, lineColor = ChartSky)
                }
            }
        }

        item {
            PrimaryButton(
                text = "Export Combined Report (PDF)",
                onClick = {
                    val file = PdfExporter.export(
                        context = context,
                        fileName = "yolo_homes_report.pdf",
                        title = "Yolo-Home's Report",
                        subtitle = "Last 12 months summary",
                        rows = buildList {
                            add(PdfRow("Total Maintenance", Formatters.currency(state.totalMaintenance, state.currency)))
                            add(PdfRow("Total Water Revenue", Formatters.currency(state.totalWaterRevenue, state.currency)))
                            add(PdfRow("Total Consumption", Formatters.liters(state.totalConsumption)))
                            add(PdfRow("Receipts", state.receipts.size.toString()))
                            add(PdfRow("Readings", state.readings.size.toString()))
                            add(
                                PdfRow(
                                    "Grand Total Revenue",
                                    Formatters.currency(state.totalMaintenance + state.totalWaterRevenue, state.currency),
                                    emphasize = true
                                )
                            )
                        }
                    )
                    PdfExporter.share(context, file, "Share report")
                }
            )
        }
    }
    }
}
