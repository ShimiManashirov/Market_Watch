package com.example.marketwatch.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.marketwatch.network.ApiClient
import com.example.marketwatch.network.FinnhubSymbol
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SearchUiState {
    object Idle : SearchUiState
    object Loading : SearchUiState
    data class Success(val results: List<FinnhubSymbol>) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

class SearchViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun search(query: String) {
        if (query.isBlank()) {
            _uiState.value = SearchUiState.Idle
            return
        }

        viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            try {
                val searchResult = ApiClient.finnhubApi.search(query, ApiClient.API_KEY)
                _uiState.value = SearchUiState.Success(searchResult.result)
            } catch (e: Exception) {
                _uiState.value = SearchUiState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }
}
