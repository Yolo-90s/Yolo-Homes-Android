package com.example.yolo_homes.core

/** Firestore collection paths. These mirror the existing schema — do not rename. */
object FirestoreCollections {
    const val APP_SETTINGS = "appSettings"
    const val APP_SETTINGS_DOC = "main"
    const val MASTER_FLATS = "masterFlats"
    // The prior app's operational flats collection (auto-IDs, `flatNumber` field).
    // maintenanceReceipts/readings reference THESE document ids.
    const val FLATS = "flats"
    const val MAINTENANCE_RECEIPTS = "maintenanceReceipts"
    const val READINGS = "readings"
    const val USERS = "users"
}

object Roles {
    const val ADMIN = "admin"
    const val RESIDENT = "resident"
}

object PaymentMethods {
    val ALL = listOf("Cash", "UPI", "Bank Transfer", "Cheque", "Card")
}
