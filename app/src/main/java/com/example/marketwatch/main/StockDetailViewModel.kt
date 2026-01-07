package com.example.marketwatch.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.marketwatch.network.ApiClient
import com.example.marketwatch.network.FinnhubCompanyProfile
import com.example.marketwatch.network.FinnhubQuote
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface StockDetailUiState {
    object Loading : StockDetailUiState
    data class Success(val quote: FinnhubQuote, val profile: FinnhubCompanyProfile) : StockDetailUiState
    data class Error(val message: String) : StockDetailUiState
}

class StockDetailViewModel(private val stockSymbol: String) : ViewModel() {

    private val _uiState = MutableStateFlow<StockDetailUiState>(StockDetailUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        fetchStockData()
    }

    fun fetchStockData() {
        viewModelScope.launch {
            _uiState.value = StockDetailUiState.Loading
            try {
                val quoteDeferred = viewModelScope.async { ApiClient.finnhubApi.getQuote(stockSymbol, ApiClient.API_KEY) }
                val profileDeferred = viewModelScope.async { ApiClient.finnhubApi.getCompanyProfile(stockSymbol, ApiClient.API_KEY) }

                val quote = quoteDeferred.await()
                val profile = profileDeferred.await()

                _uiState.value = StockDetailUiState.Success(quote, profile)
            } catch (e: Exception) {
                _uiState.value = StockDetailUiState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }
}

class StockDetailViewModelFactory(private val stockSymbol: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StockDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StockDetailViewModel(stockSymbol) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
