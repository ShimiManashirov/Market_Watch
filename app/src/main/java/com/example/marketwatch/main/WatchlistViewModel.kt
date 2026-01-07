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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WatchlistItem(val symbol: String, val quote: FinnhubQuote? = null)

class WatchlistViewModel(authViewModel: AuthViewModel) : ViewModel() {

    val watchlistItems: StateFlow<List<WatchlistItem>> = authViewModel.watchlist
        .flatMapLatest { symbols ->
            val itemsFlow = MutableStateFlow(symbols.map { WatchlistItem(it) })
            viewModelScope.launch {
                val updatedItems = symbols.map {
                    async {
                        try {
                            val quote = ApiClient.finnhubApi.getQuote(it, ApiClient.API_KEY)
                            WatchlistItem(it, quote)
                        } catch (e: Exception) {
                            WatchlistItem(it, null) // In case of error, return item without quote
                        }
                    }
                }.awaitAll()
                itemsFlow.value = updatedItems
            }
            itemsFlow
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}
