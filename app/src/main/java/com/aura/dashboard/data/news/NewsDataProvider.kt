package com.aura.dashboard.data.news

import com.aura.dashboard.core.util.HtmlText
import com.aura.dashboard.data.news.api.NewsDataArticleDto
import com.aura.dashboard.data.news.api.NewsDataService
import com.aura.dashboard.data.news.rss.RssParser
import com.aura.dashboard.domain.model.Article
import com.aura.dashboard.domain.model.NewsCategory
import com.aura.dashboard.domain.model.NewsError
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.io.IOException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** NewsData.io provider: supports every country in [Countries] and all categories. */
class NewsDataProvider(
    private val service: NewsDataService,
    private val config: NewsApiConfig,
    private val json: Json,
) : NewsProvider {

    override val displayName: String = "NewsData.io"

    override fun supports(countryCode: String): Boolean = config.isConfigured

    override fun categories(countryCode: String): List<NewsCategory> = NewsCategory.entries

    override fun publishers(countryCode: String): List<String> = emptyList()

    override suspend fun fetch(countryCode: String, category: NewsCategory, limit: Int): List<Article> {
        val articles = LinkedHashMap<String, Article>()
        var page: String? = null
        var requests = 0
        // The free plan returns at most 10 results per request; page until we have enough.
        while (articles.size < limit && requests < MAX_REQUESTS) {
            val body = request(countryCode, category, page)
            requests++
            val results = body["results"] as? JsonArray ?: throw NewsException(NewsError.INVALID_RESPONSE)
            results.forEach { element ->
                val dto = runCatching { json.decodeFromJsonElement(NewsDataArticleDto.serializer(), element) }.getOrNull()
                dto?.toArticle()?.let { articles.putIfAbsent(it.url, it) }
            }
            page = (body["nextPage"] as? JsonPrimitive)?.contentOrNull
            if (page.isNullOrBlank() || results.isEmpty()) break
        }
        return articles.values.take(limit)
    }

    private suspend fun request(countryCode: String, category: NewsCategory, page: String?): JsonObject {
        val response = try {
            service.latest(
                apiKey = config.apiKey.takeIf { it.isNotBlank() },
                country = countryCode.lowercase(),
                category = category.apiValue(),
                size = PAGE_SIZE,
                page = page,
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (io: IOException) {
            throw NewsException(NewsError.OFFLINE, io)
        } catch (serialization: SerializationException) {
            throw NewsException(NewsError.INVALID_RESPONSE, serialization)
        } catch (other: RuntimeException) {
            throw NewsException(NewsError.UNKNOWN, other)
        }

        if (!response.isSuccessful) {
            throw NewsException(
                when (response.code()) {
                    401, 403 -> NewsError.UNAUTHORIZED
                    429 -> NewsError.RATE_LIMITED
                    in 500..599 -> NewsError.SERVER
                    else -> NewsError.INVALID_RESPONSE
                },
            )
        }
        val body = response.body() ?: throw NewsException(NewsError.INVALID_RESPONSE)
        val status = (body["status"] as? JsonPrimitive)?.contentOrNull
        if (status != "success") throw NewsException(NewsError.INVALID_RESPONSE)
        return body
    }

    private fun NewsDataArticleDto.toArticle(): Article? {
        if (duplicate == true) return null
        val url = RssParser.normalizeUrl(link) ?: return null
        val cleanTitle = title?.let(HtmlText::toPlainText)?.takeIf { it.isNotBlank() } ?: return null
        return Article(
            url = url,
            title = cleanTitle,
            summary = HtmlText.truncate(HtmlText.toPlainText(description.orEmpty()), 280),
            imageUrl = RssParser.normalizeImageUrl(imageUrl),
            sourceName = sourceName?.takeIf { it.isNotBlank() } ?: sourceId ?: RssParser.domainOf(url).orEmpty(),
            sourceDomain = RssParser.domainOf(sourceUrl ?: url),
            publishedAt = parsePubDate(pubDate, pubDateTz),
        )
    }

    private fun NewsCategory.apiValue(): String = when (this) {
        NewsCategory.TOP -> "top"
        NewsCategory.LOCAL -> "domestic"
        NewsCategory.WORLD -> "world"
        NewsCategory.BUSINESS -> "business"
        NewsCategory.TECHNOLOGY -> "technology"
        NewsCategory.SCIENCE -> "science"
        NewsCategory.SPORTS -> "sports"
        NewsCategory.ENTERTAINMENT -> "entertainment"
        NewsCategory.HEALTH -> "health"
    }

    companion object {
        private const val PAGE_SIZE = 10
        private const val MAX_REQUESTS = 5
        private val PUB_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        /** NewsData returns "yyyy-MM-dd HH:mm:ss" in the zone given by pubDateTZ (normally UTC). */
        internal fun parsePubDate(value: String?, zone: String?): Instant? {
            if (value.isNullOrBlank()) return null
            val zoneId = runCatching { ZoneId.of(zone?.takeIf { it.isNotBlank() } ?: "UTC") }.getOrDefault(ZoneId.of("UTC"))
            return runCatching { LocalDateTime.parse(value.trim(), PUB_DATE_FORMAT).atZone(zoneId).toInstant() }
                .getOrElse { RssParser.parseDate(value) }
        }
    }
}
