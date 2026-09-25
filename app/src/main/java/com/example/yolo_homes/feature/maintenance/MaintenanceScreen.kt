package com.example.yolo_homes.feature.maintenance

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
 * Top-level "Maintenance" bottom-nav destination. Thin wrapper around the
 * existing [MaintenanceHomeScreen] content — just adds its own labeled top
 * bar and an unambiguous FAB (previously this same "+" button lived on
 * the old tabbed home screen and silently meant "add maintenance" or "add
 * reading" depending on which invisible tab was selected).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceScreen(
    isAdmin: Boolean,
    onViewHistory: () -> Unit,
    onReceiptClick: (String) -> Unit,
    onAddMaintenance: () -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Maintenance") }) },
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = onAddMaintenance,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "Add payment record",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            MaintenanceHomeScreen(
                onViewHistory = onViewHistory,
                onReceiptClick = onReceiptClick
            )
        }
    }
}
