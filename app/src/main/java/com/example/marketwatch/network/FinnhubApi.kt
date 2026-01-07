package com.example.marketwatch.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface FinnhubApi {
    @GET("quote")
    suspend fun getQuote(
        @Query("symbol") symbol: String,
        @Query("token") token: String
    ): FinnhubQuote

    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("token") token: String
    ): FinnhubSearchResult

    @GET("stock/profile2")
    suspend fun getCompanyProfile(
        @Query("symbol") symbol: String,
        @Query("token") token: String
    ): FinnhubCompanyProfile

    @GET("stock/earnings")
    suspend fun getEarningsCalendar(
        @Query("symbol") symbol: String,
        @Query("token") token: String
    ): List<EarningRelease>
}

@Serializable
data class FinnhubQuote(
    @SerialName("c")
    val currentPrice: Double? = null,
    @SerialName("d")
    val change: Double? = null,
    @SerialName("dp")
    val percentChange: Double? = null,
    @SerialName("h")
    val highPrice: Double? = null,
    @SerialName("l")
    val lowPrice: Double? = null,
    @SerialName("o")
    val openPrice: Double? = null,
    @SerialName("pc")
    val previousClose: Double? = null,
)

@Serializable
data class FinnhubSearchResult(
    val count: Int,
    val result: List<FinnhubSymbol>
)

@Serializable
data class FinnhubSymbol(
    val description: String,
    val displaySymbol: String,
    val symbol: String,
    val type: String
)

@Serializable
data class FinnhubCompanyProfile(
    val country: String? = null,
    val currency: String? = null,
    val exchange: String? = null,
    val ipo: String? = null,
    val marketCapitalization: Double? = null,
    val name: String? = null,
    val phone: String? = null,
    val shareOutstanding: Double? = null,
    val ticker: String? = null,
    val weburl: String? = null,
    val logo: String? = null,
    val finnhubIndustry: String? = null
)

@Serializable
data class EarningRelease(
    val date: String? = null,
    val epsActual: Double? = null,
    val epsEstimate: Double? = null,
    val hour: String? = null,
    val quarter: Int? = null,
    val revenueActual: Double? = null,
    val revenueEstimate: Double? = null,
    val symbol: String? = null,
    val year: Int? = null
)
