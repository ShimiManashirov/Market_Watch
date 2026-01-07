package com.example.marketwatch.main

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

class FeedViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts = _posts.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    init {
        fetchPosts()
    }

    private fun fetchPosts() {
        _isLoading.value = true
        db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("FeedViewModel", "Listen failed.", error)
                    _isLoading.value = false
                    return@addSnapshotListener
                }

                val postList = snapshot?.documents?.mapNotNull { document ->
                    document.toObject(Post::class.java)?.copy(id = document.id)
                } ?: emptyList()
                
                _posts.value = postList
                _isLoading.value = false
            }
    }

    fun createPost(text: String, imageUri: Uri?, onComplete: (Boolean) -> Unit) {
        val currentUser = auth.currentUser ?: return onComplete(false)
        val userId = currentUser.uid

        db.collection("users").document(userId).get()
            .addOnSuccessListener { userDoc ->
                val userName = userDoc.getString("name") ?: "Anonymous"
                val userProfileImageUrl = userDoc.getString("profileImageUrl")

                if (imageUri != null) {
                    val imageRef = storage.reference.child("post_images/${UUID.randomUUID()}")
                    imageRef.putFile(imageUri)
                        .continueWithTask { task ->
                            if (!task.isSuccessful) { task.exception?.let { throw it } }
                            imageRef.downloadUrl
                        }.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                savePost(userId, userName, userProfileImageUrl, text, task.result.toString(), onComplete)
                            } else {
                                onComplete(false)
                            }
                        }
                } else {
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
        val newPostRef = db.collection("posts").document()
        val post = Post(
            id = newPostRef.id, // Use the document's ID
            userId = userId,
            userName = userName,
            userProfileImageUrl = userProfileImageUrl,
            text = text,
            imageUrl = imageUrl,
            timestamp = Date()
        )

        newPostRef.set(post)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun updatePost(post: Post, newText: String, onComplete: (Boolean) -> Unit) {
        db.collection("posts").document(post.id)
            .update("text", newText)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun deletePost(postId: String, onComplete: (Boolean, String?) -> Unit) {
        if (postId.isEmpty()) {
            onComplete(false, "Post ID is empty.")
            return
        }
        db.collection("posts").document(postId).delete()
            .addOnSuccessListener { onComplete(true, null) }
            .addOnFailureListener { e -> onComplete(false, e.localizedMessage) }
    }
}
