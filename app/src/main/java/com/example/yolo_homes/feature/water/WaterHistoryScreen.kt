package com.example.yolo_homes.feature.water

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WaterDrop
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.yolo_homes.core.Formatters
import com.example.yolo_homes.feature.water.components.WaterReadingCard
import com.example.yolo_homes.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterHistoryScreen(
    onBack: () -> Unit,
    onReadingClick: (String) -> Unit,
    viewModel: WaterViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var sortNewest by remember { mutableStateOf(true) }

    val filtered = state.readings
        .filter {
            if (query.isBlank()) true else {
                val flat = state.flatsById[it.flatId]
                listOfNotNull(flat?.displayName, flat?.ownerName, flat?.tenantName)
                    .joinToString(" ").lowercase().contains(query.lowercase())
            }
        }
        .let { if (sortNewest) it else it.sortedByDescending { r -> r.usageLiters } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reading History") },
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
                placeholder = { Text("Search flat or owner…") },
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
                FilterChip(selected = !sortNewest, onClick = { sortNewest = false }, label = { Text("Highest usage") })
            }

            if (filtered.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.WaterDrop,
                    title = "No Readings Available",
                    message = "Try a different search."
                )
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filtered, key = { it.id }) { reading ->
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
    }
}
