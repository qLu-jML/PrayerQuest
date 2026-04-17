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
     */
    suspend fun handleSignInResult(result: ActivityResult): Result<FirebaseUser> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
                ?: return Result.failure(Exception("No ID token received"))

            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user
                ?: return Result.failure(Exception("Sign-in succeeded but no user returned"))

            Log.d(TAG, "Sign-in successful: ${user.displayName}")
            Result.success(user)
        } catch (e: ApiException) {
            Log.e(TAG, "Google Sign-In failed: ${e.statusCode}", e)
            Result.failure(Exception("Google Sign-In failed: ${e.message}"))
        } catch (e: Exception) {
            Log.e(TAG, "Firebase auth failed", e)
            Result.failure(e)
        }
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
