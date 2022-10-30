package com.mindyhsu.minmap

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET

private const val API_KEY = "***REMOVED***"
private const val ORIGIN = "台北市大安區新生南路一段10652忠孝新生站"
private const val DESTINATION = "台北市中正區仁愛路二段AppWorks+School"
private const val DIRECTIONS_URL = "https://maps.googleapis.com/maps/api/directions/json?origin=%E5%8F%B0%E5%8C%97%E5%B8%82%E5%A4%A7%E5%AE%89%E5%8D%80%E6%96%B0%E7%94%9F%E5%8D%97%E8%B7%AF%E4%B8%80%E6%AE%B510652%E5%BF%A0%E5%AD%9D%E6%96%B0%E7%94%9F%E7%AB%99&destination=%E5%8F%B0%E5%8C%97%E5%B8%82%E4%B8%AD%E6%AD%A3%E5%8D%80%E4%BB%81%E6%84%9B%E8%B7%AF%E4%BA%8C%E6%AE%B5AppWorks+School&key=***REMOVED***"
//    "https://maps.googleapis.com/maps/api/directions/json?origin=${ORIGIN}&destination=${DESTINATION}&key=$API_KEY/"

private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

// Log request data
private val logging: HttpLoggingInterceptor =
    HttpLoggingInterceptor().setLevel(
        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    )
private val client = OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
    val newRequest = chain.request().newBuilder()
        .addHeader("Content-Type", "application/json")
        .build()

    chain.proceed(newRequest)
}).addInterceptor(logging).build()

// let Retrofit use Moshi to convert Json into Kotlin Objects
private val retrofit = Retrofit.Builder().addConverterFactory(MoshiConverterFactory.create(moshi))
    .addCallAdapterFactory(CoroutineCallAdapterFactory())
    .baseUrl(DIRECTIONS_URL).client(client).build()

interface MinMapApiService {
    @GET(".")
    suspend fun getDirection()
}

object MinMapApi {
    val retrofitService: MinMapApiService by lazy {
        retrofit.create(MinMapApiService::class.java)
    }
}