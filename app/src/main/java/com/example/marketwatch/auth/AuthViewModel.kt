package com.example.marketwatch.auth

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

    init {
        fetchUserDetails()
    }

    private fun fetchUserDetails() {
        val userId = auth.currentUser?.uid
        userId?.let {
            db.collection("users").document(it).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        _userName.value = document.getString("name") ?: "Guest"
                        _userEmail.value = document.getString("email") ?: ""
                    }
                }
        }
    }

    fun createUser(email: String, password: String, fullName: String, onComplete: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    val userDetails = hashMapOf(
                        "name" to fullName,
                        "email" to email
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
    }
}
