package com.example.marketwatch.main

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Transaction(
    val id: String = "",
    val symbol: String = "",
    val name: String = "",
    val quantity: Double = 0.0,
    val purchasePrice: Double = 0.0,
    @ServerTimestamp
    val date: Date? = null
)
