package com.aura.dashboard.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.aura.dashboard.domain.model.AccentColor
import com.aura.dashboard.domain.model.AppStyle
import com.aura.dashboard.domain.model.BackgroundEffect
import com.aura.dashboard.domain.model.ClockStyle
import com.aura.dashboard.domain.model.IconStyle
import com.aura.dashboard.domain.model.TemperatureUnit
import com.aura.dashboard.domain.model.WeatherLocation
import com.aura.dashboard.domain.model.NewsCategory
import com.aura.dashboard.domain.model.NewsRefreshInterval
import com.aura.dashboard.domain.model.Priority
import com.aura.dashboard.domain.model.TaskSort
import com.aura.dashboard.domain.model.ThemeMode
import com.aura.dashboard.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

interface UserPreferencesRepository {
    val preferences: Flow<UserPreferences>
    suspend fun current(): UserPreferences = preferences.first()

    suspend fun completeOnboarding(name: String, countryCode: String)
    suspend fun setUserName(name: String)
    suspend fun setAvatarPath(path: String?)
    suspend fun setCountry(countryCode: String)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setAccentColor(accent: AccentColor)
    suspend fun setRemindersEnabled(enabled: Boolean)
    suspend fun setNotificationSound(enabled: Boolean)
    suspend fun setNotificationVibration(enabled: Boolean)
    suspend fun setNewsCategories(categories: Set<NewsCategory>)
    suspend fun setNewsArticleCount(count: Int)
    suspend fun setNewsRefreshInterval(interval: NewsRefreshInterval)
    suspend fun setShowCompletedTasks(show: Boolean)
    suspend fun setDefaultPriority(priority: Priority)
    suspend fun setTaskSort(sort: TaskSort)
    suspend fun setQuickAppsSeeded()
    suspend fun setQuickAppsSeedVersion(version: Int)
    suspend fun setAppStyle(style: AppStyle)
    suspend fun setBackgroundEffect(effect: BackgroundEffect)
    suspend fun setClockStyle(style: ClockStyle)
    suspend fun setIconStyle(style: IconStyle)
    suspend fun setTemperatureUnit(unit: TemperatureUnit)
    suspend fun setWeatherLocation(location: WeatherLocation?)
}

@Singleton
class DataStoreUserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : UserPreferencesRepository {

    private object Keys {
        val onboardingCompleted = booleanPreferencesKey("onboarding_completed")
        val userName = stringPreferencesKey("user_name")
        val avatarPath = stringPreferencesKey("avatar_path")
        val country = stringPreferencesKey("country")
        val themeMode = stringPreferencesKey("theme_mode")
        val accent = stringPreferencesKey("accent")
        val remindersEnabled = booleanPreferencesKey("reminders_enabled")
        val notificationSound = booleanPreferencesKey("notification_sound")
        val notificationVibration = booleanPreferencesKey("notification_vibration")
        val newsCategories = stringSetPreferencesKey("news_categories")
        val newsArticleCount = intPreferencesKey("news_article_count")
        val newsRefreshInterval = stringPreferencesKey("news_refresh_interval")
        val showCompletedTasks = booleanPreferencesKey("show_completed_tasks")
        val defaultPriority = stringPreferencesKey("default_priority")
        val taskSort = stringPreferencesKey("task_sort")
        val quickAppsSeeded = booleanPreferencesKey("quick_apps_seeded")
        val quickAppsSeedVersion = intPreferencesKey("quick_apps_seed_version")
        val appStyle = stringPreferencesKey("app_style")
        val backgroundEffect = stringPreferencesKey("background_effect")
        val clockStyle = stringPreferencesKey("clock_style")
        val iconStyle = stringPreferencesKey("icon_style")
        val temperatureUnit = stringPreferencesKey("temperature_unit")
        val weatherLocation = stringPreferencesKey("weather_location")
    }

