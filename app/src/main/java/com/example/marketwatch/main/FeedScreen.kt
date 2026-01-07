package com.example.marketwatch.main

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.marketwatch.R
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FeedScreen(
    feedViewModel: FeedViewModel = viewModel(),
    onAddPost: () -> Unit,
    onEditPost: (String) -> Unit
) {
    val posts by feedViewModel.posts.collectAsState()
    val isLoading by feedViewModel.isLoading.collectAsState()
    val context = LocalContext.current
    
    var showAddPostDialog by remember { mutableStateOf(false) }
    var showEditPostDialog by remember { mutableStateOf<Post?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<Post?>(null) }

    if (showAddPostDialog) {
        AddEditPostDialog(
            onDismiss = { showAddPostDialog = false },
            onConfirm = { text, imageUri ->
                feedViewModel.createPost(text, imageUri) { success ->
                    if (!success) {
                        Toast.makeText(context, "Failed to create post.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    showEditPostDialog?.let {
        AddEditPostDialog(
            post = it,
            onDismiss = { showEditPostDialog = null },
            onConfirm = { text, _ -> // Image editing not supported in this simplified dialog
                feedViewModel.updatePost(it, text) { success ->
                     if (!success) {
                        Toast.makeText(context, "Failed to update post.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    showDeleteConfirmDialog?.let { postToDelete ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("Delete Post") },
            text = { Text("Are you sure you want to permanently delete this post?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        feedViewModel.deletePost(postToDelete.id) { success, errorMessage ->
                            val message = if (success) "Post deleted" else "Failed to delete post: $errorMessage"
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                        showDeleteConfirmDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddPostDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Post")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator()
            } else if (posts.isEmpty()) {
                Text("No posts yet. Be the first to share!")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(posts, key = { it.id }) { post ->
                        PostItem(
                            post = post, 
                            onEditClick = { onEditPost(post.id) },
                            onDeleteClick = { showDeleteConfirmDialog = post }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PostItem(post: Post, onEditClick: () -> Unit, onDeleteClick: () -> Unit) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    AsyncImage(
                        model = post.userProfileImageUrl,
                        contentDescription = "User profile image",
                        placeholder = painterResource(id = R.drawable.ic_launcher_foreground),
                        error = painterResource(id = R.drawable.ic_launcher_foreground),
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = post.userName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                        val date = post.timestamp?.let { SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault()).format(it) }
                        Text(text = date ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (post.userId == currentUserId) {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = {
                                    showMenu = false
                                    onEditClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    showMenu = false
                                    onDeleteClick()
                                }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = post.text, style = MaterialTheme.typography.bodyLarge)
            post.imageUrl?.let {
                Spacer(modifier = Modifier.height(12.dp))
                AsyncImage(
                    model = it,
                    contentDescription = "Post image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
fun AddEditPostDialog(
    post: Post? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, Uri?) -> Unit
) {
    var text by remember { mutableStateOf(post?.text ?: "") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? -> imageUri = uri }
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (post == null) "Create Post" else "Edit Post") },
        text = {
            Column {
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("What's on your mind?") }
                )
                if (post == null) { // Only show image picker for new posts
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Add Image")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = if(imageUri == null) "Add Image" else "Change Image")
                    }
                    imageUri?.let {
                        AsyncImage(model = it, contentDescription = null, modifier = Modifier.size(100.dp).padding(top = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(text, imageUri)
                    onDismiss()
                },
                enabled = text.isNotBlank()
            ) {
                Text(if (post == null) "Post" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
