package com.example.marketwatch.main

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Post(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userProfileImageUrl: String? = null,
    val text: String = "",
    val imageUrl: String? = null,
    @ServerTimestamp
    val timestamp: Date? = null
)
