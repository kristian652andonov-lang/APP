package com.aura.dashboard.ui.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.dashboard.core.time.AppClock
import com.aura.dashboard.data.preferences.UserPreferencesRepository
import com.aura.dashboard.data.repository.WeatherRepository
import com.aura.dashboard.data.repository.WeatherState
import com.aura.dashboard.domain.model.TemperatureUnit
import com.aura.dashboard.domain.model.WeatherError
import com.aura.dashboard.domain.model.WeatherLocation
import com.aura.dashboard.ui.util.minuteTicker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class WeatherUiState(
    val weather: WeatherState = WeatherState(),
    val unit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val isDefaultLocation: Boolean = true,
    val now: Instant = Instant.now(),
)

data class LocationSearchState(
    val query: String = "",
    val results: List<WeatherLocation> = emptyList(),
    val isSearching: Boolean = false,
    val failed: Boolean = false,
    val isLocating: Boolean = false,
)

sealed interface WeatherEvent {
    data class LocationFailed(val error: WeatherError) : WeatherEvent
    data object LocationUpdated : WeatherEvent
}

@OptIn(FlowPreview::class)
@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val weather: WeatherRepository,
    preferences: UserPreferencesRepository,
    clock: AppClock,
) : ViewModel() {

    val uiState: StateFlow<WeatherUiState> = combine(weather.state, preferences.preferences, minuteTicker(clock)) { state, prefs, now ->
        WeatherUiState(weather = state, unit = prefs.temperatureUnit, isDefaultLocation = prefs.weatherLocation == null, now = now)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeatherUiState())

    private val _search = MutableStateFlow(LocationSearchState())
    val search: StateFlow<LocationSearchState> = _search.asStateFlow()

    private val _events = Channel<WeatherEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val queries = MutableStateFlow("")

    init {
        viewModelScope.launch { weather.refresh(force = false) }
        viewModelScope.launch {
            queries.debounce(350).collectLatest { query ->
                if (query.trim().length < 2) {
                    _search.value = _search.value.copy(results = emptyList(), isSearching = false, failed = false)
                    return@collectLatest
                }
                _search.value = _search.value.copy(isSearching = true, failed = false)
                val result = weather.search(query)
                _search.value = _search.value.copy(
                    results = result.getOrDefault(emptyList()),
                    isSearching = false,
                    failed = result.isFailure,
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch { weather.refresh(force = true) }
    }

    fun onQueryChange(query: String) {
        _search.value = _search.value.copy(query = query)
        queries.value = query
    }

    fun selectLocation(location: WeatherLocation?) {
        viewModelScope.launch {
            weather.setLocation(location)
            _search.value = LocationSearchState()
            _events.send(WeatherEvent.LocationUpdated)
        }
    }

    /** Called once the location permission has been granted. */
    fun useDeviceLocation() {
        viewModelScope.launch {
            _search.value = _search.value.copy(isLocating = true)
            val error = weather.useDeviceLocation()
            _search.value = _search.value.copy(isLocating = false)
            _events.send(if (error == null) WeatherEvent.LocationUpdated else WeatherEvent.LocationFailed(error))
        }
    }

    fun onLocationPermissionDenied() {
        viewModelScope.launch { _events.send(WeatherEvent.LocationFailed(WeatherError.PERMISSION_DENIED)) }
    }
}
