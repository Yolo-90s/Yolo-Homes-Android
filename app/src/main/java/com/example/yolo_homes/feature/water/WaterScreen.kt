package com.example.yolo_homes.feature.water

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
    onAddReading: () -> Unit,
    onBulkAddReading: () -> Unit = {},
    onOpenFlatDirectory: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Water") },
                actions = {
                    IconButton(onClick = onOpenFlatDirectory) {
                        Icon(Icons.Outlined.Apartment, contentDescription = "Flat Directory")
                    }
                }
            )
        },
        floatingActionButton = {
            if (isAdmin) {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    SmallFloatingActionButton(onClick = onBulkAddReading) {
                        Icon(Icons.Outlined.UploadFile, contentDescription = "Bulk upload readings from a photo")
                    }
                    androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))
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
