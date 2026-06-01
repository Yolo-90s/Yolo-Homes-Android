package com.example.yolo_homes.data.model

/**
 * The authenticated user plus their resolved access level and (for residents) their own flat.
 * Role is derived by matching the user's email to a flat in `masterFlats` (see AuthRepository).
 */
data class UserSession(
    val user: AppUser,
    val role: Role,
    /** The `flats`-collection document id of this user's flat, used to scope resident data. */
    val flatId: String? = null
) {
    val isAdmin: Boolean get() = role.canWrite
    val isResident: Boolean get() = role.isResident
}
