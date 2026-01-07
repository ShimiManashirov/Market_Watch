package com.example.marketwatch.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun MyPostsScreen(
    myPostsViewModel: MyPostsViewModel = viewModel(),
    onEditPost: (String) -> Unit
) {
    val posts by myPostsViewModel.posts.collectAsState()
    val isLoading by myPostsViewModel.isLoading.collectAsState()

    Scaffold {
        Box(modifier = Modifier.fillMaxSize().padding(it), contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator()
            } else if (posts.isEmpty()) {
                Text("You haven't posted anything yet.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(posts) { post ->
                        PostItem(post = post, onEditClick = { onEditPost(post.id) }, onDeleteClick = { /* Add delete logic if needed */ })
                    }
                }
            }
        }
    }
}
