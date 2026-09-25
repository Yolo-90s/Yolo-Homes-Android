package com.example.yolo_homes.data.model

import com.example.yolo_homes.core.PostTypes

/**
 * Maps to the `posts` collection — the Home screen's "Community Board".
 * Independent of Maintenance/Water: any admin-posted news, event, or notice.
 */
data class Post(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = PostTypes.NOTICE,
    /** Only meaningful when [type] == "event". Null/0 means no date set. */
    val eventDateMillis: Long? = null,
    val pinned: Boolean = false,
    val createdByUid: String = "",
    val createdByName: String = "",
    val createdAtMillis: Long = 0L
) {
    val typeLabel: String
        get() = when (type) {
            PostTypes.NEWS -> "News"
            PostTypes.EVENT -> "Event"
            else -> "Notice"
        }
}
