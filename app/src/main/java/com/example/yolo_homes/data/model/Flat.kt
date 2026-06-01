package com.example.yolo_homes.data.model

import com.google.firebase.firestore.DocumentId

/**
 * Maps to the `masterFlats` collection. `role` drives admin vs resident access.
 */
data class Flat(
    @DocumentId val id: String = "",
    val flatNo: String = "",
    val block: String = "",
    val ownerName: String = "",
    val ownerPhone: String = "",
    val tenantName: String = "",
    val tenantPhone: String = "",
    val role: String = "resident",
    /** Google sign-in email linked to this flat (set via Manage Residents). */
    val email: String = ""
) {
    val displayName: String
        get() = if (block.isNotBlank()) "$block-$flatNo" else flatNo

    val occupantName: String
        get() = tenantName.ifBlank { ownerName }

    companion object {
        /**
         * Builds a lookup keyed by BOTH the document id and the flatNo, so a reading/receipt
         * whose `flatId` stores either form still resolves to its flat. Document id wins on clash.
         */
        fun lookup(flats: List<Flat>): Map<String, Flat> {
            val byNo = flats.filter { it.flatNo.isNotBlank() }.associateBy { it.flatNo }
            val byId = flats.filter { it.id.isNotBlank() }.associateBy { it.id }
            return byNo + byId
        }
    }
}
