package com.aura.dashboard.data.news

import com.aura.dashboard.domain.model.Article
import com.aura.dashboard.domain.model.NewsCategory
import com.aura.dashboard.domain.model.NewsError

/** A source of real, attributable news articles. */
interface NewsProvider {
    val displayName: String

    fun supports(countryCode: String): Boolean
    fun categories(countryCode: String): List<NewsCategory>
    fun publishers(countryCode: String): List<String>

    /** @throws NewsException on any failure. */
    suspend fun fetch(countryCode: String, category: NewsCategory, limit: Int): List<Article>
}

class NewsException(val error: NewsError, cause: Throwable? = null) : Exception(error.name, cause)
