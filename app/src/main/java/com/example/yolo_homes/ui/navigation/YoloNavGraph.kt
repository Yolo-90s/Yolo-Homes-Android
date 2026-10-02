package com.example.yolo_homes.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.yolo_homes.data.model.UserSession
import com.example.yolo_homes.feature.flats.FlatDetailScreen
import com.example.yolo_homes.feature.flats.FlatDirectoryScreen
import com.example.yolo_homes.feature.home.HomeScreen
import com.example.yolo_homes.feature.maintenance.AddMaintenanceScreen
import com.example.yolo_homes.feature.maintenance.MaintenanceHistoryScreen
import com.example.yolo_homes.feature.maintenance.MaintenanceScreen
import com.example.yolo_homes.feature.maintenance.ReceiptDetailScreen
import com.example.yolo_homes.feature.profile.ProfileScreen
import com.example.yolo_homes.feature.reports.ReportsScreen
import com.example.yolo_homes.feature.settings.SettingsScreen
import com.example.yolo_homes.feature.water.AddReadingScreen
import com.example.yolo_homes.feature.water.BulkAddReadingScreen
import com.example.yolo_homes.feature.water.FlatConsumptionScreen
import com.example.yolo_homes.feature.water.WaterBillScreen
import com.example.yolo_homes.feature.water.WaterHistoryScreen
import com.example.yolo_homes.feature.water.WaterReportScreen
import com.example.yolo_homes.feature.water.WaterScreen
import com.example.yolo_homes.ui.components.AccessRestrictedScreen

/**
 * Signed-in navigation. A persistent labeled bottom bar (Home / Maintenance
 * / Water / Reports / Profile — [YoloBottomBar]) hosts the 5 top-level
 * destinations; everything else (Add/History/Detail/Settings/etc.) is a
 * full screen pushed on top with its own top bar + back, same as before.
 */
