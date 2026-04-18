package com.prayerquest.app.firebase

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Manages Firebase Authentication with Google Sign-In.
 * Required for Prayer Groups — users need identity to share prayers across devices.
 */
class FirebaseAuthManager(context: Context) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _authState = MutableStateFlow<AuthState>(
        if (auth.currentUser != null) AuthState.SignedIn(auth.currentUser!!)
        else AuthState.SignedOut
    )
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val currentUser: FirebaseUser? get() = auth.currentUser
    val currentUserId: String? get() = auth.currentUser?.uid
    val isSignedIn: Boolean get() = auth.currentUser != null
    val displayName: String? get() = auth.currentUser?.displayName

    private val googleSignInClient: GoogleSignInClient?
    private val isAvailable: Boolean

    init {
        // Get the web client ID from google-services.json (auto-generated R value)
        val webClientId = getWebClientId(context)

        if (webClientId != null) {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build()

            googleSignInClient = GoogleSignIn.getClient(context, gso)
            isAvailable = true

            // Listen for auth state changes
            auth.addAuthStateListener { firebaseAuth ->
                val user = firebaseAuth.currentUser
                _authState.value = if (user != null) AuthState.SignedIn(user) else AuthState.SignedOut
            }
        } else {
            Log.w(TAG, "Google Sign-In not available: google-services.json not configured")
            googleSignInClient = null
            isAvailable = false
        }
    }

    /**
     * Returns the Google Sign-In intent to launch from an Activity.
     */
    fun getSignInIntent(): Intent? = googleSignInClient?.signInIntent

    /**
     * Handles the result from the Google Sign-In Activity.
     * Call this from your ActivityResultLauncher callback.
     *
     * Surfaces the ApiException statusCode and a human-readable hint so the
     * UI can show the user what's actually wrong. By far the most common
     * cause of failure on a fresh Firebase project is statusCode 10
     * (DEVELOPER_ERROR) — the SHA-1 fingerprint of the APK's signing key
     * hasn't been registered on the Firebase project for this package name.
     */
    suspend fun handleSignInResult(result: ActivityResult): Result<FirebaseUser> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
                ?: return Result.failure(Exception("No ID token received from Google"))

            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user
                ?: return Result.failure(Exception("Sign-in succeeded but no user returned"))

            Log.d(TAG, "Sign-in successful: ${user.displayName}")
            Result.success(user)
        } catch (e: ApiException) {
            val code = e.statusCode
            val hint = describeSignInStatusCode(code)
            Log.e(TAG, "Google Sign-In failed: statusCode=$code ($hint)", e)
            Result.failure(Exception("Google Sign-In failed (code $code): $hint"))
        } catch (e: Exception) {
            Log.e(TAG, "Firebase auth failed", e)
            Result.failure(Exception("Firebase auth failed: ${e.message ?: e.javaClass.simpleName}"))
        }
    }

    /**
     * Map Google Sign-In / Common Status Codes to user-actionable hints.
     * Numeric codes are from com.google.android.gms.common.api.CommonStatusCodes
     * and com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes.
     */
    private fun describeSignInStatusCode(code: Int): String = when (code) {
        10 -> "DEVELOPER_ERROR — this APK's SHA-1 isn't registered in the " +
            "Firebase project. Add the debug+release SHA-1s to the Firebase " +
            "console for com.prayerquest.app, re-download google-services.json, " +
            "and rebuild."
        12500 -> "SIGN_IN_FAILED — Google Play Services could not complete sign-in " +
            "(often SHA-1 / package-name mismatch or Play Services out of date)."
        12501 -> "SIGN_IN_CANCELLED — user dismissed the account picker."
        12502 -> "SIGN_IN_CURRENTLY_IN_PROGRESS — another sign-in is already running."
        7 -> "NETWORK_ERROR — check your internet connection."
        8 -> "INTERNAL_ERROR — Google Play Services hiccup; try again."
        16 -> "API_NOT_CONNECTED — Google Play Services is missing or out of date."
        17 -> "CANCELED — sign-in was cancelled."
        else -> "Unknown error."
    }

    /**
     * Sign out of both Firebase and Google.
     */
    suspend fun signOut() {
        auth.signOut()
        googleSignInClient?.signOut()?.await()
        _authState.value = AuthState.SignedOut
    }

    /**
     * Delete the Firebase Auth account permanently.
     * Call AFTER all Firestore data has been cleaned up.
     * Revokes Google token so the user can re-sign-in with the same account if desired.
     */
    suspend fun deleteAccount(): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("No user signed in"))

        return try {
            // Delete the Firebase Auth account
            user.delete().await()
            Log.d(TAG, "Firebase Auth account deleted for ${user.uid}")

            // Revoke Google sign-in so the account picker resets
            googleSignInClient?.revokeAccess()?.await()

            _authState.value = AuthState.SignedOut
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete Firebase Auth account", e)
            Result.failure(e)
        }
    }

    /**
     * Observe auth state as a Flow.
     */
    fun observeAuthState(): Flow<AuthState> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            trySend(if (user != null) AuthState.SignedIn(user) else AuthState.SignedOut)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    private fun getWebClientId(context: Context): String? {
        // The web client ID is stored in google-services.json and accessed via
        // the auto-generated string resource by the Google Services plugin.
        val resId = context.resources.getIdentifier(
            "default_web_client_id", "string", context.packageName
        )
        return if (resId != 0) {
            context.getString(resId)
        } else {
            null
        }
    }

    companion object {
        private const val TAG = "FirebaseAuthManager"
    }
}

/**
 * Sealed class representing authentication state.
 */
sealed class AuthState {
    data class SignedIn(val user: FirebaseUser) : AuthState()
    data object SignedOut : AuthState()
}
