package com.aura.dashboard.data.news.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/** NewsData.io "latest news" endpoint (https://newsdata.io/documentation). */
interface NewsDataService {
    @GET("api/1/latest")
    suspend fun latest(
        @Query("apikey") apiKey: String?,
        @Query("country") country: String,
        @Query("category") category: String?,
        @Query("size") size: Int,
        @Query("page") page: String?,
        @Query("removeduplicate") removeDuplicate: Int = 1,
    ): Response<JsonObject>
}

@Serializable
data class NewsDataArticleDto(
    @SerialName("article_id") val articleId: String? = null,
    val title: String? = null,
    val link: String? = null,
    val description: String? = null,
    @SerialName("pubDate") val pubDate: String? = null,
    @SerialName("pubDateTZ") val pubDateTz: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("source_id") val sourceId: String? = null,
    @SerialName("source_name") val sourceName: String? = null,
    @SerialName("source_url") val sourceUrl: String? = null,
    val duplicate: Boolean? = null,
)
