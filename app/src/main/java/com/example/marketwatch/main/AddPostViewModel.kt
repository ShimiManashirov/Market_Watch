package com.example.marketwatch.main

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.Date
import java.util.UUID

class AddPostViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    fun createPost(text: String, imageUri: Uri?, onComplete: (Boolean) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onComplete(false)
            return
        }
        val userId = currentUser.uid

        // First, get the user's details from the 'users' collection
        db.collection("users").document(userId).get()
            .addOnSuccessListener { userDoc ->
                val userName = userDoc.getString("name") ?: "Anonymous"
                val userProfileImageUrl = userDoc.getString("profileImageUrl")

                // If an image is selected, upload it to Firebase Storage first
                if (imageUri != null) {
                    val imageRef = storage.reference.child("post_images/${UUID.randomUUID()}")
                    imageRef.putFile(imageUri)
                        .addOnSuccessListener { 
                            imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                                savePost(userId, userName, userProfileImageUrl, text, downloadUrl.toString(), onComplete)
                            }
                        }
                        .addOnFailureListener { onComplete(false) }
                } else {
                    // If no image, save the post directly
                    savePost(userId, userName, userProfileImageUrl, text, null, onComplete)
                }
            }
            .addOnFailureListener { onComplete(false) }
    }

    private fun savePost(
        userId: String,
        userName: String,
        userProfileImageUrl: String?,
        text: String,
        imageUrl: String?,
        onComplete: (Boolean) -> Unit
    ) {
        val post = Post(
            userId = userId,
            userName = userName,
            userProfileImageUrl = userProfileImageUrl,
            text = text,
            imageUrl = imageUrl,
            timestamp = Date() // Firestore will convert this to a server timestamp
        )

        db.collection("posts").add(post)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }
}