    override val preferences: Flow<UserPreferences> = dataStore.data
        .catch { error ->
            // A corrupted/unreadable file must not crash the app; fall back to defaults.
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map(::toUserPreferences)
        .distinctUntilChanged()

    private fun toUserPreferences(p: Preferences): UserPreferences {
        val defaults = UserPreferences()
        return UserPreferences(
            onboardingCompleted = p[Keys.onboardingCompleted] ?: defaults.onboardingCompleted,
            userName = p[Keys.userName] ?: defaults.userName,
            avatarPath = p[Keys.avatarPath],
            countryCode = p[Keys.country] ?: defaults.countryCode,
            themeMode = p[Keys.themeMode].toEnum(defaults.themeMode),
            accentColor = p[Keys.accent].toEnum(defaults.accentColor),
            remindersEnabled = p[Keys.remindersEnabled] ?: defaults.remindersEnabled,
            notificationSound = p[Keys.notificationSound] ?: defaults.notificationSound,
            notificationVibration = p[Keys.notificationVibration] ?: defaults.notificationVibration,
            newsCategories = p[Keys.newsCategories]
                ?.mapNotNull { name -> NewsCategory.entries.firstOrNull { it.name == name } }
                ?.toSet()
                ?.takeIf { it.isNotEmpty() }
                ?: defaults.newsCategories,
            newsArticleCount = p[Keys.newsArticleCount] ?: defaults.newsArticleCount,
            newsRefreshInterval = p[Keys.newsRefreshInterval].toEnum(defaults.newsRefreshInterval),
            showCompletedTasks = p[Keys.showCompletedTasks] ?: defaults.showCompletedTasks,
            defaultPriority = p[Keys.defaultPriority].toEnum(defaults.defaultPriority),
            taskSort = p[Keys.taskSort].toEnum(defaults.taskSort),
            quickAppsSeeded = p[Keys.quickAppsSeeded] ?: defaults.quickAppsSeeded,
            quickAppsSeedVersion = p[Keys.quickAppsSeedVersion] ?: defaults.quickAppsSeedVersion,
            appStyle = p[Keys.appStyle].toEnum(defaults.appStyle),
            backgroundEffect = p[Keys.backgroundEffect].toEnum(defaults.backgroundEffect),
            clockStyle = p[Keys.clockStyle].toEnum(defaults.clockStyle),
            iconStyle = p[Keys.iconStyle].toEnum(defaults.iconStyle),
            temperatureUnit = p[Keys.temperatureUnit].toEnum(defaults.temperatureUnit),
            weatherLocation = p[Keys.weatherLocation]?.let { raw ->
                runCatching { json.decodeFromString(WeatherLocation.serializer(), raw) }.getOrNull()
            },
        )
    }

    override suspend fun completeOnboarding(name: String, countryCode: String) {
        dataStore.edit {
            it[Keys.userName] = name.trim()
            it[Keys.country] = countryCode
            it[Keys.onboardingCompleted] = true
        }
    }

    override suspend fun setUserName(name: String) = set(Keys.userName, name.trim())

    override suspend fun setAvatarPath(path: String?) {
        dataStore.edit { if (path == null) it.remove(Keys.avatarPath) else it[Keys.avatarPath] = path }
    }

    override suspend fun setCountry(countryCode: String) = set(Keys.country, countryCode)
    override suspend fun setThemeMode(mode: ThemeMode) = set(Keys.themeMode, mode.name)
    override suspend fun setAccentColor(accent: AccentColor) = set(Keys.accent, accent.name)
    override suspend fun setRemindersEnabled(enabled: Boolean) = set(Keys.remindersEnabled, enabled)
    override suspend fun setNotificationSound(enabled: Boolean) = set(Keys.notificationSound, enabled)
    override suspend fun setNotificationVibration(enabled: Boolean) = set(Keys.notificationVibration, enabled)

    override suspend fun setNewsCategories(categories: Set<NewsCategory>) {
        // TOP is always available so the News screen is never empty of filters.
        set(Keys.newsCategories, (categories + NewsCategory.TOP).map { it.name }.toSet())
    }

    override suspend fun setNewsArticleCount(count: Int) = set(Keys.newsArticleCount, count.coerceIn(5, 50))
    override suspend fun setNewsRefreshInterval(interval: NewsRefreshInterval) = set(Keys.newsRefreshInterval, interval.name)
    override suspend fun setShowCompletedTasks(show: Boolean) = set(Keys.showCompletedTasks, show)
    override suspend fun setDefaultPriority(priority: Priority) = set(Keys.defaultPriority, priority.name)
    override suspend fun setTaskSort(sort: TaskSort) = set(Keys.taskSort, sort.name)
    override suspend fun setQuickAppsSeeded() = set(Keys.quickAppsSeeded, true)
    override suspend fun setQuickAppsSeedVersion(version: Int) = set(Keys.quickAppsSeedVersion, version)
    override suspend fun setAppStyle(style: AppStyle) = set(Keys.appStyle, style.name)
    override suspend fun setBackgroundEffect(effect: BackgroundEffect) = set(Keys.backgroundEffect, effect.name)
    override suspend fun setClockStyle(style: ClockStyle) = set(Keys.clockStyle, style.name)
    override suspend fun setIconStyle(style: IconStyle) = set(Keys.iconStyle, style.name)
    override suspend fun setTemperatureUnit(unit: TemperatureUnit) = set(Keys.temperatureUnit, unit.name)

    override suspend fun setWeatherLocation(location: WeatherLocation?) {
        dataStore.edit {
            if (location == null) {
                it.remove(Keys.weatherLocation)
            } else {
                it[Keys.weatherLocation] = json.encodeToString(WeatherLocation.serializer(), location)
            }
        }
    }

    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        dataStore.edit { it[key] = value }
    }

    private inline fun <reified E : Enum<E>> String?.toEnum(default: E): E =
        enumValues<E>().firstOrNull { it.name == this } ?: default
}
