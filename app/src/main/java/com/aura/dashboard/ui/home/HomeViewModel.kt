package com.aura.dashboard.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.dashboard.core.network.ConnectivityObserver
import com.aura.dashboard.core.time.AppClock
import com.aura.dashboard.data.apps.AppLauncher
import com.aura.dashboard.data.apps.LaunchResult
import com.aura.dashboard.data.preferences.UserPreferencesRepository
import com.aura.dashboard.data.repository.NewsRepository
import com.aura.dashboard.data.repository.QuickAppRepository
import com.aura.dashboard.data.repository.ReminderRepository
import com.aura.dashboard.data.repository.TaskRepository
import com.aura.dashboard.domain.model.Article
import com.aura.dashboard.domain.model.NewsCategory
import com.aura.dashboard.domain.model.NewsError
import com.aura.dashboard.domain.model.NewsRefreshResult
import com.aura.dashboard.domain.model.QuickApp
import com.aura.dashboard.domain.model.Reminder
import com.aura.dashboard.domain.model.Task
import com.aura.dashboard.domain.model.TaskSort
import com.aura.dashboard.domain.model.UserPreferences
import com.aura.dashboard.domain.schedule.TaskQueries
import com.aura.dashboard.ui.apps.NotInstalledInfo
import com.aura.dashboard.ui.util.minuteTicker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

enum class Greeting { MORNING, AFTERNOON, EVENING }

data class HomeNewsState(
    val articles: List<Article> = emptyList(),
    val isRefreshing: Boolean = false,
    val error: NewsError? = null,
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val greeting: Greeting = Greeting.MORNING,
    val userName: String = "",
    val avatarPath: String? = null,
    val today: LocalDate = LocalDate.now(),
    val now: Instant = Instant.now(),
    val nowTime: LocalTime = LocalTime.now(),
    val todayTasks: List<Task> = emptyList(),
    val completedCount: Int = 0,
    val nextReminder: Reminder? = null,
    val quickApps: List<QuickApp> = emptyList(),
    val news: HomeNewsState = HomeNewsState(),
    val isOnline: Boolean = true,
) {
    val totalCount: Int get() = todayTasks.size
    val progress: Float get() = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount
}

sealed interface HomeEvent {
    data class AppNotInstalled(val info: NotInstalledInfo) : HomeEvent
    data object LaunchFailed : HomeEvent
}

private data class DashboardCore(
    val prefs: UserPreferences,
    val now: Instant,
    val tasks: List<Task>,
    val nextReminder: Reminder?,
    val quickApps: List<QuickApp>,
)

private data class NewsStatus(val isRefreshing: Boolean = false, val error: NewsError? = null)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val tasks: TaskRepository,
    reminders: ReminderRepository,
    quickApps: QuickAppRepository,
    private val news: NewsRepository,
    private val preferences: UserPreferencesRepository,
    private val connectivity: ConnectivityObserver,
    private val clock: AppClock,
    private val launcher: AppLauncher,
) : ViewModel() {

    private val newsStatus = MutableStateFlow(NewsStatus())
    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val core = combine(
        preferences.preferences,
        minuteTicker(clock),
        tasks.observeTasks(),
        reminders.observeNextReminder(),
        quickApps.observeQuickApps(),
    ) { prefs, now, allTasks, next, apps -> DashboardCore(prefs, now, allTasks, next, apps) }

    private val articles = preferences.preferences
        .map { it.countryCode }
        .distinctUntilChanged()
        .flatMapLatest { country -> news.observeArticles(country, NewsCategory.TOP) }

    val uiState: StateFlow<HomeUiState> = combine(core, articles, newsStatus, connectivity.isOnline) { core, articles, status, online ->
        val zone = clock.zone()
        val zoned = core.now.atZone(zone)
        val today = zoned.toLocalDate()
        val todayTasks = TaskQueries.sort(
            core.tasks.filter { TaskQueries.isForToday(it, today, zone) },
            TaskSort.TIME,
        )
        HomeUiState(
            isLoading = false,
            greeting = greetingFor(zoned.hour),
            userName = core.prefs.userName,
            avatarPath = core.prefs.avatarPath,
            today = today,
            now = core.now,
            nowTime = zoned.toLocalTime(),
            todayTasks = todayTasks,
            completedCount = todayTasks.count { it.isCompleted },
            nextReminder = core.nextReminder,
            quickApps = core.quickApps.filter { it.showOnHome }.take(MAX_HOME_APPS),
            news = HomeNewsState(articles.take(MAX_HOME_ARTICLES), status.isRefreshing, status.error),
            isOnline = online,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        viewModelScope.launch {
            preferences.preferences
                .map { it.countryCode to it.newsArticleCount }
                .distinctUntilChanged()
                .collectLatest { (country, count) -> refreshNews(country, count, force = false) }
        }
        viewModelScope.launch {
            // Back online: catch up on headlines if the cache went stale.
            connectivity.isOnline.drop(1).filter { it }.collect {
                val prefs = preferences.current()
                refreshNews(prefs.countryCode, prefs.newsArticleCount, force = false)
            }
        }
    }

    fun refreshNews() {
        viewModelScope.launch {
            val prefs = preferences.current()
            refreshNews(prefs.countryCode, prefs.newsArticleCount, force = true)
        }
    }

    private suspend fun refreshNews(country: String, count: Int, force: Boolean) {
        newsStatus.update { it.copy(isRefreshing = true) }
        val result = if (force) {
            news.refresh(country, NewsCategory.TOP, count)
        } else {
            news.refreshIfStale(country, NewsCategory.TOP, count, STALE_AFTER)
        }
        newsStatus.value = NewsStatus(isRefreshing = false, error = (result as? NewsRefreshResult.Failure)?.error)
    }

    fun toggleTask(task: Task, completed: Boolean) {
        viewModelScope.launch { runCatching { tasks.setCompleted(task.id, completed) } }
    }

    fun launchApp(app: QuickApp) {
        viewModelScope.launch {
            when (val result = launcher.launch(app.packageName)) {
                LaunchResult.Launched -> Unit
                is LaunchResult.NotInstalled -> _events.send(
                    HomeEvent.AppNotInstalled(NotInstalledInfo(app.packageName, app.label, result.webUrl)),
                )
                LaunchResult.Failed -> _events.send(HomeEvent.LaunchFailed)
            }
        }
    }

    fun openStore(packageName: String) = launcher.openStoreListing(packageName)

    fun openWeb(url: String) = launcher.openWeb(url)

    companion object {
        private const val MAX_HOME_APPS = 8
        private const val MAX_HOME_ARTICLES = 6
        val STALE_AFTER: Duration = Duration.ofMinutes(30)

        fun greetingFor(hour: Int): Greeting = when (hour) {
            in 5..11 -> Greeting.MORNING
            in 12..17 -> Greeting.AFTERNOON
            else -> Greeting.EVENING
        }
    }
}
