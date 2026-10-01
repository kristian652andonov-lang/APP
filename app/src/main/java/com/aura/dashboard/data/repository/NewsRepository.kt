package com.aura.dashboard.data.repository

import com.aura.dashboard.core.network.ConnectivityObserver
import com.aura.dashboard.core.time.AppClock
import com.aura.dashboard.data.local.AuraDatabase
import com.aura.dashboard.data.local.entity.NewsArticleEntity
import com.aura.dashboard.data.local.entity.toDomain
import com.aura.dashboard.data.news.NewsException
import com.aura.dashboard.data.news.NewsProvider
import com.aura.dashboard.domain.model.Article
import com.aura.dashboard.domain.model.NewsCategory
import com.aura.dashboard.domain.model.NewsError
import com.aura.dashboard.domain.model.NewsRefreshResult
import com.aura.dashboard.domain.model.NewsSourceInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

interface NewsRepository {
    /** Cached articles; emits immediately (offline) and again after every refresh. */
    fun observeArticles(countryCode: String, category: NewsCategory): Flow<List<Article>>
    fun observeLastUpdated(countryCode: String, category: NewsCategory): Flow<Instant?>
    suspend fun refresh(countryCode: String, category: NewsCategory, limit: Int): NewsRefreshResult

    /** Refreshes only if the cache is older than [maxAge]. */
    suspend fun refreshIfStale(countryCode: String, category: NewsCategory, limit: Int, maxAge: Duration): NewsRefreshResult?
    fun sourceInfo(countryCode: String): NewsSourceInfo
    suspend fun pruneOtherCountries(countryCode: String)
}

@Singleton
class CachedNewsRepository @Inject constructor(
    database: AuraDatabase,
    private val providerChain: NewsProviderChain,
    private val connectivity: ConnectivityObserver,
    private val clock: AppClock,
) : NewsRepository {

    private val dao = database.newsDao()
    private val refreshLocks = ConcurrentHashMap<String, Mutex>()

    override fun observeArticles(countryCode: String, category: NewsCategory): Flow<List<Article>> =
        dao.observeArticles(countryCode, category.name).map { list -> list.map { it.toDomain() } }

    override fun observeLastUpdated(countryCode: String, category: NewsCategory): Flow<Instant?> =
        dao.observeFetchedAt(countryCode, category.name).map { it?.let(Instant::ofEpochMilli) }

    override suspend fun refresh(countryCode: String, category: NewsCategory, limit: Int): NewsRefreshResult {
        val key = "$countryCode/${category.name}"
        return refreshLocks.getOrPut(key) { Mutex() }.withLock { refreshLocked(countryCode, category, limit) }
    }

    override suspend fun refreshIfStale(
        countryCode: String,
        category: NewsCategory,
        limit: Int,
        maxAge: Duration,
    ): NewsRefreshResult? {
        val fetchedAt = dao.getFetchedAt(countryCode, category.name)?.let(Instant::ofEpochMilli)
        val fresh = fetchedAt != null && Duration.between(fetchedAt, clock.now()) < maxAge
        return if (fresh) null else refresh(countryCode, category, limit)
    }

    private suspend fun refreshLocked(countryCode: String, category: NewsCategory, limit: Int): NewsRefreshResult {
        val providers = providersFor(countryCode, category)
        if (providers.isEmpty()) return NewsRefreshResult.Failure(NewsError.UNSUPPORTED_COUNTRY)
        if (!connectivity.isOnline.value) return NewsRefreshResult.Failure(NewsError.OFFLINE)

        var lastError = NewsError.UNKNOWN
        for (provider in providers) {
            try {
                val articles = provider.fetch(countryCode, category, limit)
                val now = clock.now().toEpochMilli()
                dao.replace(
                    country = countryCode,
                    category = category.name,
                    articles = articles.mapIndexed { index, article ->
                        NewsArticleEntity(
                            countryCode = countryCode,
                            category = category.name,
                            url = article.url,
                            position = index,
                            title = article.title,
                            summary = article.summary,
                            imageUrl = article.imageUrl,
                            sourceName = article.sourceName,
                            sourceDomain = article.sourceDomain,
                            publishedAt = article.publishedAt?.toEpochMilli(),
                            fetchedAt = now,
                        )
                    },
                    fetchedAt = now,
                )
                return NewsRefreshResult.Success(articles.size)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (news: NewsException) {
                lastError = news.error
            } catch (error: Exception) {
                // Database or unexpected errors must never crash the screen.
                lastError = NewsError.UNKNOWN
            }
        }
        return NewsRefreshResult.Failure(lastError)
    }

    /** The configured API first (if any), then publisher RSS feeds as a fallback. */
    private fun providersFor(countryCode: String, category: NewsCategory): List<NewsProvider> =
        providerChain.providers
            .filter { it.supports(countryCode) && category in it.categories(countryCode) }

    override fun sourceInfo(countryCode: String): NewsSourceInfo {
        val providers = providerChain.providers.filter { it.supports(countryCode) }
        val main = providers.firstOrNull()
        return NewsSourceInfo(
            providerName = main?.displayName.orEmpty(),
            publishers = main?.publishers(countryCode).orEmpty(),
            categories = NewsCategory.entries.filter { category -> providers.any { category in it.categories(countryCode) } },
            isAvailable = main != null,
        )
    }

    override suspend fun pruneOtherCountries(countryCode: String) {
        runCatching {
            dao.deleteOtherCountries(countryCode)
            dao.deleteOtherCountryStates(countryCode)
        }
    }
}

/** News providers in priority order: the configured API first, then publisher RSS feeds. */
class NewsProviderChain(val providers: List<NewsProvider>)
