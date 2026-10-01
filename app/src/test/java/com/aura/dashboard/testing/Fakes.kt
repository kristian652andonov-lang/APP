package com.aura.dashboard.testing

import com.aura.dashboard.core.time.AppClock
import com.aura.dashboard.data.preferences.UserPreferencesRepository
import com.aura.dashboard.domain.model.AccentColor
import com.aura.dashboard.domain.model.NewsCategory
import com.aura.dashboard.domain.model.NewsRefreshInterval
import com.aura.dashboard.domain.model.Priority
import com.aura.dashboard.domain.model.TaskSort
import com.aura.dashboard.domain.model.ThemeMode
import com.aura.dashboard.domain.model.UserPreferences
import com.aura.dashboard.notifications.AlarmTarget
import com.aura.dashboard.notifications.NotificationDismisser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.ZoneId

class FakeClock(var instant: Instant, private val zoneId: ZoneId = ZoneId.of("Europe/Sofia")) : AppClock {
    override fun now(): Instant = instant
    override fun zone(): ZoneId = zoneId
}

class FakeDismisser : NotificationDismisser {
    val dismissed = mutableListOf<AlarmTarget>()
    override fun dismiss(target: AlarmTarget) {
        dismissed += target
    }
}

class FakePreferences(initial: UserPreferences = UserPreferences()) : UserPreferencesRepository {
    override val preferences = MutableStateFlow(initial)
    private fun edit(block: (UserPreferences) -> UserPreferences) = preferences.update(block)

    override suspend fun completeOnboarding(name: String, countryCode: String) = edit { it.copy(userName = name, countryCode = countryCode, onboardingCompleted = true) }
    override suspend fun setUserName(name: String) = edit { it.copy(userName = name) }
    override suspend fun setAvatarPath(path: String?) = edit { it.copy(avatarPath = path) }
    override suspend fun setCountry(countryCode: String) = edit { it.copy(countryCode = countryCode) }
    override suspend fun setThemeMode(mode: ThemeMode) = edit { it.copy(themeMode = mode) }
    override suspend fun setAccentColor(accent: AccentColor) = edit { it.copy(accentColor = accent) }
    override suspend fun setRemindersEnabled(enabled: Boolean) = edit { it.copy(remindersEnabled = enabled) }
    override suspend fun setNotificationSound(enabled: Boolean) = edit { it.copy(notificationSound = enabled) }
    override suspend fun setNotificationVibration(enabled: Boolean) = edit { it.copy(notificationVibration = enabled) }
    override suspend fun setNewsCategories(categories: Set<NewsCategory>) = edit { it.copy(newsCategories = categories) }
    override suspend fun setNewsArticleCount(count: Int) = edit { it.copy(newsArticleCount = count) }
    override suspend fun setNewsRefreshInterval(interval: NewsRefreshInterval) = edit { it.copy(newsRefreshInterval = interval) }
    override suspend fun setShowCompletedTasks(show: Boolean) = edit { it.copy(showCompletedTasks = show) }
    override suspend fun setDefaultPriority(priority: Priority) = edit { it.copy(defaultPriority = priority) }
    override suspend fun setTaskSort(sort: TaskSort) = edit { it.copy(taskSort = sort) }
    override suspend fun setQuickAppsSeeded() = edit { it.copy(quickAppsSeeded = true) }
}
