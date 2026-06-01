package com.example.yolo_homes.feature.maintenance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.yolo_homes.core.Formatters
import com.example.yolo_homes.core.PaymentMethods
import com.example.yolo_homes.data.model.Flat
import com.example.yolo_homes.ui.components.LabeledDropdown
import com.example.yolo_homes.ui.components.PrimaryButton
import com.example.yolo_homes.ui.components.SurfaceCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMaintenanceScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: MaintenanceViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()

    var selectedFlat by remember { mutableStateOf<Flat?>(null) }
    var amount by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf(PaymentMethods.ALL.first()) }
    val periods = remember { Formatters.recentPeriods(12).reversed() }
    var period by remember { mutableStateOf(periods.first()) }

    LaunchedEffect(saveState) {
        if (saveState is SaveState.Success) {
            viewModel.resetSaveState()
            onSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Maintenance") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LabeledDropdown(
                label = "Select Flat",
                options = state.flats,
                selected = selectedFlat,
                optionLabel = { "${it.displayName} — ${it.ownerName}" },
                onSelect = { selectedFlat = it }
            )

            selectedFlat?.let { flat ->
                SurfaceCard {
                    Column {
                        Text("Owner", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(flat.ownerName.ifBlank { "—" }, style = MaterialTheme.typography.titleSmall)
                        Text(flat.ownerPhone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            LabeledDropdown(
                label = "Period",
                options = periods,
                selected = period,
                optionLabel = { Formatters.monthLabel(it) },
                onSelect = { period = it }
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Amount") },
                prefix = { Text(state.currency) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            LabeledDropdown(
                label = "Payment Method",
                options = PaymentMethods.ALL,
                selected = paymentMethod,
                optionLabel = { it },
                onSelect = { paymentMethod = it }
            )

            (saveState as? SaveState.Error)?.let {
                Text(it.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            PrimaryButton(
                text = "Save Receipt",
                loading = saveState is SaveState.Saving,
                enabled = selectedFlat != null && (amount.toDoubleOrNull() ?: 0.0) > 0.0,
                onClick = {
                    viewModel.addReceipt(
                        flatId = selectedFlat!!.id,
                        amount = amount.toDoubleOrNull() ?: 0.0,
                        period = period,
                        paymentMethod = paymentMethod
                    )
                }
            )
        }
    }
}