@Composable
fun YoloNavGraph(
    session: UserSession,
    appVersion: String,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { YoloBottomBar(navController, session) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.DASHBOARD) {
                HomeScreen(
                    session = session,
                    onOpenMaintenance = { navController.navigate(Routes.MAINTENANCE) },
                    onOpenWater = { navController.navigate(Routes.WATER) },
                    onOpenReports = { navController.navigate(Routes.REPORTS) },
                    onAddMaintenance = { navController.navigate(Routes.ADD_MAINTENANCE) },
                    onAddReading = { navController.navigate(Routes.ADD_READING) },
                    onOpenFlatDirectory = { navController.navigate(Routes.FLAT_DIRECTORY) },
                    onOpenMyFlat = { navController.navigate(Routes.flatDetail(it)) }
                )
            }

            composable(Routes.MAINTENANCE) {
                // Guests see the Community Board only — no Maintenance access.
                if (session.isGuest) {
                    AccessRestrictedScreen(
                        title = "Maintenance",
                        onBack = { navController.navigate(Routes.DASHBOARD) },
                        message = "Ask an admin to link your account to your flat to see this."
                    )
                } else {
                    MaintenanceScreen(
                        isAdmin = session.isAdmin,
                        onViewHistory = { navController.navigate(Routes.MAINTENANCE_HISTORY) },
                        onReceiptClick = { navController.navigate(Routes.receiptDetail(it)) },
                        onAddMaintenance = { navController.navigate(Routes.ADD_MAINTENANCE) },
                        onOpenFlatDirectory = { navController.navigate(Routes.FLAT_DIRECTORY) }
                    )
                }
            }

            composable(Routes.WATER) {
                if (session.isGuest) {
                    AccessRestrictedScreen(
                        title = "Water",
                        onBack = { navController.navigate(Routes.DASHBOARD) },
                        message = "Ask an admin to link your account to your flat to see this."
                    )
                } else {
                    WaterScreen(
                        isAdmin = session.isAdmin,
                        onViewHistory = { navController.navigate(Routes.READING_HISTORY) },
                        onReadingClick = { navController.navigate(Routes.waterBill(it)) },
                        onViewConsumption = { navController.navigate(Routes.FLAT_CONSUMPTION) },
                        onOpenWaterReport = { navController.navigate(Routes.WATER_REPORT) },
                        onAddReading = { navController.navigate(Routes.ADD_READING) },
                        onBulkAddReading = { navController.navigate(Routes.BULK_ADD_READING) },
                        onOpenFlatDirectory = { navController.navigate(Routes.FLAT_DIRECTORY) }
                    )
                }
            }

            composable(Routes.FLAT_DIRECTORY) {
                FlatDirectoryScreen(
                    onBack = { navController.popBackStack() },
                    onOpenFlat = { navController.navigate(Routes.flatDetail(it)) }
                )
            }
            composable(
                Routes.FLAT_DETAIL,
                arguments = listOf(navArgument("flatId") { type = NavType.StringType })
            ) {
                FlatDetailScreen(
                    onBack = { navController.popBackStack() },
                    onReceiptClick = { navController.navigate(Routes.receiptDetail(it)) },
                    onReadingClick = { navController.navigate(Routes.waterBill(it)) }
                )
            }

            composable(Routes.REPORTS) {
                // Building-wide reports are Admin/Developer only — not part of a Member's
                // "own flat data" scope.
                if (!session.isAdmin) {
                    AccessRestrictedScreen(
                        title = "Reports",
                        onBack = { navController.navigate(Routes.DASHBOARD) }
                    )
                } else {
                    ReportsScreen()
                }
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    session = session,
                    appVersion = appVersion,
                    onLogout = onLogout
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    isAdmin = session.isAdmin,
                    canManageFlats = session.canManageFlats,
                    onBack = { navController.popBackStack() },
                    onOpenFlatDirectory = { navController.navigate(Routes.FLAT_DIRECTORY) }
                )
            }

            // ---- Maintenance ----
            composable(Routes.ADD_MAINTENANCE) {
                // Capturing a payment is Admin/Developer only — guard the route directly since
                // it's a write, not just a view (defense-in-depth beyond hiding the FAB/button).
                if (!session.isAdmin) {
                    AccessRestrictedScreen(title = "Add Maintenance", onBack = { navController.popBackStack() })
                } else {
                    AddMaintenanceScreen(onBack = { navController.popBackStack() }, onSaved = { navController.popBackStack() })
                }
            }
            composable(Routes.MAINTENANCE_HISTORY) {
                MaintenanceHistoryScreen(
                    onBack = { navController.popBackStack() },
                    onReceiptClick = { navController.navigate(Routes.receiptDetail(it)) }
                )
            }
            composable(
                Routes.RECEIPT_DETAIL,
                arguments = listOf(navArgument("receiptId") { type = NavType.StringType })
            ) { entry ->
                ReceiptDetailScreen(
                    receiptId = entry.arguments?.getString("receiptId").orEmpty(),
                    onBack = { navController.popBackStack() }
                )
            }

            // ---- Water ----
            composable(Routes.ADD_READING) {
                if (!session.isAdmin) {
                    AccessRestrictedScreen(title = "Add Reading", onBack = { navController.popBackStack() })
                } else {
                    AddReadingScreen(onBack = { navController.popBackStack() }, onSaved = { navController.popBackStack() })
                }
            }
            composable(Routes.BULK_ADD_READING) {
                if (!session.isAdmin) {
                    AccessRestrictedScreen(title = "Bulk Upload Readings", onBack = { navController.popBackStack() })
                } else {
                    BulkAddReadingScreen(onBack = { navController.popBackStack() }, onSaved = { navController.popBackStack() })
                }
            }
            composable(Routes.READING_HISTORY) {
                WaterHistoryScreen(
                    onBack = { navController.popBackStack() },
                    onReadingClick = { navController.navigate(Routes.waterBill(it)) }
                )
            }
            composable(
                Routes.WATER_BILL,
                arguments = listOf(navArgument("readingId") { type = NavType.StringType })
            ) { entry ->
                WaterBillScreen(
                    readingId = entry.arguments?.getString("readingId").orEmpty(),
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.FLAT_CONSUMPTION) {
                FlatConsumptionScreen(
                    onBack = { navController.popBackStack() },
                    onOpenReport = { navController.navigate(Routes.WATER_REPORT) }
                )
            }
            composable(Routes.WATER_REPORT) {
                WaterReportScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
