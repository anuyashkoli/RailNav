package com.app.railnav.data.remote

import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

object RetrofitClient {

    private const val BASE_URL = "https://irctc-api2.p.rapidapi.com/"

    // 1. JSON Configuration (Lenient to prevent crashes on bad data)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // 2. HTTP Client (Adds logging so you can see requests in Logcat)
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    // 3. The Retrofit Instance
    val api: TrainApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TrainApi::class.java)
    }
}