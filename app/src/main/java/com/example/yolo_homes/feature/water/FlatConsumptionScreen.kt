package com.example.yolo_homes.feature.water

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.WaterDrop
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.yolo_homes.core.Formatters
import com.example.yolo_homes.feature.water.components.FlatConsumptionRow
import com.example.yolo_homes.feature.water.components.MonthFilterBar
import com.example.yolo_homes.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlatConsumptionScreen(
    onBack: () -> Unit,
    viewModel: WaterViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val maxLiters = state.flatConsumption.maxOfOrNull { it.liters } ?: 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Flat-wise Consumption") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            MonthFilterBar(
                periods = state.availablePeriods,
                selected = state.selectedPeriod,
                onSelect = viewModel::selectPeriod,
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
            )

            // Month summary strip
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Consumption", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(Formatters.liters(state.selectedMonthTotal), style = MaterialTheme.typography.titleMedium)
                }
                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                    Text("Revenue", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        Formatters.currency(state.selectedMonthRevenue, state.currency),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (state.flats.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.WaterDrop,
                    title = "No Flats Found",
                    message = "Add flats to masterFlats to see consumption here."
                )
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(state.flatConsumption) { index, item ->
                        FlatConsumptionRow(
                            rank = index + 1,
                            flatName = item.flat.displayName,
                            occupant = item.flat.occupantName,
                            liters = item.liters,
                            amount = item.amount,
                            currency = state.currency,
                            hasReading = item.hasReading,
                            maxLiters = maxLiters
                        )
                    }
                }
            }
        }
    }
}
