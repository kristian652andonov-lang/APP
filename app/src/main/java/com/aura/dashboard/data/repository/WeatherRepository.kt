package com.aura.dashboard.data.repository

import android.content.Context
import com.aura.dashboard.core.di.ApplicationScope
import com.aura.dashboard.core.di.IoDispatcher
import com.aura.dashboard.core.time.AppClock
import com.aura.dashboard.data.preferences.UserPreferencesRepository
import com.aura.dashboard.data.weather.CapitalCities
import com.aura.dashboard.data.weather.DeviceLocationProvider
import com.aura.dashboard.data.weather.ForecastDto
import com.aura.dashboard.data.weather.OpenMeteoService
import com.aura.dashboard.data.weather.WeatherMapping
import com.aura.dashboard.domain.model.TemperatureUnit
import com.aura.dashboard.domain.model.UserPreferences
import com.aura.dashboard.domain.model.WeatherError
import com.aura.dashboard.domain.model.WeatherLocation
import com.aura.dashboard.domain.model.WeatherSnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

data class WeatherState(
    val location: WeatherLocation? = null,
    val snapshot: WeatherSnapshot? = null,
    val isRefreshing: Boolean = false,
    val error: WeatherError? = null,
)

interface WeatherRepository {
    val state: StateFlow<WeatherState>

    /** Fetches a forecast unless the cached one is fresh (or [force] is set). */
    suspend fun refresh(force: Boolean = false)
    suspend fun search(query: String): Result<List<WeatherLocation>>
    suspend fun setLocation(location: WeatherLocation?)

    /** Uses the device's approximate location. The caller must hold the location permission. */
    suspend fun useDeviceLocation(): WeatherError?
}

