package com.example.yolo_homes.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.yolo_homes.data.model.UserSession
import com.example.yolo_homes.feature.maintenance.MaintenanceHomeScreen
import com.example.yolo_homes.feature.water.WaterDashboardScreen
import com.example.yolo_homes.ui.components.InitialsAvatar
import kotlinx.coroutines.launch

/**
 * The signed-in home: a top app bar (with Reports / Settings / Profile actions) and two
 * swipeable tabs — Maintenance and Water — each showing only its own content. No bottom bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTabsScreen(
    session: UserSession,
    onOpenReports: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    onAddMaintenance: () -> Unit,
    onAddReading: () -> Unit,
    onMaintenanceHistory: () -> Unit,
    onReceiptClick: (String) -> Unit,
    onWaterHistory: () -> Unit,
    onReadingClick: (String) -> Unit,
    onViewConsumption: () -> Unit,
    onOpenWaterReport: () -> Unit
) {
    val tabs = listOf("Maintenance", "Water")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Yolo-Home's", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = onOpenReports) {
                        Icon(Icons.Outlined.Assessment, contentDescription = "Reports")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = onOpenProfile) {
                        InitialsAvatar(session.user.initials, session.user.avatarColor, size = 32.dp)
                    }
                }
            )
        },
        floatingActionButton = {
            // Only managers/developers can capture data; residents get a read-only view.
            if (session.isAdmin) {
                FloatingActionButton(
                    onClick = { if (pagerState.currentPage == 0) onAddMaintenance() else onAddReading() },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = if (pagerState.currentPage == 0) "Add maintenance" else "Add reading",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            androidx.compose.foundation.layout.Column {
                TabRow(selectedTabIndex = pagerState.currentPage) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            text = {
                                Text(
                                    title,
                                    fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        )
                    }
                }
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    when (page) {
                        0 -> MaintenanceHomeScreen(
                            onViewHistory = onMaintenanceHistory,
                            onReceiptClick = onReceiptClick
                        )
                        else -> WaterDashboardScreen(
                            onViewHistory = onWaterHistory,
                            onReadingClick = onReadingClick,
                            onViewConsumption = onViewConsumption,
                            onOpenReport = onOpenWaterReport
                        )
                    }
                }
            }
        }
    }
}
