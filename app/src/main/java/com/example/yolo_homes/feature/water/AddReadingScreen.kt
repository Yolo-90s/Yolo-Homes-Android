package com.example.yolo_homes.feature.water

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
import com.example.yolo_homes.data.model.Flat
import com.example.yolo_homes.ui.components.DetailRow
import com.example.yolo_homes.ui.components.LabeledDropdown
import com.example.yolo_homes.ui.components.PrimaryButton
import com.example.yolo_homes.ui.components.SurfaceCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReadingScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: WaterViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()

    var selectedFlat by remember { mutableStateOf<Flat?>(null) }
    var currentText by remember { mutableStateOf("") }

    LaunchedEffect(saveState) {
        if (saveState is WaterSaveState.Success) {
            viewModel.resetSaveState()
            viewModel.resetDraft()
            onSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Reading") },
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
                optionLabel = { "${it.displayName} — ${it.occupantName}" },
                onSelect = {
                    selectedFlat = it
                    currentText = ""
                    viewModel.onFlatSelected(it.id)
                }
            )

            if (selectedFlat != null) {
                OutlinedTextField(
                    value = Formatters.liters(draft.previous),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Previous Reading") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = currentText,
                    onValueChange = {
                        currentText = it.filter { c -> c.isDigit() || c == '.' }
                        viewModel.onCurrentReadingChanged(currentText.toDoubleOrNull() ?: 0.0)
                    },
                    label = { Text("Current Reading") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Live preview
                SurfaceCard {
                    Column {
                        Text("Live Calculation", style = MaterialTheme.typography.titleSmall)
                        androidx.compose.foundation.layout.Spacer(Modifier.padding(4.dp))
                        DetailRow("Usage", Formatters.liters(draft.bill.usage))
                        DetailRow("Exclude Limit (≤)", Formatters.liters(state.settings.freeLiters))
                        DetailRow("Billable Usage", Formatters.liters(draft.bill.excess))
                        DetailRow("Rate / Liter", Formatters.currencyPrecise(state.settings.ratePerExcessLiter, state.currency))
                        DetailRow("Amount", Formatters.currency(draft.bill.amount, state.currency))
                    }
                }
            }

            (saveState as? WaterSaveState.Error)?.let {
                Text(it.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            PrimaryButton(
                text = "Save Reading",
                loading = saveState is WaterSaveState.Saving,
                enabled = selectedFlat != null && (currentText.toDoubleOrNull() ?: 0.0) >= draft.previous &&
                    currentText.isNotBlank(),
                onClick = { selectedFlat?.let { viewModel.saveReading(it.id) } }
            )
        }
    }
}
