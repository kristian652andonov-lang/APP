package com.aura.dashboard.ui.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.dashboard.core.network.ConnectivityObserver
import com.aura.dashboard.core.time.AppClock
import com.aura.dashboard.data.news.Countries
import com.aura.dashboard.data.news.Country
import com.aura.dashboard.data.preferences.UserPreferencesRepository
import com.aura.dashboard.data.repository.NewsRepository
import com.aura.dashboard.domain.model.Article
import com.aura.dashboard.domain.model.NewsCategory
import com.aura.dashboard.domain.model.NewsError
import com.aura.dashboard.domain.model.NewsRefreshResult
import com.aura.dashboard.domain.model.NewsSourceInfo
import com.aura.dashboard.ui.util.minuteTicker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

data class NewsUiState(
    val country: Country = Countries.byCode("BG"),
    val categories: List<NewsCategory> = listOf(NewsCategory.TOP),
    val selected: NewsCategory = NewsCategory.TOP,
    val articles: List<Article> = emptyList(),
    val lastUpdated: Instant? = null,
    val isRefreshing: Boolean = false,
    val error: NewsError? = null,
    val isOnline: Boolean = true,
    val sourceInfo: NewsSourceInfo = NewsSourceInfo("", emptyList(), emptyList(), isAvailable = true),
    val now: Instant = Instant.now(),
    val isLoaded: Boolean = false,
)

private data class Selection(val country: String, val category: NewsCategory, val count: Int)

private data class FeedStatus(val isRefreshing: Boolean = false, val error: NewsError? = null)

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val news: NewsRepository,
    private val preferences: UserPreferencesRepository,
    private val connectivity: ConnectivityObserver,
    clock: AppClock,
) : ViewModel() {

    private val requestedCategory = MutableStateFlow(NewsCategory.TOP)
    private val status = MutableStateFlow(FeedStatus())

    private val selection = combine(preferences.preferences, requestedCategory) { prefs, requested ->
        val available = availableCategories(prefs.countryCode, prefs.newsCategories)
        Selection(prefs.countryCode, requested.takeIf { it in available } ?: available.first(), prefs.newsArticleCount)
    }.distinctUntilChanged()

    private val feed = selection.flatMapLatest { sel ->
        combine(
            news.observeArticles(sel.country, sel.category),
            news.observeLastUpdated(sel.country, sel.category),
        ) { articles, updated -> Triple(sel, articles, updated) }
    }

    val uiState: StateFlow<NewsUiState> = combine(
        feed,
        preferences.preferences,
        status,
        connectivity.isOnline,
        minuteTicker(clock),
    ) { (sel, articles, updated), prefs, status, online, now ->
        NewsUiState(
            country = Countries.byCode(sel.country),
            categories = availableCategories(sel.country, prefs.newsCategories),
            selected = sel.category,
            articles = articles,
            lastUpdated = updated,
            isRefreshing = status.isRefreshing,
            error = status.error,
            isOnline = online,
            sourceInfo = news.sourceInfo(sel.country),
            now = now,
            isLoaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NewsUiState())

    init {
        viewModelScope.launch {
            selection.collectLatest { sel ->
                status.value = FeedStatus()
                refresh(sel, force = false)
            }
        }
        viewModelScope.launch {
            connectivity.isOnline.drop(1).filter { it }.collect { refresh(selection.first(), force = false) }
        }
    }

    fun selectCategory(category: NewsCategory) {
        requestedCategory.value = category
    }

    fun refresh() {
        viewModelScope.launch { refresh(selection.first(), force = true) }
    }

    private suspend fun refresh(sel: Selection, force: Boolean) {
        status.value = status.value.copy(isRefreshing = true)
        val result = if (force) {
            news.refresh(sel.country, sel.category, sel.count)
        } else {
            news.refreshIfStale(sel.country, sel.category, sel.count, STALE_AFTER)
        }
        status.value = FeedStatus(isRefreshing = false, error = (result as? NewsRefreshResult.Failure)?.error)
    }

    private fun availableCategories(country: String, enabled: Set<NewsCategory>): List<NewsCategory> {
        val supported = news.sourceInfo(country).categories
        val list = supported.filter { it in enabled || it == NewsCategory.TOP }
        return list.ifEmpty { listOf(NewsCategory.TOP) }
    }

    private companion object {
        val STALE_AFTER: Duration = Duration.ofMinutes(15)
    }
}
