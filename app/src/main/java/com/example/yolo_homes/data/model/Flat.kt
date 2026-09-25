package com.example.yolo_homes.data.model

import com.example.yolo_homes.core.UnitTypes
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
    val role: String = "member",
    /** Google sign-in email linked to this flat (set via the Flat Directory). */
    val email: String = "",
    /** "resident" (default) or "staff" — e.g. the gatehouse/watchman unit (G01). */
    val unitType: String = UnitTypes.RESIDENT,
    /** True when no one currently lives here (between owners/tenants). */
    val vacant: Boolean = false
) {
    val isStaff: Boolean get() = unitType == UnitTypes.STAFF

    val displayName: String
        get() = if (block.isNotBlank()) "$block-$flatNo" else flatNo

    val occupantName: String
        get() = if (vacant) "Vacant" else tenantName.ifBlank { ownerName }

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
