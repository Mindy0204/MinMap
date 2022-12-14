package com.mindyhsu.minmap.network

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.mindyhsu.minmap.BuildConfig
import com.mindyhsu.minmap.data.MapDirection
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

private const val DIRECTIONS_URL = "https://maps.googleapis.com/maps/api/directions/"
private const val HEADER_NAME = "Content-Type"
private const val HEADER_VALUE = "application/json"

private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

// Log request data
private val logging: HttpLoggingInterceptor =
    HttpLoggingInterceptor().setLevel(
        if (BuildConfig.TIMBER_VISIABLE) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    )
private val client = OkHttpClient.Builder().addInterceptor(
    Interceptor { chain ->
        val newRequest = chain.request().newBuilder()
            .addHeader(HEADER_NAME, HEADER_VALUE)
            .build()

        chain.proceed(newRequest)
    }
).addInterceptor(logging).build()

// let Retrofit use Moshi to convert Json into Kotlin Objects
private val retrofit = Retrofit.Builder().addConverterFactory(MoshiConverterFactory.create(moshi))
    .addCallAdapterFactory(CoroutineCallAdapterFactory())
    .baseUrl(DIRECTIONS_URL).client(client).build()

interface MinMapApiService {
    @GET("json")
    suspend fun getDirection(
        @Query("origin") startLocation: String,
        @Query("destination") endLocation: String,
        @Query("mode") mode: String,
        @Query("key") apiKey: String
    ): MapDirection
}

object MinMapApi {
    val retrofitService: MinMapApiService by lazy {
        retrofit.create(MinMapApiService::class.java)
    }
}
