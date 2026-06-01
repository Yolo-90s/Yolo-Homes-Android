package com.example.yolo_homes.data.model

import com.google.firebase.firestore.DocumentId

/**
 * Maps to the `users` collection. `createdAt` is stored as epoch millis (Long) to match
 * the existing data; deserialization is done manually in AuthRepository to tolerate
 * legacy fields and timestamp/long differences.
 */
data class AppUser(
    @DocumentId val id: String = "",
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val initials: String = "",
    val avatarColor: String = "#2563EB",
    val createdAt: Long = 0L
) {
    companion object {
        fun deriveInitials(name: String): String {
            val parts = name.trim().split(" ").filter { it.isNotBlank() }
            return when {
                parts.isEmpty() -> "?"
                parts.size == 1 -> parts[0].take(2).uppercase()
                else -> "${parts.first().first()}${parts.last().first()}".uppercase()
            }
        }
    }
}
