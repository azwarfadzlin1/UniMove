package com.example.unimove.ui.viewmodel

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val user: FirebaseUser? = null,
    val currentUserName: String? = null,
    val currentUserPhotoUrl: String? = null,
    val isEmailNotVerified: Boolean = false,
    val profile: UserProfile? = null,
    val isProfileComplete: Boolean = false
)

data class UserTotals(
    val totalDistanceKm: Double = 0.0,
    val totalTimeSec: Long = 0L
)

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val dailyCalorieTarget: Int? = null,
    val runGoalKm: Double? = null,
    val gender: String = "",
    val college: String = "",
    val age: Int? = null,
    val weight: Double? = null,
    val height: Double? = null,
    val avatarKey: String = "",
    val authProvider: String = "",
    val isProfileComplete: Boolean = false
)

data class AuthOutcome(
    val requiresOnboarding: Boolean
)

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private var userDocumentListener: ListenerRegistration? = null
    private var observedUid: String? = null

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _totals = MutableStateFlow(UserTotals())
    val totals: StateFlow<UserTotals> = _totals.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _uiState.value = _uiState.value.copy(user = user)

            if (user != null) {
                observeUserDocument(user.uid)
                refreshUserState(user.uid)
            } else {
                clearObservedUser()
                _uiState.value = _uiState.value.copy(
                    currentUserName = null,
                    currentUserPhotoUrl = null,
                    profile = null,
                    isProfileComplete = false
                )
                _totals.value = UserTotals()
            }
        }

        auth.currentUser?.let { currentUser ->
            _uiState.value = _uiState.value.copy(user = currentUser)
            observeUserDocument(currentUser.uid)
            refreshUserState(currentUser.uid)
        }
    }

    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    fun registerWithEmail(
        email: String,
        password: String,
        onVerificationSent: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (email.isBlank() || password.isBlank()) {
            onError("Email and password cannot be empty.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user ?: throw Exception("User creation failed.")

                user.sendEmailVerification().await()

                _uiState.value = _uiState.value.copy(isLoading = false)
                onVerificationSent()
            } catch (e: Exception) {
                val msg = friendlyAuthError(e)
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = msg)
                onError(msg)
            }
        }
    }

    fun checkEmailVerification(
        showLoading: Boolean = true,
        onVerified: (AuthOutcome) -> Unit,
        onNotVerified: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = auth.currentUser
        if (user == null) {
            onError("Your verification session expired. Please sign in with your email.")
            return
        }

        viewModelScope.launch {
            if (showLoading) {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = null)
            }
            try {
                user.reload().await()
                val reloadedUser = auth.currentUser ?: throw Exception("Verification session expired.")

                if (!reloadedUser.isEmailVerified) {
                    if (showLoading) {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                    onNotVerified()
                    return@launch
                }

                val email = reloadedUser.email.orEmpty()
                val outcome = ensureUserDocument(
                    user = reloadedUser,
                    provider = "password",
                    fallbackName = email.substringBefore("@").ifBlank { "User" }
                )

                _uiState.value = _uiState.value.copy(
                    user = reloadedUser,
                    isLoading = false,
                    isEmailNotVerified = false
                )
                onVerified(outcome)
            } catch (e: Exception) {
                val msg = friendlyAuthError(e)
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = msg)
                onError(msg)
            }
        }
    }

    fun resendVerificationEmail(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = auth.currentUser
        if (user == null) {
            onError("Your verification session expired. Please sign in with your email.")
            return
        }

        viewModelScope.launch {
            try {
                user.sendEmailVerification().await()
                onSuccess()
            } catch (e: Exception) {
                onError(friendlyAuthError(e))
            }
        }
    }

    fun loginWithEmail(
        email: String,
        password: String,
        onSuccess: (AuthOutcome) -> Unit,
        onNotVerified: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (email.isBlank() || password.isBlank()) {
            onError("Email and password cannot be empty.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                isEmailNotVerified = false
            )
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val user = result.user ?: throw Exception("Sign-in failed.")

                if (!user.isEmailVerified) {
                    runCatching { user.sendEmailVerification().await() }
                    auth.signOut()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isEmailNotVerified = true
                    )
                    onNotVerified()
                    return@launch
                }

                val outcome = ensureUserDocument(
                    user = user,
                    provider = "password",
                    fallbackName = email.substringBefore("@").ifBlank { "User" }
                )

                _uiState.value = _uiState.value.copy(isLoading = false)
                onSuccess(outcome)
            } catch (e: Exception) {
                val msg = friendlyAuthError(e)
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = msg)
                onError(msg)
            }
        }
    }

    fun resetPassword(
        email: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (email.isBlank()) {
            onError("Please enter your email address.")
            return
        }

        viewModelScope.launch {
            try {
                auth.sendPasswordResetEmail(email).await()
                onSuccess()
            } catch (e: Exception) {
                onError(friendlyAuthError(e))
            }
        }
    }

    fun signInWithGoogle(
        context: Context,
        webClientId: String,
        onSuccess: (AuthOutcome) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val credentialManager = CredentialManager.create(context)
                val result = credentialManager.getCredential(context = context, request = request)

                val googleIdToken = GoogleIdTokenCredential
                    .createFrom(result.credential.data)
                    .idToken

                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val authResult = auth.signInWithCredential(firebaseCredential).await()
                val user = authResult.user ?: throw Exception("Firebase user is null")

                val outcome = ensureUserDocument(
                    user = user,
                    provider = "google",
                    fallbackName = user.displayName ?: user.email?.substringBefore("@") ?: "User"
                )

                _uiState.value = _uiState.value.copy(isLoading = false)
                onSuccess(outcome)
            } catch (e: GetCredentialCancellationException) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                val msg = when {
                    e.message?.contains("network", ignoreCase = true) == true -> "Network error. Check your connection."
                    e.message?.contains("canceled", ignoreCase = true) == true -> "Sign-in was cancelled."
                    else -> "Google sign-in failed. Please try again."
                }
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = msg)
                onError(msg)
            }
        }
    }

    fun signOut() {
        clearObservedUser()
        auth.signOut()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null, isEmailNotVerified = false)
    }

    fun updateDailyCalorieTarget(
        targetCalories: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("User not logged in")
            return
        }

        if (targetCalories < 800 || targetCalories > 6000) {
            onError("Target must be between 800 and 6000 kcal")
            return
        }

        viewModelScope.launch {
            try {
                firestore.collection("users").document(uid)
                    .set(mapOf("dailyCalorieTarget" to targetCalories), SetOptions.merge())
                    .await()
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to save calorie target")
            }
        }
    }

    fun updateProfileDetails(
        name: String,
        gender: String,
        college: String,
        age: Int?,
        height: Double?,
        weight: Double?,
        runGoalKm: Double?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("User not logged in")
            return
        }

        val safeName = name.trim()
        val safeCollege = college.trim()
        if (safeName.isBlank()) {
            onError("Name cannot be empty")
            return
        }
        if (safeCollege.isBlank()) {
            onError("Please select your college")
            return
        }

        if (age != null && (age < 10 || age > 100)) {
            onError("Age must be between 10 and 100")
            return
        }
        if (height != null && (height <= 0.0 || height > 260.0)) {
            onError("Height must be between 1 and 260 cm")
            return
        }
        if (weight != null && (weight <= 0.0 || weight > 350.0)) {
            onError("Weight must be between 1 and 350 kg")
            return
        }
        if (runGoalKm != null && (runGoalKm < 0.1 || runGoalKm > 100000.0)) {
            onError("Running goal must be between 0.1 and 100,000 km")
            return
        }

        viewModelScope.launch {
            try {
                val updates = mutableMapOf<String, Any>(
                    "name" to safeName,
                    "gender" to gender.trim(),
                    "college" to safeCollege,
                    "updatedAt" to System.currentTimeMillis()
                )

                if (age != null) updates["age"] = age else updates["age"] = FieldValue.delete()
                if (height != null) updates["height"] = height else updates["height"] = FieldValue.delete()
                if (weight != null) updates["weight"] = weight else updates["weight"] = FieldValue.delete()
                if (runGoalKm != null) updates["runGoalKm"] = runGoalKm else updates["runGoalKm"] = FieldValue.delete()

                firestore.collection("users").document(uid)
                    .set(updates, SetOptions.merge())
                    .await()

                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to update profile")
            }
        }
    }

    suspend fun resolveStartDestination(): String {
        val user = auth.currentUser ?: return "landing"
        if (!user.isEmailVerified && user.providerData.any { it.providerId == "password" }) {
            return "register"
        }
        val snapshot = firestore.collection("users").document(user.uid).get().await()
        return if (snapshot.getBoolean("profileCompleted") == true) "home" else "onboarding"
    }

    private suspend fun ensureUserDocument(
        user: FirebaseUser,
        provider: String,
        fallbackName: String
    ): AuthOutcome {
        val docRef = firestore.collection("users").document(user.uid)
        val snapshot = docRef.get().await()
        val now = Timestamp(Date())

        if (!snapshot.exists()) {
            val userMap = hashMapOf(
                "name" to (user.displayName?.takeIf { it.isNotBlank() } ?: fallbackName),
                "email" to (user.email ?: ""),
                "avatarKey" to "",
                "photoUrl" to "",
                "providerPhotoUrl" to (user.photoUrl?.toString() ?: ""),
                "authProvider" to provider,
                "college" to "",
                "profileCompleted" to false,
                "onboardingCompleted" to false,
                "totalDistance" to 0.0,
                "totalTime" to 0L,
                "totalSessions" to 0L,
                "createdAt" to now,
                "lastLogin" to now
            )
            docRef.set(userMap).await()
            refreshUserState(user.uid)
            return AuthOutcome(requiresOnboarding = true)
        }

        val updates = hashMapOf<String, Any>(
            "email" to (user.email ?: snapshot.getString("email").orEmpty()),
            "authProvider" to provider,
            "providerPhotoUrl" to (user.photoUrl?.toString() ?: snapshot.getString("providerPhotoUrl").orEmpty()),
            "lastLogin" to now
        )

        if (snapshot.getString("name").isNullOrBlank()) {
            updates["name"] = user.displayName?.takeIf { it.isNotBlank() } ?: fallbackName
        }
        if (snapshot.getString("college") == null) {
            updates["college"] = ""
        }

        docRef.set(updates, SetOptions.merge()).await()
        refreshUserState(user.uid)

        return AuthOutcome(
            requiresOnboarding = snapshot.getBoolean("profileCompleted") != true
        )
    }

    private fun refreshUserState(uid: String) {
        fetchUserProfile(uid)
        fetchUserTotals(uid)
    }

    private fun observeUserDocument(uid: String) {
        if (observedUid == uid && userDocumentListener != null) return

        clearObservedUser()
        observedUid = uid
        userDocumentListener = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null || !snapshot.exists()) return@addSnapshotListener

                val avatarKey = snapshot.getString("avatarKey")
                    ?.takeIf { it.isNotBlank() }
                    ?: snapshot.getString("photoUrl").orEmpty()

                val profile = UserProfile(
                    uid = uid,
                    name = snapshot.getString("name").orEmpty(),
                    email = snapshot.getString("email").orEmpty(),
                    dailyCalorieTarget = snapshot.getLong("dailyCalorieTarget")?.toInt(),
                    runGoalKm = snapshot.getDouble("runGoalKm"),
                    gender = snapshot.getString("gender").orEmpty(),
                    college = snapshot.getString("college").orEmpty(),
                    age = snapshot.getLong("age")?.toInt(),
                    weight = snapshot.getDouble("weight"),
                    height = snapshot.getDouble("height"),
                    avatarKey = avatarKey,
                    authProvider = snapshot.getString("authProvider").orEmpty(),
                    isProfileComplete = snapshot.getBoolean("profileCompleted") == true
                )

                _uiState.value = _uiState.value.copy(
                    currentUserName = profile.name.ifBlank { null },
                    currentUserPhotoUrl = profile.avatarKey.ifBlank { null },
                    profile = profile,
                    isProfileComplete = profile.isProfileComplete
                )

                _totals.value = UserTotals(
                    totalDistanceKm = snapshot.getDouble("totalDistance") ?: 0.0,
                    totalTimeSec = snapshot.getLong("totalTime") ?: 0L
                )
            }
    }

    private fun clearObservedUser() {
        userDocumentListener?.remove()
        userDocumentListener = null
        observedUid = null
    }

    private fun fetchUserProfile(uid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val doc = firestore.collection("users").document(uid).get().await()
                if (!doc.exists()) return@launch

                val avatarKey = doc.getString("avatarKey")
                    ?.takeIf { it.isNotBlank() }
                    ?: doc.getString("photoUrl").orEmpty()

                val profile = UserProfile(
                    uid = uid,
                    name = doc.getString("name").orEmpty(),
                    email = doc.getString("email").orEmpty(),
                    dailyCalorieTarget = doc.getLong("dailyCalorieTarget")?.toInt(),
                    runGoalKm = doc.getDouble("runGoalKm"),
                    gender = doc.getString("gender").orEmpty(),
                    college = doc.getString("college").orEmpty(),
                    age = doc.getLong("age")?.toInt(),
                    weight = doc.getDouble("weight"),
                    height = doc.getDouble("height"),
                    avatarKey = avatarKey,
                    authProvider = doc.getString("authProvider").orEmpty(),
                    isProfileComplete = doc.getBoolean("profileCompleted") == true
                )

                _uiState.value = _uiState.value.copy(
                    currentUserName = profile.name.ifBlank { null },
                    currentUserPhotoUrl = profile.avatarKey.ifBlank { null },
                    profile = profile,
                    isProfileComplete = profile.isProfileComplete
                )
            } catch (_: Exception) {
            }
        }
    }

    private fun fetchUserTotals(uid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val doc = firestore.collection("users").document(uid).get().await()
                if (doc.exists()) {
                    _totals.value = UserTotals(
                        totalDistanceKm = doc.getDouble("totalDistance") ?: 0.0,
                        totalTimeSec = doc.getLong("totalTime") ?: 0L
                    )
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun friendlyAuthError(e: Exception): String {
        val msg = e.message ?: ""
        val authCode = (e as? FirebaseAuthException)?.errorCode
        when (authCode) {
            "ERROR_INVALID_EMAIL" -> return "Enter a valid email address."
            "ERROR_EMAIL_ALREADY_IN_USE" -> return "An account with this email already exists. Sign in instead."
            "ERROR_WEAK_PASSWORD" -> return "Use a stronger password with at least 6 characters."
            "ERROR_WRONG_PASSWORD" -> return "Wrong password. Try again or reset your password."
            "ERROR_INVALID_CREDENTIAL" -> return "Wrong email or password. Check both fields and try again."
            "ERROR_USER_NOT_FOUND" -> return "No account found with this email. Create an account first."
            "ERROR_USER_DISABLED" -> return "This account has been disabled. Contact support."
            "ERROR_TOO_MANY_REQUESTS" -> return "Too many attempts. Wait a moment, then try again."
            "ERROR_NETWORK_REQUEST_FAILED" -> return "Network error. Check your connection and try again."
            "ERROR_OPERATION_NOT_ALLOWED" -> return "Email sign-in is not enabled for this app."
        }
        return when {
            msg.contains("email address is already in use", ignoreCase = true) -> "An account with this email already exists. Sign in instead."
            msg.contains("badly formatted", ignoreCase = true) -> "Enter a valid email address."
            msg.contains("invalid credential", ignoreCase = true) ||
                msg.contains("invalid_login_credentials", ignoreCase = true) ||
                msg.contains("password is invalid", ignoreCase = true) ||
                msg.contains("wrong password", ignoreCase = true) -> "Wrong email or password. Check both fields and try again."
            msg.contains("no user record", ignoreCase = true) ||
                msg.contains("user not found", ignoreCase = true) -> "No account found with this email. Create an account first."
            msg.contains("weak-password", ignoreCase = true) ||
                msg.contains("at least 6", ignoreCase = true) -> "Use a stronger password with at least 6 characters."
            msg.contains("network", ignoreCase = true) -> "Network error. Check your connection and try again."
            else -> "We could not complete that request. Please try again."
        }
    }

    override fun onCleared() {
        clearObservedUser()
        super.onCleared()
    }
}
