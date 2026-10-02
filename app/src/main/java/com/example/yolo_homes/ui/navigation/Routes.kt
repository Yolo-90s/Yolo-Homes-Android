package com.example.yolo_homes.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.yolo_homes.data.model.UserSession

/** All navigation destinations as string routes. */
object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"

    const val DASHBOARD = "dashboard"
    const val MAINTENANCE = "maintenance"
    const val WATER = "water"
    const val REPORTS = "reports"
    const val PROFILE = "profile"

    const val ADD_MAINTENANCE = "maintenance/add"
    const val MAINTENANCE_HISTORY = "maintenance/history"
    const val RECEIPT_DETAIL = "maintenance/receipt/{receiptId}"
    fun receiptDetail(id: String) = "maintenance/receipt/$id"

    const val ADD_READING = "water/add"
    const val BULK_ADD_READING = "water/bulk-add"
    const val READING_HISTORY = "water/history"
    const val FLAT_CONSUMPTION = "water/consumption"
    const val WATER_REPORT = "water/report"
    const val WATER_BILL = "water/bill/{readingId}"
    fun waterBill(id: String) = "water/bill/$id"

    const val SETTINGS = "settings"

    const val FLAT_DIRECTORY = "flats"
    const val FLAT_DETAIL = "flats/{flatId}"
    fun flatDetail(id: String) = "flats/$id"
}

/** The five bottom-navigation tabs. */
enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    DASHBOARD(Routes.DASHBOARD, "Home", Icons.Outlined.Dashboard),
    MAINTENANCE(Routes.MAINTENANCE, "Maintenance", Icons.Outlined.Receipt),
    WATER(Routes.WATER, "Water", Icons.Outlined.WaterDrop),
    REPORTS(Routes.REPORTS, "Reports", Icons.Outlined.Assessment),
    PROFILE(Routes.PROFILE, "Profile", Icons.Outlined.Person);

    /**
     * A Guest sees Home + Profile only (Community Board is enough — everything else is
     * flat-scoped or building-wide data they have no access to). A Member additionally sees
     * Maintenance + Water (their own flat) but not Reports (building-wide, Admin/Developer only).
     */
    fun isVisibleFor(session: UserSession): Boolean = when (this) {
        DASHBOARD, PROFILE -> true
        MAINTENANCE, WATER -> !session.isGuest
        REPORTS -> session.isAdmin
    }

    companion object {
        val routes = entries.map { it.route }.toSet()
    }
}
