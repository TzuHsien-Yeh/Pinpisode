package com.tzuhsien.immediat.network

import com.tzuhsien.immediat.BuildConfig
import com.tzuhsien.immediat.data.model.YouTubeResult
import com.tzuhsien.immediat.data.model.YouTubeSearchResult
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.Base64.getDecoder

private const val BASE_URL = "https://www.googleapis.com/youtube/v3/"
private const val ENDPOINT_VIDEOS = "videos"
private const val ENDPOINT_SEARCH = "search"

private val decodedBytes: ByteArray = getDecoder().decode(BuildConfig.encodedYtApiKey)
private val decodedYtApiKey = String(decodedBytes)

val loggingInterceptor =
    HttpLoggingInterceptor().setLevel(
        if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
        else HttpLoggingInterceptor.Level.NONE
    )

val client = OkHttpClient.Builder()
    .addInterceptor(loggingInterceptor)
    .addInterceptor { chain ->
        val url = chain
            .request()
            .url
            .newBuilder()
            .addQueryParameter("key", decodedYtApiKey)
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
    @GET(ENDPOINT_VIDEOS)
    suspend fun getVideoInfo(
        @Query("part") part: String,
        @Query("id") id: String
    ): YouTubeResult

    @GET(ENDPOINT_SEARCH)
    suspend fun getYouTubeSearchResult(
        @Query("part") part: String,
        @Query("type") type: String,
        @Query("maxResults") maxResult: Int?,
        @Query("q") query: String?
    ): YouTubeSearchResult

    @GET(ENDPOINT_VIDEOS)
    suspend fun getTrendingVideos(
        @Query("part") part: String,
        @Query("chart") chart: String,
        @Query("regionCode") regionCode: String?,
        @Query("maxResults") maxResult: Int?
    ): YouTubeResult

}

/**
 * A public Api object that exposes the lazy-initialized Retrofit service
 */
object YouTubeApi {
    val retrofitService: YouTubeApiService by lazy { retrofit.create(YouTubeApiService::class.java) }
}
