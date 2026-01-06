package com.example.marketwatch.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _userName = MutableStateFlow("Guest")
    val userName = _userName.asStateFlow()

    private val _userEmail = MutableStateFlow("")
    val userEmail = _userEmail.asStateFlow()

    private val _userPreferences = MutableStateFlow<Map<String, Any>>(emptyMap())
    val userPreferences = _userPreferences.asStateFlow()

    init {
        fetchUserDetails()
    }

    private fun fetchUserDetails() {
        val userId = auth.currentUser?.uid
        userId?.let {
            db.collection("users").document(it).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        _userName.value = document.getString("name") ?: "Guest"
                        _userEmail.value = document.getString("email") ?: ""
                        _userPreferences.value = document.data ?: emptyMap()
                    }
                }
        }
    }

    fun updateUserPreference(key: String, value: String) {
        val userId = auth.currentUser?.uid
        userId?.let {
            db.collection("users").document(it).update(key, value)
                .addOnSuccessListener { 
                    Log.d("AuthViewModel", "User preference updated: $key = $value")
                    fetchUserDetails()
                }
                .addOnFailureListener { e -> Log.w("AuthViewModel", "Error updating preference", e) }
        }
    }

    fun changeUserPassword(newPassword: String, onComplete: (Boolean, String?) -> Unit) {
        auth.currentUser?.updatePassword(newPassword)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("AuthViewModel", "Password updated successfully")
                onComplete(true, null)
            } else {
                Log.w("AuthViewModel", "Error updating password", task.exception)
                onComplete(false, task.exception?.message)
            }
        }
    }

    fun deleteUser(onComplete: (Boolean, String?) -> Unit) {
        val user = auth.currentUser
        val userId = user?.uid

        if (userId == null) {
            onComplete(false, "User not found")
            return
        }

        // 1. Delete user document from Firestore
        db.collection("users").document(userId).delete()
            .addOnSuccessListener { 
                Log.d("AuthViewModel", "User document deleted from Firestore")
                // 2. Delete user from Authentication
                user.delete()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("AuthViewModel", "User deleted from Authentication")
                            onComplete(true, null)
                        } else {
                            Log.w("AuthViewModel", "Error deleting user from Authentication", task.exception)
                            onComplete(false, task.exception?.message)
                        }
                    }
            }
            .addOnFailureListener { e ->
                Log.w("AuthViewModel", "Error deleting user document from Firestore", e)
                onComplete(false, e.message)
            }
    }

    fun createUser(email: String, password: String, fullName: String, onComplete: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    val userDetails = hashMapOf(
                        "name" to fullName,
                        "email" to email,
                        "currency" to "USD - $ (דולר אמריקאי)", // Default preference
                        "timezone" to "(UTC+3) ישראל" // Default preference
                    )

                    userId?.let {
                        db.collection("users").document(it).set(userDetails)
                            .addOnSuccessListener { 
                                onComplete(true, null) 
                            }
                            .addOnFailureListener { e ->
                                onComplete(false, e.message)
                            }
                    }
                } else {
                    onComplete(false, task.exception?.message)
                }
            }
    }

    fun loginUser(email: String, password: String, onComplete: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    fetchUserDetails() // Fetch user details on successful login
                    onComplete(true, null)
                } else {
                    onComplete(false, task.exception?.message)
                }
            }
    }

    fun logout() {
        auth.signOut()
        _userName.value = "Guest"
        _userEmail.value = ""
        _userPreferences.value = emptyMap()
    }
}
