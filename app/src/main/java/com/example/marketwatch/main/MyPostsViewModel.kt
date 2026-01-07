package com.example.marketwatch.main

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MyPostsViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts = _posts.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    init {
        fetchUserPosts()
    }

    private fun fetchUserPosts() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _isLoading.value = false
            return
        }

        _isLoading.value = true
        db.collection("posts")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("MyPostsViewModel", "Listen failed.", error)
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
}
