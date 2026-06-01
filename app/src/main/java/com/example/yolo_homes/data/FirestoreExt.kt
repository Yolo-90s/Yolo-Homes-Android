package com.example.yolo_homes.data

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Emits the deserialized list every time the query result changes (snapshot listener),
 * giving us offline-first single-source-of-truth streams.
 */
inline fun <reified T : Any> Query.asFlow(): Flow<List<T>> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            close(error)
            return@addSnapshotListener
        }
        if (snapshot != null) {
            trySend(snapshot.toObjects(T::class.java))
        }
    }
    awaitClose { registration.remove() }
}

/** Snapshot-listener stream that maps each document with [mapper] (tolerant deserialization). */
fun <T : Any> Query.asFlow(mapper: (DocumentSnapshot) -> T): Flow<List<T>> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            close(error)
            return@addSnapshotListener
        }
        if (snapshot != null) {
            trySend(snapshot.documents.map(mapper))
        }
    }
    awaitClose { registration.remove() }
}

/** Document snapshot stream mapped with [mapper] (tolerant deserialization). */
fun <T> DocumentReference.asFlow(mapper: (DocumentSnapshot) -> T): Flow<T> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            close(error)
            return@addSnapshotListener
        }
        if (snapshot != null) {
            trySend(mapper(snapshot))
        }
    }
    awaitClose { registration.remove() }
}

inline fun <reified T : Any> DocumentReference.asFlow(): Flow<T?> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            close(error)
            return@addSnapshotListener
        }
        trySend(snapshot?.toObject(T::class.java))
    }
    awaitClose { registration.remove() }
}
