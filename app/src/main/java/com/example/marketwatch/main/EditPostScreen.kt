package com.example.marketwatch.main

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.marketwatch.R
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPostScreen(
    postId: String,
    onNavigateBack: () -> Unit,
    feedViewModel: FeedViewModel = viewModel()
) {
    var text by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var originalImageUrl by remember { mutableStateOf<String?>(null) }
    var isPosting by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current

    // Fetch the post details when the screen is first composed
    LaunchedEffect(postId) {
        val db = FirebaseFirestore.getInstance()
        db.collection("posts").document(postId).get()
            .addOnSuccessListener { document ->
                text = document.getString("text") ?: ""
                originalImageUrl = document.getString("imageUrl")
                isLoading = false
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to load post.", Toast.LENGTH_SHORT).show()
                isLoading = false
                onNavigateBack()
            }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? -> imageUri = uri }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Post") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isPosting) {
                        CircularProgressIndicator(modifier = Modifier.padding(horizontal = 16.dp).size(24.dp))
                    } else {
                        Button(
                            onClick = {
                                isPosting = true
                                val postToUpdate = Post(id = postId, text = text, imageUrl = originalImageUrl)
                                feedViewModel.updatePost(postToUpdate, text) { success ->
                                    isPosting = false
                                    if (success) {
                                        onNavigateBack()
                                    } else {
                                        Toast.makeText(context, "Failed to update post", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = text.isNotBlank(),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Save")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    placeholder = { Text("What are you thinking?") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                AsyncImage(
                    model = imageUri ?: originalImageUrl,
                    contentDescription = "Selected image",
                    placeholder = painterResource(id = R.drawable.ic_launcher_foreground),
                    error = painterResource(id = R.drawable.ic_launcher_foreground),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = "Change Image", modifier = Modifier.padding(end = 8.dp))
                    Text("Change Image")
                }
            }
        }
    }
}
