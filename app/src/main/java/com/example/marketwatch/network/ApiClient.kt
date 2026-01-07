package com.example.marketwatch.network

import com.example.marketwatch.BuildConfig
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val FINNHUB_BASE_URL = "https://finnhub.io/api/v1/"
    private const val FRANKFURTER_BASE_URL = "https://api.frankfurter.app/"
    val API_KEY = BuildConfig.FINNHUB_API_KEY

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val finnhubRetrofit = Retrofit.Builder()
        .baseUrl(FINNHUB_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val frankfurterRetrofit = Retrofit.Builder()
        .baseUrl(FRANKFURTER_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val finnhubApi: FinnhubApi = finnhubRetrofit.create(FinnhubApi::class.java)
    val exchangeRateApi: ExchangeRateApi = frankfurterRetrofit.create(ExchangeRateApi::class.java)
}
