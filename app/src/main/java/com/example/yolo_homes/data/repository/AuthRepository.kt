package com.example.yolo_homes.data.repository

import com.example.yolo_homes.core.FirestoreCollections
import com.example.yolo_homes.data.SessionManager
import com.example.yolo_homes.data.model.AppUser
import com.example.yolo_homes.data.model.Role
import com.example.yolo_homes.data.model.UserSession
import com.example.yolo_homes.data.strOrNull
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val sessionManager: SessionManager
) {
    val currentUid: String? get() = auth.currentUser?.uid
    val isSignedIn: Boolean get() = auth.currentUser != null

    /**
     * Emits the resolved session whenever auth state changes (login, logout, token refresh).
     * Null means signed-out.
     */
    fun observeSession(): Flow<UserSession?> = channelFlow {
        val listener = FirebaseAuth.AuthStateListener {
            // Resolve the session off the listener thread for each auth change.
            launch { trySend(resolveSession()) }
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    private suspend fun resolveSession(): UserSession? {
        val fbUser = auth.currentUser
        if (fbUser == null) {
            sessionManager.update(null)
            return null
        }
        val name = fbUser.displayName ?: fbUser.email?.substringBefore("@") ?: "User"
        // Firestore reads here may fail (permission/network) — never let that crash sign-in.
        val profile = try {
            ensureUserProfile(uid = fbUser.uid, displayName = name, email = fbUser.email ?: "")
        } catch (e: Exception) {
            AppUser(
                id = fbUser.uid, uid = fbUser.uid, displayName = name,
                email = fbUser.email ?: "", initials = AppUser.deriveInitials(name),
                avatarColor = pickAvatarColor(fbUser.uid)
            )
        }
        val resolution = try {
            resolveRoleAndFlat(email = fbUser.email, phone = fbUser.phoneNumber)
        } catch (e: Exception) {
            RoleResolution(Role.UNKNOWN, null)
        }
        // Mirror the role onto the user profile so Firestore rules can enforce writes.
        if (resolution.role != Role.UNKNOWN) {
            runCatching {
                db.collection(FirestoreCollections.USERS).document(fbUser.uid)
                    .set(mapOf("role" to resolution.role.name.lowercase()), SetOptions.merge()).await()
            }
        }
        val session = UserSession(user = profile, role = resolution.role, flatId = resolution.flatId)
        sessionManager.update(session)
        return session
    }

    private data class RoleResolution(val role: Role, val flatId: String?)

    /**
     * Resolves the user's role and own-flat by matching their email (case-insensitive) — then
     * owner/tenant phone as a fallback — against `masterFlats`. The matched flat's `role` field
     * drives access; the corresponding `flats` doc id is returned to scope resident data.
     */
    private suspend fun resolveRoleAndFlat(email: String?, phone: String?): RoleResolution {
        if (email == null && phone == null) return RoleResolution(Role.UNKNOWN, null)
        val flats = db.collection(FirestoreCollections.MASTER_FLATS).get().await().documents
        val match = flats.firstOrNull { doc ->
            val docEmail = doc.get("email")?.toString()
            val ownerPhone = doc.get("ownerPhone")?.toString()
            val tenantPhone = doc.get("tenantPhone")?.toString()
            (email != null && docEmail != null && docEmail.equals(email, ignoreCase = true)) ||
                (phone != null && (phone == ownerPhone || phone == tenantPhone))
        } ?: return RoleResolution(Role.UNKNOWN, null)

        val role = Role.from(match.get("role")?.toString())
        val flatNo = match.get("flatNo")?.toString() ?: match.id
        // Map the flat number to the operational `flats` collection doc id used by receipts/readings.
        val scopeId = runCatching {
            db.collection(FirestoreCollections.FLATS)
                .whereEqualTo("flatNumber", flatNo).limit(1)
                .get().await().documents.firstOrNull()?.id
        }.getOrNull()
        return RoleResolution(role, scopeId)
    }

    /** Signs in to Firebase with a Google ID token obtained via Credential Manager. */
    suspend fun signInWithGoogle(idToken: String): Result<Unit> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).await()
        Unit
    }

    fun signOut() = auth.signOut()

    /**
     * Creates or refreshes the `users/{uid}` document, returning the profile.
     * Reads fields manually (not via toObject) so legacy/extra fields and
     * Long-vs-Timestamp `createdAt` values don't break deserialization.
     */
    private suspend fun ensureUserProfile(uid: String, displayName: String, email: String): AppUser {
        val ref = db.collection(FirestoreCollections.USERS).document(uid)
        val snap = ref.get().await()
        if (snap.exists()) {
            // Read every field defensively — the legacy doc may store, e.g., avatarColor
            // as a packed numeric Compose color rather than a hex String.
            val storedColor = snap.strOrNull("avatarColor")
            return AppUser(
                id = uid,
                uid = snap.strOrNull("uid") ?: uid,
                displayName = snap.strOrNull("displayName") ?: displayName,
                email = snap.strOrNull("email") ?: email,
                initials = snap.strOrNull("initials") ?: AppUser.deriveInitials(displayName),
                avatarColor = storedColor?.takeIf { it.startsWith("#") } ?: pickAvatarColor(uid),
                createdAt = readMillis(snap.get("createdAt"))
            )
        }

        val initials = AppUser.deriveInitials(displayName)
        val now = System.currentTimeMillis()
        val data = mapOf(
            "uid" to uid,
            "displayName" to displayName,
            "email" to email,
            "initials" to initials,
            "avatarColor" to pickAvatarColor(uid),
            "createdAt" to now
        )
        ref.set(data, SetOptions.merge()).await()
        return AppUser(
            id = uid, uid = uid, displayName = displayName,
            email = email, initials = initials, avatarColor = pickAvatarColor(uid), createdAt = now
        )
    }

    /** Tolerates `createdAt` stored as Long, Double, Firestore Timestamp or Date. */
    private fun readMillis(value: Any?): Long = when (value) {
        is Long -> value
        is Double -> value.toLong()
        is com.google.firebase.Timestamp -> value.toDate().time
        is java.util.Date -> value.time
        else -> 0L
    }

    private fun pickAvatarColor(seed: String): String {
        val palette = listOf("#2563EB", "#0EA5E9", "#14B8A6", "#F59E0B", "#8B5CF6", "#F43F5E")
        val idx = (seed.hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) }) % palette.size
        return palette[idx]
    }
}
