package com.example.yolo_homes.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.yolo_homes.core.Formatters
import com.example.yolo_homes.data.model.UserSession
import com.example.yolo_homes.feature.maintenance.MaintenanceViewModel
import com.example.yolo_homes.feature.water.WaterViewModel
import com.example.yolo_homes.ui.components.StatCard
import com.example.yolo_homes.ui.components.SurfaceCard
import com.example.yolo_homes.ui.theme.ChartSky
import com.example.yolo_homes.ui.theme.LightPrimary

/**
 * The real landing screen — what a non-technical resident sees first.
 * Answers "what do I do here" directly: a greeting, this month's numbers
 * at a glance, and large labeled shortcuts to everything else. Replaces
 * the old swipeable Maintenance/Water tabs (see plan notes in
 * YoloNavGraph.kt) — those are now their own bottom-nav destinations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    session: UserSession,
    onOpenMaintenance: () -> Unit,
    onOpenWater: () -> Unit,
    onOpenReports: () -> Unit,
    onAddMaintenance: () -> Unit,
    onAddReading: () -> Unit,
    maintenanceViewModel: MaintenanceViewModel = hiltViewModel(),
    waterViewModel: WaterViewModel = hiltViewModel()
) {
    val maintenanceState by maintenanceViewModel.uiState.collectAsStateWithLifecycle()
    val waterState by waterViewModel.uiState.collectAsStateWithLifecycle()
    val firstName = session.user.displayName.substringBefore(' ').ifBlank { "there" }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Yolo-Home's") }) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text("Hi, $firstName", style = MaterialTheme.typography.headlineSmall)
                Text(
                    if (session.isAdmin) "Here's what's happening in your building."
                    else "Here's your apartment overview.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    "Maintenance This Month", maintenanceState.currentMonthTotal,
                    Icons.Outlined.Payments, LightPrimary, Modifier.weight(1f)
                ) { Formatters.currency(it, maintenanceState.currency) }
                StatCard(
                    "Water This Month", waterState.totalConsumption,
                    Icons.Outlined.WaterDrop, ChartSky, Modifier.weight(1f)
                ) { Formatters.liters(it) }
            }

            HomeShortcutCard(
                title = "Maintenance",
                subtitle = "View payment history & receipts",
                icon = Icons.Outlined.Receipt,
                onClick = onOpenMaintenance
            )
            HomeShortcutCard(
                title = "Water",
                subtitle = "View usage, bills & meter readings",
                icon = Icons.Outlined.WaterDrop,
                onClick = onOpenWater
            )
            HomeShortcutCard(
                title = "Reports",
                subtitle = "Download monthly summaries as PDF",
                icon = Icons.Outlined.Assessment,
                onClick = onOpenReports
            )

            if (session.isAdmin) {
                Text(
                    "Quick add",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onAddMaintenance, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(" Payment")
                    }
                    OutlinedButton(onClick = onAddReading, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(" Reading")
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeShortcutCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    SurfaceCard(Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}
