package com.example.yolo_homes.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.yolo_homes.data.model.UserSession
import com.example.yolo_homes.feature.maintenance.AddMaintenanceScreen
import com.example.yolo_homes.feature.maintenance.MaintenanceHistoryScreen
import com.example.yolo_homes.feature.maintenance.ReceiptDetailScreen
import com.example.yolo_homes.feature.profile.ProfileScreen
import com.example.yolo_homes.feature.reports.ReportsScreen
import com.example.yolo_homes.feature.residents.ManageResidentsScreen
import com.example.yolo_homes.feature.settings.SettingsScreen
import com.example.yolo_homes.feature.water.AddReadingScreen
import com.example.yolo_homes.feature.water.FlatConsumptionScreen
import com.example.yolo_homes.feature.water.WaterBillScreen
import com.example.yolo_homes.feature.water.WaterHistoryScreen

/**
 * Signed-in navigation. The start is a tabbed home (Maintenance | Water) with no bottom bar;
 * everything else is a full screen pushed on top with its own top bar + back.
 */
@Composable
fun YoloNavGraph(
    session: UserSession,
    appVersion: String,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.DASHBOARD) {
        composable(Routes.DASHBOARD) {
            HomeTabsScreen(
                session = session,
                onOpenReports = { navController.navigate(Routes.REPORTS) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenProfile = { navController.navigate(Routes.PROFILE) },
                onAddMaintenance = { navController.navigate(Routes.ADD_MAINTENANCE) },
                onAddReading = { navController.navigate(Routes.ADD_READING) },
                onMaintenanceHistory = { navController.navigate(Routes.MAINTENANCE_HISTORY) },
                onReceiptClick = { navController.navigate(Routes.receiptDetail(it)) },
                onWaterHistory = { navController.navigate(Routes.READING_HISTORY) },
                onReadingClick = { navController.navigate(Routes.waterBill(it)) },
                onViewConsumption = { navController.navigate(Routes.FLAT_CONSUMPTION) }
            )
        }

        composable(Routes.REPORTS) {
            ReportsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.PROFILE) {
            ProfileScreen(
                session = session,
                appVersion = appVersion,
                onBack = { navController.popBackStack() },
                onLogout = onLogout
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                isAdmin = session.isAdmin,
                onBack = { navController.popBackStack() },
                onManageResidents = { navController.navigate(Routes.MANAGE_RESIDENTS) }
            )
        }
        composable(Routes.MANAGE_RESIDENTS) {
            ManageResidentsScreen(onBack = { navController.popBackStack() })
        }

        // ---- Maintenance ----
        composable(Routes.ADD_MAINTENANCE) {
            AddMaintenanceScreen(onBack = { navController.popBackStack() }, onSaved = { navController.popBackStack() })
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
            AddReadingScreen(onBack = { navController.popBackStack() }, onSaved = { navController.popBackStack() })
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
            FlatConsumptionScreen(onBack = { navController.popBackStack() })
        }
    }
}
