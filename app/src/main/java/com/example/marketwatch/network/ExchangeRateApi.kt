package com.example.marketwatch.network

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

@Serializable
data class ExchangeRatesResponse(
    val base: String,
    val rates: Map<String, Double>
)

interface ExchangeRateApi {
    @GET("latest")
    suspend fun getLatestRates(@Query("from") base: String): ExchangeRatesResponse
}
