package com.example.marketwatch.util

import com.example.marketwatch.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

object CurrencyConverter {

    private val rates = ConcurrentHashMap<String, Double>()

    suspend fun updateRates() {
        withContext(Dispatchers.IO) {
            try {
                val response = ApiClient.exchangeRateApi.getLatestRates("USD")
                rates.clear()
                rates.putAll(response.rates)
                rates["USD"] = 1.0 // Ensure base currency is always present
            } catch (e: Exception) {
                // Handle error, maybe log it or use fallback rates
            }
        }
    }

    fun convert(amount: Double, fromCurrency: String, toCurrency: String): Double {
        val fromRate = rates[fromCurrency.uppercase()] ?: return amount
        val toRate = rates[toCurrency.uppercase()] ?: return amount
        
        val amountInUsd = amount / fromRate
        return amountInUsd * toRate
    }
}
