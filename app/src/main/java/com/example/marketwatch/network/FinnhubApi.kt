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

    @GET("company-news")
    suspend fun getCompanyNews(
        @Query("symbol") symbol: String,
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("token") token: String
    ): List<CompanyNews>

    @GET("forex/rates")
    suspend fun getForexRates(
        @Query("base") base: String,
        @Query("token") token: String
    ): ForexRates
}

@Serializable
data class ForexRates(
    val base: String? = null,
    val quote: Map<String, Double> = emptyMap()
)

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
data class CompanyNews(
    val category: String? = null,
    val datetime: Long? = null,
    val headline: String? = null,
    val id: Int? = null,
    val image: String? = null,
    val related: String? = null,
    val source: String? = null,
    val summary: String? = null,
    val url: String? = null
)
