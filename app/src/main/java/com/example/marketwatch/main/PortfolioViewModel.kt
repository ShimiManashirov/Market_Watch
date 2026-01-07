package com.example.marketwatch.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.marketwatch.network.ApiClient
import com.example.marketwatch.network.FinnhubQuote
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class Stock(val id: String, val symbol: String, val name: String)
data class PortfolioItem(val stock: Stock, val quote: FinnhubQuote? = null)

class PortfolioViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _portfolioItems = MutableStateFlow<List<PortfolioItem>>(emptyList())
    val portfolioItems = _portfolioItems.asStateFlow()

    init {
        fetchUserPortfolio()
    }

    private fun fetchUserPortfolio() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).collection("portfolio")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("PortfolioViewModel", "Listen failed.", error)
                    return@addSnapshotListener
                }

                val stockList = snapshot?.documents?.mapNotNull { doc ->
                    val symbol = doc.getString("symbol")
                    val name = doc.getString("name")
                    if (symbol != null && name != null) {
                        Stock(id = doc.id, symbol = symbol, name = name)
                    } else {
                        null
                    }
                } ?: emptyList()

                viewModelScope.launch {
                    _portfolioItems.value = stockList.map { PortfolioItem(it) } // Initial loading state
                    try {
                        val updatedItems = stockList.map { stock ->
                            async {
                                val quote = ApiClient.finnhubApi.getQuote(stock.symbol, ApiClient.API_KEY)
                                PortfolioItem(stock, quote)
                            }
                        }.awaitAll()
                        _portfolioItems.value = updatedItems
                    } catch (e: Exception) {
                        Log.e("PortfolioViewModel", "Error fetching quotes", e)
                        // You might want to update the UI to show an error state
                    }
                }
            }
    }

    fun addStock(symbol: String, name: String, onResult: (Boolean, String) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onResult(false, "User not logged in.")
            return
        }

        val portfolioCollection = db.collection("users").document(userId).collection("portfolio")

        portfolioCollection.whereEqualTo("symbol", symbol.uppercase()).get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    val newStock = hashMapOf("symbol" to symbol.uppercase(), "name" to name)
                    portfolioCollection.add(newStock)
                        .addOnSuccessListener { onResult(true, "'$name' has been added to your portfolio.") }
                        .addOnFailureListener { e -> onResult(false, "Failed to add stock: ${e.message}") }
                } else {
                    onResult(false, "'$name' is already in your portfolio.")
                }
            }
            .addOnFailureListener { e -> onResult(false, "Error checking portfolio: ${e.message}") }
    }

    fun removeStock(stockId: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("portfolio").document(stockId)
            .delete()
            .addOnSuccessListener { Log.d("PortfolioViewModel", "Stock removed successfully") }
            .addOnFailureListener { e -> Log.w("PortfolioViewModel", "Error removing stock", e) }
    }
}
