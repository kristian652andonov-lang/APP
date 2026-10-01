package com.aura.dashboard.data.news.rss

import com.aura.dashboard.data.news.NewsException
import com.aura.dashboard.data.news.NewsProvider
import com.aura.dashboard.domain.model.Article
import com.aura.dashboard.domain.model.NewsCategory
import com.aura.dashboard.domain.model.NewsError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runInterruptible
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.time.Instant
import java.util.Locale

/** Keyless provider that reads publishers' official RSS feeds (see [RssFeeds]). */
class RssNewsProvider(
    private val client: OkHttpClient,
    private val parser: RssParser,
    private val ioDispatcher: CoroutineDispatcher,
) : NewsProvider {

    override val displayName: String = "Publisher RSS feeds"

    override fun supports(countryCode: String): Boolean = RssFeeds.forCountry(countryCode).isNotEmpty()

    override fun categories(countryCode: String): List<NewsCategory> =
        NewsCategory.entries.filter { it in RssFeeds.forCountry(countryCode) }

    override fun publishers(countryCode: String): List<String> =
        RssFeeds.forCountry(countryCode).values.flatten().map { it.publisher }.distinct()

    override suspend fun fetch(countryCode: String, category: NewsCategory, limit: Int): List<Article> = coroutineScope {
        val sources = RssFeeds.forCountry(countryCode)[category]
            ?: throw NewsException(NewsError.UNSUPPORTED_COUNTRY)

        val results = sources
            .map { source -> async { runCatchingNonCancellation { fetchFeed(source) } } }
            .awaitAll()

        val successful = results.mapNotNull { it.getOrNull() }
        if (successful.isEmpty()) {
            val firstError = results.firstNotNullOfOrNull { it.exceptionOrNull() }
            throw firstError as? NewsException ?: NewsException(NewsError.UNKNOWN, firstError)
        }
        merge(successful, limit)
    }

    private suspend fun fetchFeed(source: FeedSource): List<Article> = runInterruptible(ioDispatcher) {
        val request = Request.Builder()
            .url(source.url)
            .header("Accept", "application/rss+xml, application/atom+xml, application/xml;q=0.9, text/xml;q=0.8")
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw NewsException(if (response.code >= 500) NewsError.SERVER else NewsError.INVALID_RESPONSE)
                }
                val body = response.body
                if (body.contentLength() > MAX_FEED_BYTES) throw NewsException(NewsError.INVALID_RESPONSE)
                body.byteStream().use { stream -> parser.parse(stream, source.publisher) }
            }
        } catch (io: IOException) {
            throw NewsException(NewsError.OFFLINE, io)
        } catch (xml: XmlPullParserException) {
            throw NewsException(NewsError.INVALID_RESPONSE, xml)
        }
    }

    private inline fun <T> runCatchingNonCancellation(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        Result.failure(error)
    }

    companion object {
        private const val MAX_FEED_BYTES = 5L * 1024 * 1024
        private val FUTURE_TOLERANCE: java.time.Duration = java.time.Duration.ofMinutes(10)

        /**
         * Newest first, without duplicate links or headlines (feeds often overlap). Items dated
         * in the future (scheduled posts) rank with undated ones at the end instead of on top.
         */
        internal fun merge(feeds: List<List<Article>>, limit: Int, now: Instant = Instant.now()): List<Article> {
            val seenUrls = HashSet<String>()
            val seenTitles = HashSet<String>()
            return feeds.flatten()
                .sortedWith(
                    compareByDescending(nullsFirst()) { article ->
                        article.publishedAt?.takeUnless { it.isAfter(now.plus(FUTURE_TOLERANCE)) }
                    },
                )
                .filter { article ->
                    val titleKey = article.title.lowercase(Locale.ROOT).filter(Char::isLetterOrDigit)
                    seenUrls.add(article.url) && seenTitles.add(titleKey)
                }
                .take(limit)
        }
    }
}
