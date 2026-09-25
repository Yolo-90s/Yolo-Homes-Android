package com.example.yolo_homes.data.repository

import com.example.yolo_homes.core.FirestoreCollections
import com.example.yolo_homes.data.asFlow
import com.example.yolo_homes.data.model.Post
import com.example.yolo_homes.data.toPost
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** The Home screen's "Community Board" — news/events/notices, independent of Maintenance/Water. */
@Singleton
class PostRepository @Inject constructor(
    private val db: FirebaseFirestore
) {
    private val collection get() = db.collection(FirestoreCollections.POSTS)

    /** Pinned posts first, then newest first. */
    fun observePosts(): Flow<List<Post>> =
        collection.orderBy("createdAt", Query.Direction.DESCENDING)
            .asFlow { it.toPost() }
            .map { posts -> posts.sortedByDescending { it.pinned } }
            .catch { emit(emptyList()) }

    suspend fun createPost(
        title: String,
        body: String,
        type: String,
        eventDateMillis: Long?,
        pinned: Boolean,
        createdByUid: String,
        createdByName: String
    ) {
        val data = mapOf(
            "title" to title,
            "body" to body,
            "type" to type,
            "eventDate" to eventDateMillis,
            "pinned" to pinned,
            "createdByUid" to createdByUid,
            "createdByName" to createdByName,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        collection.add(data).await()
    }

    suspend fun deletePost(id: String) {
        collection.document(id).delete().await()
    }
}
