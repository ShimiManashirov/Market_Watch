package com.example.marketwatch.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.marketwatch.auth.AuthViewModel
import com.example.marketwatch.network.ApiClient
import com.example.marketwatch.network.FinnhubQuote
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WatchlistItem(val symbol: String, val name: String? = null, val quote: FinnhubQuote? = null)

class WatchlistViewModel(authViewModel: AuthViewModel) : ViewModel() {

    val watchlistItems: StateFlow<List<WatchlistItem>> = authViewModel.watchlist
        .flatMapLatest { symbols ->
            val itemsFlow = MutableStateFlow(symbols.map { WatchlistItem(it) })
            viewModelScope.launch {
                val updatedItems = symbols.map { symbol ->
                    async {
                        try {
                            val profileDeferred = async { ApiClient.finnhubApi.getCompanyProfile(symbol, ApiClient.API_KEY) }
                            val quoteDeferred = async { ApiClient.finnhubApi.getQuote(symbol, ApiClient.API_KEY) }
                            val profile = profileDeferred.await()
                            val quote = quoteDeferred.await()
                            WatchlistItem(symbol, profile.name, quote)
                        } catch (e: Exception) {
                            WatchlistItem(symbol, null, null) // In case of error, return item without details
                        }
                    }
                }.awaitAll()
                itemsFlow.value = updatedItems
            }
            itemsFlow
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}
