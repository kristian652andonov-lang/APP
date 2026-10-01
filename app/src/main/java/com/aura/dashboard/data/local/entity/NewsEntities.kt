package com.aura.dashboard.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.aura.dashboard.domain.model.Article
import java.time.Instant

@Entity(
    tableName = "news_articles",
    primaryKeys = ["country_code", "category", "url"],
)
data class NewsArticleEntity(
    @ColumnInfo(name = "country_code") val countryCode: String,
    val category: String,
    val url: String,
    val position: Int,
    val title: String,
    val summary: String,
    @ColumnInfo(name = "image_url") val imageUrl: String?,
    @ColumnInfo(name = "source_name") val sourceName: String,
    @ColumnInfo(name = "source_domain") val sourceDomain: String?,
    @ColumnInfo(name = "published_at") val publishedAt: Long?,
    @ColumnInfo(name = "fetched_at") val fetchedAt: Long,
)

/** Remembers when a (country, category) pair was last fetched successfully, even if empty. */
@Entity(tableName = "news_fetch_state", primaryKeys = ["country_code", "category"])
data class NewsFetchStateEntity(
    @ColumnInfo(name = "country_code") val countryCode: String,
    val category: String,
    @ColumnInfo(name = "fetched_at") val fetchedAt: Long,
)

fun NewsArticleEntity.toDomain() = Article(
    url = url,
    title = title,
    summary = summary,
    imageUrl = imageUrl,
    sourceName = sourceName,
    sourceDomain = sourceDomain,
    publishedAt = publishedAt?.let(Instant::ofEpochMilli),
)
