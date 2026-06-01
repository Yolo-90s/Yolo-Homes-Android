package com.example.yolo_homes.feature.maintenance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
fun ReceiptDetailScreen(
    receiptId: String,
    onBack: () -> Unit,
    viewModel: MaintenanceViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val receipt = state.receipts.firstOrNull { it.id == receiptId }
    val flat = receipt?.let { state.flatsById[it.flatId] }

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("Receipt") },
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
            if (receipt == null) {
                Text("Receipt not found.", style = MaterialTheme.typography.bodyMedium)
                return@Column
            }
            SurfaceCard {
                Column {
                    Text("Maintenance Receipt", style = MaterialTheme.typography.titleMedium)
                    Text(
                        Formatters.currency(receipt.amount, state.currency),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    androidx.compose.foundation.layout.Spacer(Modifier.padding(4.dp))
                    DetailRow("Flat Number", flat?.displayName ?: "—")
                    DetailRow("Owner Name", flat?.ownerName ?: "—")
                    DetailRow("Period", Formatters.monthLabel(receipt.period))
                    DetailRow("Paid Date", Formatters.shortDate(receipt.paidDate))
                    DetailRow("Payment Method", receipt.paymentMethod)
                    DetailRow("Captured By", receipt.capturedBy.ifBlank { "—" })
                    if (receipt.edited) DetailRow("Note", "Edited")
                }
            }
            PrimaryButton(
                text = "Share PDF",
                onClick = {
                    val file = PdfExporter.export(
                        context = context,
                        fileName = "receipt_${receipt.id}.pdf",
                        title = "Maintenance Receipt",
                        subtitle = "${flat?.displayName ?: ""} • ${Formatters.monthLabel(receipt.period)}",
                        rows = listOf(
                            PdfRow("Flat Number", flat?.displayName ?: "—"),
                            PdfRow("Owner Name", flat?.ownerName ?: "—"),
                            PdfRow("Period", Formatters.monthLabel(receipt.period)),
                            PdfRow("Paid Date", Formatters.shortDate(receipt.paidDate)),
                            PdfRow("Payment Method", receipt.paymentMethod),
                            PdfRow("Amount", Formatters.currency(receipt.amount, state.currency), emphasize = true)
                        )
                    )
                    PdfExporter.share(context, file, "Share receipt")
                }
            )
        }
    }
}
