package com.example.marketwatch.auth

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser = _currentUser.asStateFlow()

    private val _userName = MutableStateFlow("Guest")
    val userName = _userName.asStateFlow()

    private val _userEmail = MutableStateFlow("")
    val userEmail = _userEmail.asStateFlow()

    private val _userPreferences = MutableStateFlow<Map<String, Any>>(emptyMap())
    val userPreferences = _userPreferences.asStateFlow()

    private val _userProfileImageUrl = MutableStateFlow<String?>(null)
    val userProfileImageUrl = _userProfileImageUrl.asStateFlow()

    private val _watchlist = MutableStateFlow<List<String>>(emptyList())
    val watchlist = _watchlist.asStateFlow()

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
        // Use snapshot listener for real-time updates on user profile
        db.collection("users").document(userId)
            .addSnapshotListener { document, e ->
                if (e != null) {
                    Log.w("AuthViewModel", "User details listen failed.", e)
                    return@addSnapshotListener
                }
                if (document != null && document.exists()) {
                    _userName.value = document.getString("name") ?: "Guest"
                    _userEmail.value = document.getString("email") ?: ""
                    _userProfileImageUrl.value = document.getString("profileImageUrl")
                    val data = document.data
                    // Filter out complex objects if any, to keep preferences simple
                    _userPreferences.value = data?.filterValues { it !is Map<*, *> && it !is List<*> } ?: emptyMap()
                }
            }
        
        // Use snapshot listener for real-time updates on watchlist
        db.collection("users").document(userId).collection("watchlist")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("AuthViewModel", "Watchlist listen failed.", e)
                    return@addSnapshotListener
                }
                val symbols = snapshot?.documents?.map { it.id } ?: emptyList()
                _watchlist.value = symbols
            }
    }

    private fun clearUserDetails() {
        _userName.value = "Guest"
        _userEmail.value = ""
        _userPreferences.value = emptyMap()
        _userProfileImageUrl.value = null
        _watchlist.value = emptyList()
    }

    fun toggleWatchlist(symbol: String, onComplete: (Boolean, Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return onComplete(false, false)
        val docRef = db.collection("users").document(userId).collection("watchlist").document(symbol)
        val isInWatchlist = _watchlist.value.contains(symbol)

        if (isInWatchlist) {
            docRef.delete()
                .addOnSuccessListener { onComplete(true, false) } // Success, removed
                .addOnFailureListener { onComplete(false, false) }
        } else {
            docRef.set(mapOf("addedAt" to Date()))
                .addOnSuccessListener { onComplete(true, true) } // Success, added
                .addOnFailureListener { onComplete(false, false) }
        }
    }

    fun updateUserPreference(key: String, value: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).update(key, value)
            .addOnSuccessListener { Log.d("AuthViewModel", "User preference updated: $key = $value") }
            .addOnFailureListener { e -> Log.w("AuthViewModel", "Error updating preference", e) }
    }

    fun uploadProfileImage(uri: Uri, onResult: (Boolean, String?) -> Unit) {
        val userId = auth.currentUser?.uid ?: run {
            onResult(false, "User not logged in.")
            return
        }
        val storageRef = storage.reference.child("profile_images/$userId")

        storageRef.putFile(uri)
            .continueWithTask { task ->
                if (!task.isSuccessful) { task.exception?.let { throw it } }
                storageRef.downloadUrl
            }.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    updateUserPreference("profileImageUrl", task.result.toString())
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.message)
                }
            }
    }

    fun changeUserPassword(newPassword: String, onComplete: (Boolean, String?) -> Unit) {
        auth.currentUser?.updatePassword(newPassword)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onComplete(true, null)
            } else {
                onComplete(false, task.exception?.message)
            }
        }
    }

    fun deleteUser(onComplete: (Boolean, String?) -> Unit) {
        val user = auth.currentUser ?: return onComplete(false, "User not found")
        val userId = user.uid

        db.collection("users").document(userId).delete()
            .addOnSuccessListener { 
                user.delete()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            onComplete(true, null)
                        } else {
                            onComplete(false, task.exception?.message)
                        }
                    }
            }
            .addOnFailureListener { e -> onComplete(false, e.message) }
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
