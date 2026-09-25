package com.example.yolo_homes.feature.water

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Top-level "Water" bottom-nav destination. Thin wrapper around the
 * existing [WaterDashboardScreen] content — see MaintenanceScreen.kt for
 * why this split from the old tabbed home screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterScreen(
    isAdmin: Boolean,
    onViewHistory: () -> Unit,
    onReadingClick: (String) -> Unit,
    onViewConsumption: () -> Unit,
    onOpenWaterReport: () -> Unit,
    onAddReading: () -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Water") }) },
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = onAddReading,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "Add meter reading",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            WaterDashboardScreen(
                onViewHistory = onViewHistory,
                onReadingClick = onReadingClick,
                onViewConsumption = onViewConsumption,
                onOpenReport = onOpenWaterReport
            )
        }
    }
}
