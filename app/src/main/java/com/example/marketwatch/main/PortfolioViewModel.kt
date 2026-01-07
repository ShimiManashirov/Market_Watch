package com.example.marketwatch.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.marketwatch.auth.AuthViewModel
import com.example.marketwatch.network.ApiClient
import com.example.marketwatch.util.CurrencyConverter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class Holding(
    val symbol: String,
    val name: String,
    val totalQuantity: Double,
    val averagePrice: Double,
    val currentPrice: Double? = null,
    val totalValue: Double? = null,
    val currencySymbol: String = "$"
)

class PortfolioViewModel(private val authViewModel: AuthViewModel) : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _holdings = MutableStateFlow<List<Holding>>(emptyList())
    val holdings = _holdings.asStateFlow()

    private val _totalPortfolioValue = MutableStateFlow(0.0)
    val totalPortfolioValue = _totalPortfolioValue.asStateFlow()

    private var preferredCurrency = "USD"
    private var currencySymbol = "$"

    init {
        viewModelScope.launch {
            // React to changes in user preferences, specifically the currency
            authViewModel.userPreferences.collectLatest { preferences ->
                val currencyPref = preferences["currency"] as? String ?: "USD - $ (US Dollar)"
                preferredCurrency = currencyPref.split(" ")[0]
                currencySymbol = currencyPref.split(" ")[2].replace("(", "").replace(")", "")
                // Re-fetch and re-calculate holdings whenever currency changes
                fetchHoldings()
            }
        }
    }

    private fun fetchHoldings() {
        val userId = authViewModel.currentUser.value?.uid ?: return

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

class PortfolioViewModelFactory(private val authViewModel: AuthViewModel) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PortfolioViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PortfolioViewModel(authViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