@Singleton
class OpenMeteoWeatherRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val service: OpenMeteoService,
    private val preferences: UserPreferencesRepository,
    private val locationProvider: DeviceLocationProvider,
    private val json: Json,
    private val clock: AppClock,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope scope: CoroutineScope,
) : WeatherRepository {

    private val _state = MutableStateFlow(WeatherState())
    override val state: StateFlow<WeatherState> = _state.asStateFlow()
    private val refreshLock = Mutex()

    /** Last server-side failure, so automatic refreshes back off instead of retrying every minute. */
    private var lastServerFailure: Pair<Instant, WeatherLocation>? = null
    private val cacheFile: File get() = File(context.filesDir, "weather/forecast.json")

    init {
        scope.launch {
            loadCache()
            // Location, unit or country changes trigger a fetch for the new place.
            preferences.preferences
                .map { effectiveLocation(it) to it.temperatureUnit }
                .distinctUntilChanged()
                .collect { (location, _) ->
                    _state.update { it.copy(location = location) }
                    refresh(force = false)
                }
        }
    }

    override suspend fun refresh(force: Boolean) {
        refreshLock.withLock {
            val prefs = preferences.current()
            val location = effectiveLocation(prefs)
            if (location == null) {
                _state.update { it.copy(location = null, error = WeatherError.LOCATION_UNAVAILABLE) }
                return
            }
            val cached = _state.value.snapshot
            val fresh = cached != null &&
                cached.location.sameAs(location) &&
                cached.unit == prefs.temperatureUnit &&
                Duration.between(cached.fetchedAt, clock.now()) < STALE_AFTER
            if (fresh && !force) return
            val backingOff = lastServerFailure?.let { (at, failedFor) ->
                failedFor.sameAs(location) && Duration.between(at, clock.now()) < RETRY_AFTER
            } == true
            if (backingOff && !force) return

            _state.update { it.copy(location = location, isRefreshing = true, error = null) }
            val error = try {
                val unit = prefs.temperatureUnit
                val dto = service.forecast(
                    latitude = location.latitude.rounded(),
                    longitude = location.longitude.rounded(),
                    temperatureUnit = if (unit == TemperatureUnit.FAHRENHEIT) "fahrenheit" else "celsius",
                    windSpeedUnit = if (unit == TemperatureUnit.FAHRENHEIT) "mph" else "kmh",
                )
                val now = clock.now()
                val snapshot = WeatherMapping.toSnapshot(dto, location, unit, now)
                if (snapshot == null) {
                    WeatherError.INVALID_RESPONSE
                } else {
                    _state.update { it.copy(snapshot = snapshot) }
                    saveCache(CacheEntry(location, unit.name, now.toEpochMilli(), dto))
                    null
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (io: IOException) {
                WeatherError.OFFLINE
            } catch (serialization: SerializationException) {
                WeatherError.INVALID_RESPONSE
            } catch (other: Exception) {
                WeatherError.SERVER
            }
            // Offline failures are cheap and end when connectivity returns, so only server errors back off.
            lastServerFailure = if (error == WeatherError.SERVER || error == WeatherError.INVALID_RESPONSE) clock.now() to location else null
            _state.update { it.copy(isRefreshing = false, error = error) }
        }
    }

    override suspend fun search(query: String): Result<List<WeatherLocation>> {
        val trimmed = query.trim()
        if (trimmed.length < 2) return Result.success(emptyList())
        return try {
            val response = service.searchLocations(trimmed, Locale.getDefault().language.ifEmpty { "en" })
            Result.success(
                response.results.map {
                    WeatherLocation(
                        name = it.name,
                        region = listOfNotNull(it.admin1, it.country).distinct().joinToString(", ").ifEmpty { null },
                        countryCode = it.countryCode,
                        latitude = it.latitude,
                        longitude = it.longitude,
                    )
                },
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    override suspend fun setLocation(location: WeatherLocation?) {
        preferences.setWeatherLocation(location)
    }

    override suspend fun useDeviceLocation(): WeatherError? {
        if (!locationProvider.hasPermission()) return WeatherError.PERMISSION_DENIED
        val fix = locationProvider.currentLocation() ?: return WeatherError.LOCATION_UNAVAILABLE
        // Only ~1 km precision is ever used, stored or sent (to the geocoder and to Open-Meteo).
        val latitude = fix.latitude.rounded()
        val longitude = fix.longitude.rounded()
        val place = locationProvider.placeName(latitude, longitude)
        setLocation(
            WeatherLocation(
                name = place?.first ?: "",
                countryCode = place?.second,
                latitude = latitude,
                longitude = longitude,
                isCurrentLocation = true,
            ),
        )
        return null
    }

    private fun effectiveLocation(prefs: UserPreferences): WeatherLocation? =
        prefs.weatherLocation ?: CapitalCities.forCountry(prefs.countryCode)

    private suspend fun loadCache() {
        val entry = withContext(ioDispatcher) {
            runCatching { json.decodeFromString(CacheEntry.serializer(), cacheFile.readText()) }.getOrNull()
        } ?: return
        val unit = TemperatureUnit.entries.firstOrNull { it.name == entry.unit } ?: TemperatureUnit.CELSIUS
        val snapshot = WeatherMapping.toSnapshot(entry.forecast, entry.location, unit, Instant.ofEpochMilli(entry.fetchedAt))
        if (snapshot != null) _state.update { if (it.snapshot == null) it.copy(snapshot = snapshot) else it }
    }

    private suspend fun saveCache(entry: CacheEntry) = withContext(ioDispatcher) {
        runCatching {
            cacheFile.parentFile?.mkdirs()
            cacheFile.writeText(json.encodeToString(CacheEntry.serializer(), entry))
        }
    }

    @Serializable
    private data class CacheEntry(
        val location: WeatherLocation,
        val unit: String,
        val fetchedAt: Long,
        val forecast: ForecastDto,
    )

    companion object {
        val STALE_AFTER: Duration = Duration.ofMinutes(30)
        val RETRY_AFTER: Duration = Duration.ofMinutes(5)

        /** ~1 km precision is plenty for weather and avoids sending exact coordinates. */
        private fun Double.rounded(): Double = (this * 100).roundToInt() / 100.0

        private fun WeatherLocation.sameAs(other: WeatherLocation): Boolean =
            (latitude.rounded() == other.latitude.rounded()) && (longitude.rounded() == other.longitude.rounded())
    }
}
