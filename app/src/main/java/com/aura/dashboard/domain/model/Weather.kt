package com.aura.dashboard.domain.model

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Serializable
data class WeatherLocation(
    val name: String,
    val region: String? = null,
    val countryCode: String? = null,
    val latitude: Double,
    val longitude: Double,
    val isCurrentLocation: Boolean = false,
)

/** Simplified WMO weather codes. */
enum class WeatherCondition {
    CLEAR, PARTLY_CLOUDY, CLOUDY, FOG, DRIZZLE, RAIN, FREEZING_RAIN, SNOW, THUNDERSTORM;

    val isPrecipitation: Boolean get() = this in setOf(DRIZZLE, RAIN, FREEZING_RAIN, SNOW, THUNDERSTORM)
}

data class CurrentWeather(
    val temperature: Double,
    val feelsLike: Double,
    val humidity: Int,
    val windSpeed: Double,
    val condition: WeatherCondition,
    val isDay: Boolean,
)

data class HourlyForecast(
    val time: Instant,
    val temperature: Double,
    val condition: WeatherCondition,
    val precipitationChance: Int,
    val isDay: Boolean,
)

data class DailyForecast(
    val date: LocalDate,
    val condition: WeatherCondition,
    val high: Double,
    val low: Double,
    val precipitationChance: Int,
    val sunrise: Instant?,
    val sunset: Instant?,
    val uvIndex: Double?,
)

data class WeatherSnapshot(
    val location: WeatherLocation,
    val unit: TemperatureUnit,
    val current: CurrentWeather,
    val hourly: List<HourlyForecast>,
    val daily: List<DailyForecast>,
    val fetchedAt: Instant,
    /** The forecast location's time zone, so a city abroad shows its own local times. */
    val zone: ZoneId = ZoneId.systemDefault(),
)

enum class WeatherError { OFFLINE, LOCATION_UNAVAILABLE, PERMISSION_DENIED, SERVER, INVALID_RESPONSE }
