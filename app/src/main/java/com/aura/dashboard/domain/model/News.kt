package com.aura.dashboard.domain.model

import java.time.Instant

enum class NewsCategory { TOP, LOCAL, WORLD, BUSINESS, TECHNOLOGY, SCIENCE, SPORTS, ENTERTAINMENT, HEALTH }

data class Article(
    val url: String,
    val title: String,
    val summary: String,
    val imageUrl: String?,
    /** Publisher name, always shown next to the article. */
    val sourceName: String,
    /** Publisher domain (e.g. "dnevnik.bg"), shown when opening the original article. */
    val sourceDomain: String?,
    /** Publication time as reported by the publisher; null when the feed omits it. */
    val publishedAt: Instant?,
)

enum class NewsError {
    OFFLINE,
    UNSUPPORTED_COUNTRY,
    UNAUTHORIZED,
    RATE_LIMITED,
    SERVER,
    INVALID_RESPONSE,
    UNKNOWN,
}

sealed interface NewsRefreshResult {
    data class Success(val articleCount: Int) : NewsRefreshResult
    data class Failure(val error: NewsError) : NewsRefreshResult
}

/** Describes where news for a country comes from, for transparency in the UI. */
data class NewsSourceInfo(
    val providerName: String,
    val publishers: List<String>,
    val categories: List<NewsCategory>,
    val isAvailable: Boolean,
)
