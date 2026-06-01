package com.example.yolo_homes.feature.water

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.yolo_homes.ui.components.DetailRow
import com.example.yolo_homes.ui.components.PrimaryButton
import com.example.yolo_homes.ui.components.SurfaceCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterBillScreen(
    readingId: String,
    onBack: () -> Unit,
    viewModel: WaterViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val reading = state.readings.firstOrNull { it.id == readingId }
    val flat = reading?.let { state.flatsById[it.flatId] }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Water Bill") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (reading == null) {
                Text("Reading not found.", style = MaterialTheme.typography.bodyMedium)
                return@Column
            }
            SurfaceCard {
                Column {
                    Text(state.settings.apartmentName, style = MaterialTheme.typography.titleMedium)
                    Text("Water Bill", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.padding(4.dp))
                    Text(
                        Formatters.currency(reading.amount, state.currency),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.padding(4.dp))
                    DetailRow("Flat Number", flat?.displayName ?: "—")
                    DetailRow("Owner Name", flat?.ownerName ?: "—")
                    DetailRow("Previous Reading", Formatters.liters(reading.previousReading))
                    DetailRow("Current Reading", Formatters.liters(reading.currentReading))
                    DetailRow("Usage", Formatters.liters(reading.usageLiters))
                    DetailRow("Exclude Limit (≤)", Formatters.liters(state.settings.freeLiters))
                    DetailRow("Billable Usage", Formatters.liters(reading.excessLiters))
                    DetailRow("Rate / Liter", Formatters.currencyPrecise(state.settings.ratePerExcessLiter, state.currency))
                    DetailRow("Date", Formatters.shortDate(reading.date))
                }
            }
            PrimaryButton(
                text = "Share PDF",
                onClick = {
                    val file = PdfExporter.export(
                        context = context,
                        fileName = "water_bill_${reading.id}.pdf",
                        title = "Water Bill",
                        subtitle = "${state.settings.apartmentName} • ${flat?.displayName ?: ""}",
                        rows = listOf(
                            PdfRow("Flat Number", flat?.displayName ?: "—"),
                            PdfRow("Owner Name", flat?.ownerName ?: "—"),
                            PdfRow("Previous Reading", Formatters.liters(reading.previousReading)),
                            PdfRow("Current Reading", Formatters.liters(reading.currentReading)),
                            PdfRow("Usage", Formatters.liters(reading.usageLiters)),
                            PdfRow("Exclude Limit", Formatters.liters(state.settings.freeLiters)),
                            PdfRow("Billable Usage", Formatters.liters(reading.excessLiters)),
                            PdfRow("Rate / Liter", Formatters.currencyPrecise(state.settings.ratePerExcessLiter, state.currency)),
                            PdfRow("Amount", Formatters.currency(reading.amount, state.currency), emphasize = true)
                        )
                    )
                    PdfExporter.share(context, file, "Share water bill")
                }
            )
        }
    }
}
