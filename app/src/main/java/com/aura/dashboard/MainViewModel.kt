package com.aura.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.dashboard.core.time.AppClock
import com.aura.dashboard.data.preferences.UserPreferencesRepository
import com.aura.dashboard.data.repository.QuickAppRepository
import com.aura.dashboard.data.repository.WeatherRepository
import com.aura.dashboard.domain.model.BackgroundEffect
import com.aura.dashboard.domain.model.UserPreferences
import com.aura.dashboard.ui.background.SceneResolver
import com.aura.dashboard.ui.util.minuteTicker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MainUiState {
    data object Loading : MainUiState
    data class Ready(
        val preferences: UserPreferences,
        /** The animated background to draw, with AUTO resolved from the current weather. */
        val backgroundEffect: BackgroundEffect,
    ) : MainUiState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    preferences: UserPreferencesRepository,
    weather: WeatherRepository,
    quickApps: QuickAppRepository,
    clock: AppClock,
) : ViewModel() {

    val uiState: StateFlow<MainUiState> = combine(
        preferences.preferences,
        weather.state,
        minuteTicker(clock),
    ) { prefs, weatherState, now ->
        MainUiState.Ready(
            preferences = prefs,
            backgroundEffect = SceneResolver.resolve(
                setting = prefs.backgroundEffect,
                weather = weatherState.snapshot?.current,
                hour = now.atZone(clock.zone()).hour,
            ),
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MainUiState.Loading)

    init {
        // One-time additions to Quick Apps for people upgrading from an older version.
        viewModelScope.launch { runCatching { quickApps.applyDefaultUpdates() } }
    }
}
