package com.example.yolo_homes.data.model

/**
 * Access levels for Yolo-Home's. Exactly four — no other value is ever assigned or stored.
 *  - DEVELOPER: super-admin — sees and can add/edit/update everything, including flat
 *               directory details (owner/tenant/staff records). The only role that can.
 *  - ADMIN:     manager/committee — sees everything and can capture maintenance/water data
 *               and edit settings, but may NOT add/edit flat directory details.
 *  - MEMBER:    a resident (owner or tenant) — read-only, scoped to their own flat: the
 *               Community Board plus their own Maintenance and Water data only.
 *  - GUEST:     signed in but not linked to any flat — the default for anyone not yet
 *               assigned. Sees the Community Board only.
 */
enum class Role {
    DEVELOPER, ADMIN, MEMBER, GUEST;

    /** May capture/edit maintenance receipts & water readings, and edit apartment settings. */
    val canWrite: Boolean get() = this == DEVELOPER || this == ADMIN

    /** May add/edit flat directory details (owner/tenant/staff records). Developer-only. */
    val canManageFlats: Boolean get() = this == DEVELOPER

    /** A resident whose data is scoped to their own flat. */
    val isMember: Boolean get() = this == MEMBER

    /** Unassigned — sees the Community Board only. */
    val isGuest: Boolean get() = this == GUEST

    /** Higher wins when a user is linked to several flats with different roles. */
    val priority: Int
        get() = when (this) {
            DEVELOPER -> 4
            ADMIN -> 3
            MEMBER -> 2
            GUEST -> 1
        }

    /** Label shown in the UI. */
    val label: String
        get() = when (this) {
            DEVELOPER -> "Developer"
            ADMIN -> "Administrator"
            MEMBER -> "Member"
            GUEST -> "Guest"
        }

    companion object {
        /** "owner"/"tenant" map to MEMBER for backward compatibility with pre-existing data. */
        fun from(value: String?): Role = when (value?.trim()?.lowercase()) {
            "developer" -> DEVELOPER
            "admin" -> ADMIN
            "member", "owner", "tenant" -> MEMBER
            else -> GUEST
        }
    }
}
