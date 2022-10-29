package com.tzuhsien.immediat.network

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.tzuhsien.immediat.BuildConfig
import com.tzuhsien.immediat.ImMediAtApplication
import com.tzuhsien.immediat.data.model.YouTubeResult
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

private const val BASE_URL = "https://www.googleapis.com/youtube/v3/"

val loggingInterceptor =
    HttpLoggingInterceptor().setLevel(
        if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
        else HttpLoggingInterceptor.Level.NONE
    )

val appContext = ImMediAtApplication.applicationContext()
val ai: ApplicationInfo = appContext.packageManager
    .getApplicationInfo(appContext.packageName, PackageManager.GET_META_DATA)
val ytApiKey = ai.metaData["youtubeApiKey"].toString()

val client = OkHttpClient.Builder()
    .addInterceptor(loggingInterceptor)
    .addInterceptor { chain ->
        val url = chain
            .request()
            .url
            .newBuilder()
            .addQueryParameter("key", ytApiKey)
            .build()
        chain.proceed(chain.request().newBuilder().url(url).build())
    }
    .build()

/**
 * Use the Retrofit builder to build a retrofit object using a Gson converter.
 */
private val retrofit = Retrofit.Builder()
    .addConverterFactory(GsonConverterFactory.create())
    .baseUrl(BASE_URL)
    .client(client)
    .build()

interface YouTubeApiService {
    @GET("videos")
    suspend fun getVideoInfo(
        @Query("part") part: String,
        @Query("id") id: String
    ): YouTubeResult
}

/**
 * A public Api object that exposes the lazy-initialized Retrofit service
 */
object YouTubeApi {
    val retrofitService: YouTubeApiService by lazy { retrofit.create(YouTubeApiService::class.java) }
}
