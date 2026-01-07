package com.example.marketwatch.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.marketwatch.network.ApiClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Represents a summary of all transactions for a single stock
data class Holding(
    val symbol: String,
    val name: String,
    val totalQuantity: Double,
    val averagePrice: Double,
    // Live data
    val currentPrice: Double? = null,
    val totalValue: Double? = null
)

class PortfolioViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _holdings = MutableStateFlow<List<Holding>>(emptyList())
    val holdings = _holdings.asStateFlow()

    private val _totalPortfolioValue = MutableStateFlow(0.0)
    val totalPortfolioValue = _totalPortfolioValue.asStateFlow()

    init {
        fetchHoldings()
    }

    private fun fetchHoldings() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).collection("transactions")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("PortfolioViewModel", "Listen failed.", error)
                    return@addSnapshotListener
                }

                val transactions = snapshot?.toObjects(Transaction::class.java) ?: emptyList()

                viewModelScope.launch {
                    // Group transactions by stock symbol
                    val groupedBySymbol = transactions.groupBy { it.symbol }

                    // Calculate holdings from transactions
                    val calculatedHoldings = groupedBySymbol.map { (symbol, transactions) ->
                        val totalQuantity = transactions.sumOf { it.quantity } // Assuming only buys for now
                        val totalCost = transactions.sumOf { it.quantity * it.purchasePrice }
                        val averagePrice = if (totalQuantity > 0) totalCost / totalQuantity else 0.0
                        Holding(
                            symbol = symbol,
                            name = transactions.first().name,
                            totalQuantity = totalQuantity,
                            averagePrice = averagePrice
                        )
                    }.filter { it.totalQuantity > 0 } // Filter out fully sold stocks

                    updateCurrentPrices(calculatedHoldings)
                }
            }
    }

    private fun updateCurrentPrices(holdings: List<Holding>) {
        viewModelScope.launch {
            try {
                val updatedHoldings = holdings.map { holding ->
                    async {
                        val quote = ApiClient.finnhubApi.getQuote(holding.symbol, ApiClient.API_KEY)
                        val currentPrice = quote.currentPrice
                        val totalValue = currentPrice?.let { it * holding.totalQuantity }
                        holding.copy(currentPrice = currentPrice, totalValue = totalValue)
                    }
                }.awaitAll()

                _holdings.value = updatedHoldings
                _totalPortfolioValue.value = updatedHoldings.sumOf { it.totalValue ?: 0.0 }

            } catch (e: Exception) {
                Log.e("PortfolioViewModel", "Error fetching current prices", e)
                // If price fetching fails, show holdings without live data
                _holdings.value = holdings
            }
        }
    }
}
