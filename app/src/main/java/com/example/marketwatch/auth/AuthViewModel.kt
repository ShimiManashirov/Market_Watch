package com.example.marketwatch.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser = _currentUser.asStateFlow()

    private val _userName = MutableStateFlow("Guest")
    val userName = _userName.asStateFlow()

    private val _userEmail = MutableStateFlow("")
    val userEmail = _userEmail.asStateFlow()

    private val _userPreferences = MutableStateFlow<Map<String, Any>>(emptyMap())
    val userPreferences = _userPreferences.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _currentUser.value = user
            if (user != null) {
                fetchUserDetails(user.uid)
            } else {
                clearUserDetails()
            }
        }
    }

    private fun fetchUserDetails(userId: String) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    _userName.value = document.getString("name") ?: "Guest"
                    _userEmail.value = document.getString("email") ?: ""
                    val data = document.data
                    val simpleData = data?.filterValues { it !is Map<*, *> && it !is List<*> }
                    _userPreferences.value = simpleData ?: emptyMap()
                }
            }
    }

    private fun clearUserDetails() {
        _userName.value = "Guest"
        _userEmail.value = ""
        _userPreferences.value = emptyMap()
    }

    fun updateUserPreference(key: String, value: String) {
        val userId = auth.currentUser?.uid
        userId?.let {
            db.collection("users").document(it).update(key, value)
                .addOnSuccessListener { 
                    Log.d("AuthViewModel", "User preference updated: $key = $value")
                    fetchUserDetails(userId)
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

        db.collection("users").document(userId).delete()
            .addOnSuccessListener { 
                Log.d("AuthViewModel", "User document deleted from Firestore")
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
                        "currency" to "USD - $ (US Dollar)",
                        "timezone" to "(UTC+3) Israel"
                    )

                    userId?.let {
                        db.collection("users").document(it).set(userDetails)
                            .addOnSuccessListener { onComplete(true, null) }
                            .addOnFailureListener { e -> onComplete(false, e.message) }
                    }
                } else {
                    onComplete(false, task.exception?.message)
                }
            }
    }

    fun loginUser(email: String, password: String, onComplete: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                onComplete(task.isSuccessful, task.exception?.message)
            }
    }

    fun logout() {
        auth.signOut()
    }
}
