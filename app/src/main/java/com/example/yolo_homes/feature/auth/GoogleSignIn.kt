package com.example.yolo_homes.feature.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Requests a Google ID token via the Credential Manager API (the modern replacement
 * for the deprecated GoogleSignInClient). Returns the raw ID token to exchange with Firebase.
 *
 * Requires a valid `default_web_client_id` (Web client ID from the Firebase console).
 */
suspend fun requestGoogleIdToken(context: Context, serverClientId: String): Result<String> = runCatching {
    val option = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(serverClientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(option)
        .build()

    val response = CredentialManager.create(context).getCredential(context, request)
    val credential = response.credential
    val googleIdCredential = GoogleIdTokenCredential.createFrom(credential.data)
    googleIdCredential.idToken
}
