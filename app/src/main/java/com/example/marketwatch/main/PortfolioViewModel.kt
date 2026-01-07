package com.example.marketwatch.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.marketwatch.network.ApiClient
import com.example.marketwatch.util.CurrencyConverter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Represents a summary of all transactions for a single stock
data class Holding(
    val symbol: String,
    val name: String,
    val totalQuantity: Double,
    val averagePrice: Double,
    // Live data
    val currentPrice: Double? = null,
    val totalValue: Double? = null,
    val currencySymbol: String = "$"
)

class PortfolioViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _holdings = MutableStateFlow<List<Holding>>(emptyList())
    val holdings = _holdings.asStateFlow()

    private val _totalPortfolioValue = MutableStateFlow(0.0)
    val totalPortfolioValue = _totalPortfolioValue.asStateFlow()

    private var preferredCurrency = "USD"
    private var currencySymbol = "$"

    init {
        fetchUserPreferencesAndHoldings()
    }

    private fun fetchUserPreferencesAndHoldings() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get().addOnSuccessListener { userDoc ->
            val currencyPref = userDoc.getString("currency") ?: "USD - $ (US Dollar)"
            preferredCurrency = currencyPref.split(" ")[0]
            currencySymbol = currencyPref.split(" ")[2].replace("(", "").replace(")", "")
            fetchHoldings(userId)
        }
    }

    private fun fetchHoldings(userId: String) {
        db.collection("users").document(userId).collection("transactions")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("PortfolioViewModel", "Listen failed.", error)
                    return@addSnapshotListener
                }

                val transactions = snapshot?.toObjects(Transaction::class.java) ?: emptyList()
                val groupedBySymbol = transactions.groupBy { it.symbol }

                val calculatedHoldings = groupedBySymbol.map { (symbol, transactions) ->
                    val totalQuantity = transactions.sumOf { it.quantity }
                    val totalCost = transactions.sumOf { it.quantity * it.purchasePrice }
                    val averagePrice = if (totalQuantity > 0) totalCost / totalQuantity else 0.0
                    Holding(
                        symbol = symbol,
                        name = transactions.first().name,
                        totalQuantity = totalQuantity,
                        averagePrice = averagePrice,
                        currencySymbol = currencySymbol
                    )
                }.filter { it.totalQuantity > 0 }

                updateCurrentPrices(calculatedHoldings)
            }
    }

    private fun updateCurrentPrices(holdings: List<Holding>) {
        viewModelScope.launch {
            try {
                val updatedHoldings = holdings.map { holding ->
                    async {
                        val quote = ApiClient.finnhubApi.getQuote(holding.symbol, ApiClient.API_KEY)
                        val convertedPrice = quote.currentPrice?.let { 
                            CurrencyConverter.convert(it, "USD", preferredCurrency) 
                        }
                        val totalValue = convertedPrice?.let { it * holding.totalQuantity }
                        holding.copy(currentPrice = convertedPrice, totalValue = totalValue)
                    }
                }.awaitAll()

                _holdings.value = updatedHoldings
                _totalPortfolioValue.value = updatedHoldings.sumOf { it.totalValue ?: 0.0 }

            } catch (e: Exception) {
                Log.e("PortfolioViewModel", "Error fetching current prices", e)
            }
        }
    }
}
