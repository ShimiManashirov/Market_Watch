package com.example.marketwatch.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.marketwatch.network.ApiClient
import com.example.marketwatch.network.CompanyNews
import com.example.marketwatch.network.FinnhubCompanyProfile
import com.example.marketwatch.network.FinnhubQuote
import com.example.marketwatch.util.CurrencyConverter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed interface StockDetailUiState {
    object Loading : StockDetailUiState
    data class Success(
        val quote: FinnhubQuote,
        val profile: FinnhubCompanyProfile,
        val news: List<CompanyNews>,
        val convertedPrice: Double,
        val currencySymbol: String
    ) : StockDetailUiState
    data class Error(val message: String) : StockDetailUiState
}

class StockDetailViewModel(application: Application, private val stockSymbol: String) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<StockDetailUiState>(StockDetailUiState.Loading)
    val uiState = _uiState.asStateFlow()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    init {
        fetchStockData()
    }

    private fun fetchStockData() {
        viewModelScope.launch {
            _uiState.value = StockDetailUiState.Loading
            try {
                val userId = auth.currentUser?.uid
                val userDoc = userId?.let { db.collection("users").document(it).get().await() }
                val preferredCurrency = userDoc?.getString("currency") ?: "USD - $ (US Dollar)"
                val currencyCode = preferredCurrency.split(" ")[0]
                val currencySymbol = preferredCurrency.split(" ")[2].replace("(", "").replace(")", "")

                val quoteDeferred = viewModelScope.async { ApiClient.finnhubApi.getQuote(stockSymbol, ApiClient.API_KEY) }
                val profileDeferred = viewModelScope.async { ApiClient.finnhubApi.getCompanyProfile(stockSymbol, ApiClient.API_KEY) }
                val newsDeferred = viewModelScope.async { 
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val to = Calendar.getInstance()
                    val from = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
                    ApiClient.finnhubApi.getCompanyNews(stockSymbol, dateFormat.format(from.time), dateFormat.format(to.time), ApiClient.API_KEY) 
                }

                val quote = quoteDeferred.await()
                val profile = profileDeferred.await()
                val news = newsDeferred.await()

                val convertedPrice = CurrencyConverter.convert(quote.currentPrice ?: 0.0, "USD", currencyCode)

                _uiState.value = StockDetailUiState.Success(quote, profile, news, convertedPrice, currencySymbol)
            } catch (e: Exception) {
                _uiState.value = StockDetailUiState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    fun addTransaction(symbol: String, name: String, quantity: Double, price: Double, onResult: (Boolean, String) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onResult(false, "User not logged in.")
            return
        }

        val transaction = Transaction(
            symbol = symbol,
            name = name,
            quantity = quantity,
            purchasePrice = price,
            date = Date()
        )

        db.collection("users").document(userId).collection("transactions")
            .add(transaction)
            .addOnSuccessListener { onResult(true, "Transaction added successfully.") }
            .addOnFailureListener { e -> onResult(false, "Failed to add transaction: ${e.message}") }
    }
}

class StockDetailViewModelFactory(private val application: Application, private val stockSymbol: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StockDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StockDetailViewModel(application, stockSymbol) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
