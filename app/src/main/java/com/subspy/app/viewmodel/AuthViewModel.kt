package com.subspy.app.viewmodel

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.gmail.GmailScopes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.subspy.app.R
import com.subspy.app.data.model.UserProfile
import com.subspy.app.data.repository.FirestoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    // One-off informational messages (e.g. password reset email sent)
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val googleSignInClient: GoogleSignInClient

    init {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getWebClientId())
            .requestEmail()
            .requestScopes(Scope(GmailScopes.GMAIL_READONLY))
            .build()

        googleSignInClient = GoogleSignIn.getClient(context, gso)

        checkCurrentUser()
    }

    private fun getWebClientId(): String {
        return context.getString(R.string.default_web_client_id)
    }

    private fun checkCurrentUser() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            _authState.value = AuthState.Authenticated
            loadUserProfile()
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun signInWithEmail(email: String, password: String) {
        val e = email.trim()
        if (e.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Enter email and password")
            return
        }
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                val result = auth.signInWithEmailAndPassword(e, password).await()
                val user = result.user
                if (user != null) {
                    setAuthenticated(user.uid, user.email ?: e, user.displayName ?: "", user.photoUrl?.toString() ?: "")
                } else {
                    _authState.value = AuthState.Error("Sign-in failed")
                }
            } catch (ex: Exception) {
                Log.e(TAG, "Email sign-in failed", ex)
                _authState.value = AuthState.Error(ex.message ?: "Sign-in failed")
            }
        }
    }

    fun registerWithEmail(name: String, email: String, password: String, confirmPassword: String) {
        val n = name.trim()
        val e = email.trim()
        if (n.isBlank()) {
            _authState.value = AuthState.Error("Enter your name")
            return
        }
        if (e.isBlank()) {
            _authState.value = AuthState.Error("Enter your email")
            return
        }
        if (password.length < 6) {
            _authState.value = AuthState.Error("Password must be at least 6 characters")
            return
        }
        if (password != confirmPassword) {
            _authState.value = AuthState.Error("Passwords do not match")
            return
        }
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                val result = auth.createUserWithEmailAndPassword(e, password).await()
                val user = result.user
                if (user != null) {
                    try {
                        user.updateProfile(
                            UserProfileChangeRequest.Builder().setDisplayName(n).build()
                        ).await()
                    } catch (ex: Exception) {
                        Log.e(TAG, "Failed to set display name", ex)
                    }
                    setAuthenticated(user.uid, user.email ?: e, n, "")
                } else {
                    _authState.value = AuthState.Error("Registration failed")
                }
            } catch (ex: Exception) {
                Log.e(TAG, "Registration failed", ex)
                _authState.value = AuthState.Error(ex.message ?: "Registration failed")
            }
        }
    }

    fun sendPasswordReset(email: String) {
        val e = email.trim()
        if (e.isBlank()) {
            _authState.value = AuthState.Error("Enter your email to reset the password")
            return
        }
        viewModelScope.launch {
            try {
                auth.sendPasswordResetEmail(e).await()
                _message.value = "Password reset email sent to $e"
            } catch (ex: Exception) {
                Log.e(TAG, "Password reset failed", ex)
                _authState.value = AuthState.Error(ex.message ?: "Could not send reset email")
            }
        }
    }

    private fun setAuthenticated(uid: String, email: String, name: String, photoUrl: String) {
        val profile = UserProfile(
            uid = uid,
            email = email,
            displayName = name,
            photoUrl = photoUrl,
            isPremium = false
        )
        _userProfile.value = profile
        _authState.value = AuthState.Authenticated
        viewModelScope.launch {
            try {
                firestoreRepository.saveUserProfile(profile)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save user profile", e)
            }
        }
    }

    fun getSignInIntent(): Intent {
        Log.d(TAG, "getSignInIntent called")
        return googleSignInClient.signInIntent
    }

    fun handleSignInResult(data: Intent?) {
        Log.d(TAG, "handleSignInResult called, data=${if (data == null) "null" else "present"}")
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)
                Log.d(TAG, "Google account: email=${account.email}, idToken=${if (account.idToken == null) "null" else "present"}")
                if (account.idToken == null) {
                    _authState.value = AuthState.Error("No ID token returned. Check web client ID / OAuth config.")
                    return@launch
                }
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                Log.e(TAG, "Google sign-in ApiException code=${e.statusCode}", e)
                _authState.value = AuthState.Error("Sign-in failed (code ${e.statusCode}): ${e.message}")
            }
        }
    }

    private suspend fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        try {
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user

            if (user != null) {
                Log.d(TAG, "Firebase auth success, uid=${user.uid} -> Authenticated")
                setAuthenticated(
                    user.uid,
                    user.email ?: "",
                    user.displayName ?: "",
                    user.photoUrl?.toString() ?: ""
                )
            } else {
                _authState.value = AuthState.Error("Authentication failed: no user returned")
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error("Authentication failed: ${e.message}")
        }
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            _userProfile.value = firestoreRepository.getUserProfile()
        }
    }

    fun signOut() {
        auth.signOut()
        googleSignInClient.signOut()
        _authState.value = AuthState.Unauthenticated
        _userProfile.value = null
    }

    companion object {
        private const val TAG = "SubSpyAuth"
    }
}

sealed class AuthState {
    data object Initial : AuthState()
    data object Loading : AuthState()
    data object Authenticated : AuthState()
    data object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
}
