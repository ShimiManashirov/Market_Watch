package com.example.marketwatch.main

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FeedViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

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
                val postList = snapshot?.toObjects(Post::class.java) ?: emptyList()
                _posts.value = postList
                _isLoading.value = false
            }
    }
}
