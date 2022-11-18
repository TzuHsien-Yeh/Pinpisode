package com.tzuhsien.immediat.network

import com.tzuhsien.immediat.data.model.EpisodeResult
import com.tzuhsien.immediat.data.model.SpotifySearchResult
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

private const val SPOTIFY_BASE_URL = "https://api.spotify.com/v1/"
private const val ENDPOINT_SEARCH = "search"
private const val ENDPOINT_EPISODES = "episodes"

val spotifyClient = OkHttpClient.Builder()
    .addInterceptor(loggingInterceptor)
    .addInterceptor { chain ->
        val url = chain
            .request()
            .url
            .newBuilder()
            .build()
        chain.proceed(chain.request().newBuilder().url(url).build())
    }
    .build()
/**
 * Use the Retrofit builder to build a retrofit object using a Gson converter.
 */
private val retrofit = Retrofit.Builder()
    .addConverterFactory(GsonConverterFactory.create())
    .baseUrl(SPOTIFY_BASE_URL)
    .client(spotifyClient)
    .build()

interface SpotifyApiService {

    @GET("$ENDPOINT_EPISODES/{id}")
    suspend fun getPodcastInfo(
        @Path("id") id: String,
        @Header("Authorization") bearerWithToken: String
    ): EpisodeResult

    @GET(ENDPOINT_SEARCH)
    suspend fun searchOnSpotify(
        @Header("Authorization") bearerWithToken: String,
        @Query("limit") limit: Int,
        @Query("type") type: String,
        @Query("q") query: String
    ): SpotifySearchResult

}

object SpotifyApi {
    val retrofitService: SpotifyApiService by lazy { retrofit.create(SpotifyApiService::class.java) }
}