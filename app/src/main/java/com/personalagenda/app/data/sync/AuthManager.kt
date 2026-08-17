package com.personalagenda.app.data.sync

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Google-account authentication for sync (Phase 2). Uses Credential Manager's
 * "Sign in with Google" to obtain an ID token, then signs into Firebase Auth.
 *
 * The Google Web Client ID is read at runtime from the `default_web_client_id`
 * string resource, which the google-services plugin generates **only once the
 * Google sign-in provider is enabled in the Firebase console and a fresh
 * google-services.json is downloaded**. Until then [isConfigured] is false and
 * the UI shows a "not configured yet" state instead of failing.
 */
class AuthManager(private val appContext: Context) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _user = MutableStateFlow(auth.currentUser)
    /** The currently signed-in Firebase user, or null when signed out. */
    val user: StateFlow<FirebaseUser?> = _user.asStateFlow()

    init {
        auth.addAuthStateListener { _user.value = it.currentUser }
    }

    /** The Firebase UID to scope this user's synced data under, or null if signed out. */
    val uid: String? get() = _user.value?.uid

    /** Whether Google sign-in is set up (web client id present in google-services.json). */
    val isConfigured: Boolean get() = webClientId() != null

    private fun webClientId(): String? {
        val resId = appContext.resources.getIdentifier(
            "default_web_client_id", "string", appContext.packageName,
        )
        return if (resId != 0) appContext.getString(resId) else null
    }

    /**
     * Launches the Google account picker and signs into Firebase. Must be called
     * with an [Activity] context (Credential Manager shows UI). Returns a [Result]
     * so the caller can surface errors without crashing.
     */
    suspend fun signInWithGoogle(activity: Activity): Result<Unit> {
        val clientId = webClientId()
            ?: return Result.failure(IllegalStateException("Google sign-in is not configured yet."))
        return try {
            val option = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(true)
                .build()
            val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
            val response = CredentialManager.create(activity).getCredential(activity, request)
            val credential = response.credential
            if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                return Result.failure(IllegalStateException("Unexpected credential type: ${credential.type}"))
            }
            val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(firebaseCredential).await()
            _user.value = auth.currentUser
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Google sign-in failed", e)
            Result.failure(e)
        }
    }

    /** Signs out of Firebase and clears the cached credential selection. */
    suspend fun signOut() {
        auth.signOut()
        _user.value = null
        runCatching {
            CredentialManager.create(appContext).clearCredentialState(ClearCredentialStateRequest())
        }
    }

    private companion object {
        const val TAG = "AuthManager"
    }
}
