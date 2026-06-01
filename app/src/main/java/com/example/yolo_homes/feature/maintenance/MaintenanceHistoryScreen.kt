package com.example.yolo_homes.feature.maintenance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.yolo_homes.core.Formatters
import com.example.yolo_homes.feature.maintenance.components.MaintenanceReceiptCard
import com.example.yolo_homes.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceHistoryScreen(
    onBack: () -> Unit,
    onReceiptClick: (String) -> Unit,
    viewModel: MaintenanceViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var sortNewest by remember { mutableStateOf(true) }
    var periodFilter by remember { mutableStateOf<String?>(null) }

    val availablePeriods = remember(state.receipts) {
        state.receipts.map { it.period }.distinct().sortedDescending()
    }

    val filtered = state.receipts
        .filter { periodFilter == null || it.period == periodFilter }
        .filter {
            if (query.isBlank()) true else {
                val flat = state.flatsById[it.flatId]
                val hay = listOfNotNull(
                    flat?.displayName, flat?.ownerName, flat?.tenantName, it.period, it.paymentMethod
                ).joinToString(" ").lowercase()
                hay.contains(query.lowercase())
            }
        }
        .let { if (sortNewest) it else it.sortedBy { r -> r.amount } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Maintenance History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search flat, owner, period…") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = sortNewest, onClick = { sortNewest = true }, label = { Text("Newest") })
                FilterChip(selected = !sortNewest, onClick = { sortNewest = false }, label = { Text("Amount") })
                FilterChip(selected = periodFilter == null, onClick = { periodFilter = null }, label = { Text("All months") })
                availablePeriods.forEach { p ->
                    FilterChip(
                        selected = periodFilter == p,
                        onClick = { periodFilter = if (periodFilter == p) null else p },
                        label = { Text(Formatters.monthLabel(p)) }
                    )
                }
            }

            if (filtered.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.ReceiptLong,
                    title = "No Receipts Found",
                    message = "Try a different search or filter."
                )
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filtered, key = { it.id }) { r ->
                        val flat = state.flatsById[r.flatId]
                        MaintenanceReceiptCard(
                            flatName = flat?.displayName ?: r.flatId,
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
    }
}
