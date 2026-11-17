package dev.master.lumio.data

import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface CatalogApi {
    @GET("health") suspend fun health(): Health
    @GET("movies") suspend fun movies(
        @Query("q") query: String? = null,
        @Query("genre") genre: String? = null,
        @Query("sort") sort: String = "title",
        @Query("order") order: String = "asc",
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
    ): MoviePage
    @GET("movies/{id}") suspend fun movie(@Path("id") id: String): Movie
    @GET("genres") suspend fun genres(): Genres
    // Streaming decoding avoids buffering the full JSON response as a String.
    @GET("catalog") suspend fun catalog(): Catalog
}

object ApiClient {
    private val json = Json { ignoreUnknownKeys = true }
    fun create(baseUrl: String): CatalogApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            // No HTTP disk cache: catalog data remains online-only.
            .build()
        return Retrofit.Builder().baseUrl(baseUrl).client(client)
            .addConverterFactory(CatalogStreamConverter(json))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build().create(CatalogApi::class.java)
    }
}
