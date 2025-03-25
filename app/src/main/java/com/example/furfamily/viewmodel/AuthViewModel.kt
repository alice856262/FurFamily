package com.example.furfamily.viewmodels

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.furfamily.R
import com.example.furfamily.profile.UserProfile
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val application: Application,
    private val firebaseAuth: FirebaseAuth,
    private val firebaseDatabase: FirebaseDatabase
) : ViewModel() {

    private val _googleSignInIntent = MutableLiveData<Intent>()
    val googleSignInIntent: LiveData<Intent> = _googleSignInIntent

    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> = _statusMessage

    private val googleSignInClient: GoogleSignInClient

    init {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(application.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(application, gso)
    }

    fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        _googleSignInIntent.value = signInIntent
    }

    fun firebaseAuthWithGoogle(idToken: String, account: GoogleSignInAccount, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FirebaseAuth", "Firebase authentication succeeded")
                    onSuccess()
                } else {
                    Log.e("FirebaseAuth", "Firebase authentication failed", task.exception)
                    onFailure(task.exception!!)
                }
            }
    }

    fun signInWithEmailPassword(email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                firebaseAuth.signInWithEmailAndPassword(email, password).await()
                _authState.postValue(AuthState.AUTHENTICATED)
                _statusMessage.postValue("Email Sign-In Successful")
            } catch (e: Exception) {
                _authState.postValue(AuthState.UNAUTHENTICATED)
                _statusMessage.postValue("Sign-In Failed: ${e.localizedMessage}")
                Log.e("AuthViewModel", "Email sign-in failed", e)
            }
        }
    }

    fun registerWithEmailPassword(
        email: String,
        password: String,
        firstName: String,
        lastName: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Create user in Firebase Authentication
                val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
                val userId = authResult.user?.uid ?: throw Exception("User ID is null")

                // Create user profile in Realtime Database
                val userProfile = UserProfile(
                    userId = userId,
                    email = email,
                    firstName = firstName,
                    lastName = lastName,
                    password = "", // We don't store password in profile
                    selectedGender = "",
                    phone = "",
                    birthDate = Date(),
                    isGoogleUser = false,
                    profileImageUrl = ""
                )

                firebaseDatabase.reference.child("userProfiles").child(userId).setValue(userProfile).await()

                _authState.postValue(AuthState.AUTHENTICATED)
                _statusMessage.postValue("Registration Successful")
            } catch (e: Exception) {
                _authState.postValue(AuthState.UNAUTHENTICATED)
                _statusMessage.postValue("Registration Failed: ${e.localizedMessage}")
                Log.e("AuthViewModel", "Registration failed", e)
            }
        }
    }

    fun fetchOrCreateUserProfile(account: GoogleSignInAccount, onSuccess: (UserProfile) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val email = account.email ?: return@launch  // Exit if email is null
            val dbRef = FirebaseDatabase.getInstance().getReference()
            val userProfilesRef = dbRef.child("userProfiles").orderByChild("email").equalTo(email)

            userProfilesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // User profile exists, retrieve and pass it to onSuccess
                        snapshot.children.forEach {
                            val userProfile = it.getValue(UserProfile::class.java)
                            userProfile?.let { profile ->
                                onSuccess(profile)
                                return@forEach
                            }
                        }
                    } else {
                        // No profile exists, create a new one
                        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
                        Log.e("ViewModel 101", "${userId}")
                        val newUserProfile = UserProfile(
                            userId = userId,
                            email = email,
                            firstName = account.givenName ?: "",
                            lastName = account.familyName ?: "",
                            password = "",  // Password is not stored for Google users
                            selectedGender = "",
                            phone = "",
                            birthDate = Date(),
                            isGoogleUser = true,
                            profileImageUrl = "")
                        // Insert new user profile into Firebase and call onSuccess
                        dbRef.child("userProfiles").child(userId).setValue(newUserProfile)
                            .addOnSuccessListener {
                                onSuccess(newUserProfile)
                            }
                            .addOnFailureListener {
                                Log.e("FirebaseError", "Failed to create new user profile", it)
                            }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseError", "Failed to fetch user profile: ${error.message}")
                }
            })
        }
    }

    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                firebaseAuth.sendPasswordResetEmail(email).await()
                _statusMessage.postValue("Password reset email sent")
            } catch (e: Exception) {
                _statusMessage.postValue("Failed to send reset link: ${e.localizedMessage}")
                Log.e("AuthViewModel", "Password reset failed", e)
            }
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
        googleSignInClient.signOut()
        _authState.value = AuthState.UNAUTHENTICATED
        _statusMessage.postValue("Signed out successfully")
    }

    // Enum to represent authentication state
    enum class AuthState {
        AUTHENTICATED,
        UNAUTHENTICATED
    }
}