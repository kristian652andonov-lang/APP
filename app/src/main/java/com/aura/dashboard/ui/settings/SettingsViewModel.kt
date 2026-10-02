package com.aura.dashboard.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.dashboard.BuildConfig
import com.aura.dashboard.data.preferences.UserPreferencesRepository
import com.aura.dashboard.data.profile.AvatarStore
import com.aura.dashboard.data.repository.NewsRepository
import com.aura.dashboard.core.time.AppClock
import com.aura.dashboard.data.repository.WeatherRepository
import com.aura.dashboard.domain.model.AccentColor
import com.aura.dashboard.domain.model.AppStyle
import com.aura.dashboard.domain.model.BackgroundEffect
import com.aura.dashboard.domain.model.ClockStyle
import com.aura.dashboard.domain.model.IconStyle
import com.aura.dashboard.domain.model.TemperatureUnit
import com.aura.dashboard.domain.model.WeatherLocation
import com.aura.dashboard.ui.background.SceneResolver
import com.aura.dashboard.domain.model.NewsCategory
import com.aura.dashboard.domain.model.NewsRefreshInterval
import com.aura.dashboard.domain.model.NewsSourceInfo
import com.aura.dashboard.domain.model.Priority
import com.aura.dashboard.domain.model.ThemeMode
import com.aura.dashboard.domain.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val newsSource: NewsSourceInfo = NewsSourceInfo("", emptyList(), emptyList(), true),
    val appVersion: String = BuildConfig.VERSION_NAME,
    val weatherLocation: WeatherLocation? = null,
    /** What "Match weather" currently shows, for its preview tile. */
    val autoBackground: BackgroundEffect = BackgroundEffect.FLOWING_LINES,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: UserPreferencesRepository,
    private val news: NewsRepository,
    private val avatarStore: AvatarStore,
    weather: WeatherRepository,
    clock: AppClock,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(preferences.preferences, weather.state) { prefs, weatherState ->
        SettingsUiState(
            preferences = prefs,
            newsSource = news.sourceInfo(prefs.countryCode),
            weatherLocation = weatherState.location,
            autoBackground = SceneResolver.resolve(
                BackgroundEffect.AUTO,
                weatherState.snapshot?.current,
                clock.now().atZone(clock.zone()).hour,
            ),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    private fun update(block: suspend UserPreferencesRepository.() -> Unit) {
        viewModelScope.launch { runCatching { preferences.block() } }
    }

    fun setName(name: String) = update { setUserName(name) }

    fun setAvatar(uri: Uri) {
        viewModelScope.launch {
            val path = avatarStore.save(uri) ?: return@launch
            preferences.setAvatarPath(path)
        }
    }

    fun removeAvatar() {
        viewModelScope.launch {
            preferences.setAvatarPath(null)
            avatarStore.clear()
        }
    }

    fun setCountry(code: String) {
        viewModelScope.launch {
            preferences.setCountry(code)
            news.pruneOtherCountries(code)
        }
    }

    fun setThemeMode(mode: ThemeMode) = update { setThemeMode(mode) }
    fun setAccent(accent: AccentColor) = update { setAccentColor(accent) }
    fun setRemindersEnabled(enabled: Boolean) = update { setRemindersEnabled(enabled) }
    fun setSound(enabled: Boolean) = update { setNotificationSound(enabled) }
    fun setVibration(enabled: Boolean) = update { setNotificationVibration(enabled) }

    fun toggleCategory(category: NewsCategory) {
        val current = uiState.value.preferences.newsCategories
        update { setNewsCategories(if (category in current) current - category else current + category) }
    }

    fun setArticleCount(count: Int) = update { setNewsArticleCount(count) }
    fun setRefreshInterval(interval: NewsRefreshInterval) = update { setNewsRefreshInterval(interval) }
    fun setShowCompleted(show: Boolean) = update { setShowCompletedTasks(show) }
    fun setDefaultPriority(priority: Priority) = update { setDefaultPriority(priority) }
    fun setAppStyle(style: AppStyle) = update { setAppStyle(style) }
    fun setBackground(effect: BackgroundEffect) = update { setBackgroundEffect(effect) }
    fun setClockStyle(style: ClockStyle) = update { setClockStyle(style) }
    fun setIconStyle(style: IconStyle) = update { setIconStyle(style) }
    fun setTemperatureUnit(unit: TemperatureUnit) = update { setTemperatureUnit(unit) }
}
