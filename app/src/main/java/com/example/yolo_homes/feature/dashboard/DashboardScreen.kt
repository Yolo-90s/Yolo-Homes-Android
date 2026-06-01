package com.example.yolo_homes.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.yolo_homes.core.Formatters
import com.example.yolo_homes.data.model.UserSession
import com.example.yolo_homes.ui.components.InitialsAvatar
import com.example.yolo_homes.ui.components.SectionHeader
import com.example.yolo_homes.ui.navigation.Routes
import com.example.yolo_homes.ui.theme.ChartGreen
import com.example.yolo_homes.ui.theme.ChartSky
import com.example.yolo_homes.ui.theme.LightAccent
import com.example.yolo_homes.ui.theme.LightPrimary

/**
 * Home hub: greeting, then the two primary modules — Maintenance and Water — as large
 * tappable cards, followed by compact quick actions. Each module screen shows only its
 * own content.
 */
@Composable
fun DashboardScreen(
    session: UserSession,
    onNavigate: (String) -> Unit,
    onAddMaintenance: () -> Unit,
    onAddReading: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { GreetingHeader(session, state.apartmentName) }

        item {
            ModuleCard(
                title = "Maintenance",
                caption = "This month collection",
                value = Formatters.currency(state.maintenanceCollection, state.currency),
                footnote = "${state.totalFlats} flats",
                icon = Icons.Rounded.ReceiptLong,
                gradient = listOf(LightPrimary, ChartGreen),
                onClick = { onNavigate(Routes.MAINTENANCE) }
            )
        }
        item {
            ModuleCard(
                title = "Water",
                caption = "This month consumption",
                value = Formatters.liters(state.waterConsumption),
                footnote = "Revenue ${Formatters.currency(state.waterRevenue, state.currency)}",
                icon = Icons.Rounded.WaterDrop,
                gradient = listOf(ChartSky, LightAccent),
                onClick = { onNavigate(Routes.WATER) }
            )
        }

        item { SectionHeader("Quick Actions") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickAction("Add Maintenance", Icons.Outlined.Receipt, LightPrimary, Modifier.weight(1f), onAddMaintenance)
                QuickAction("Add Reading", Icons.Outlined.WaterDrop, ChartSky, Modifier.weight(1f), onAddReading)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickAction("Reports", Icons.Outlined.Assessment, ChartGreen, Modifier.weight(1f)) { onNavigate(Routes.REPORTS) }
                QuickAction("Settings", Icons.Outlined.Settings, LightAccent, Modifier.weight(1f)) { onNavigate(Routes.SETTINGS) }
            }
        }
    }
}

@Composable
private fun GreetingHeader(session: UserSession, apartmentName: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "${Formatters.greeting()},",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(session.user.displayName, style = MaterialTheme.typography.headlineSmall)
            Text(
                apartmentName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Notifications, contentDescription = "Notifications")
            }
            InitialsAvatar(session.user.initials, session.user.avatarColor)
        }
    }
}

@Composable
private fun ModuleCard(
    title: String,
    caption: String,
    value: String,
    footnote: String,
    icon: ImageVector,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(gradient))
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 14.dp)
                    )
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = "Open $title",
                        tint = Color.White
                    )
                }
                Text(
                    caption,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(top = 16.dp)
                )
                Text(
                    value,
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    footnote,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Text(label, style = MaterialTheme.typography.titleSmall)
        }
    }
}
