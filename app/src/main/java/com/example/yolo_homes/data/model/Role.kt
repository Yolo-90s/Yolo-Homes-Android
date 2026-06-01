package com.example.yolo_homes.data.model

/**
 * Access levels for Yolo-Home's.
 *  - DEVELOPER: super-admin (app config, manage admins) — highest.
 *  - ADMIN:     manager/committee — capture & edit data, edit settings, all flats.
 *  - OWNER:     flat owner — read-only, scoped to their own flat.
 *  - TENANT:    renter — read-only, scoped to their own flat.
 *  - UNKNOWN:   signed in but not matched to a flat — read-only, community view.
 */
enum class Role {
    DEVELOPER, ADMIN, OWNER, TENANT, UNKNOWN;

    /** May capture/edit receipts & readings. */
    val canWrite: Boolean get() = this == DEVELOPER || this == ADMIN

    /** May edit apartment settings (rate, free-limit, reminders). */
    val canEditSettings: Boolean get() = this == DEVELOPER || this == ADMIN

    /** A resident whose data should be scoped to their own flat. */
    val isResident: Boolean get() = this == OWNER || this == TENANT

    /** Higher wins when a user is linked to several flats with different roles. */
    val priority: Int
        get() = when (this) {
            DEVELOPER -> 4
            ADMIN -> 3
            OWNER -> 2
            TENANT -> 1
            UNKNOWN -> 0
        }

    /** Label shown in the UI. */
    val label: String
        get() = when (this) {
            DEVELOPER -> "Developer"
            ADMIN -> "Administrator"
            OWNER -> "Owner"
            TENANT -> "Tenant"
            UNKNOWN -> "Resident"
        }

    companion object {
        fun from(value: String?): Role = when (value?.trim()?.lowercase()) {
            "developer" -> DEVELOPER
            "admin" -> ADMIN
            "owner" -> OWNER
            "tenant" -> TENANT
            else -> UNKNOWN
        }
    }
}
