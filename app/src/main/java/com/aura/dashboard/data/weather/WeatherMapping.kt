package com.aura.dashboard.data.weather

import com.aura.dashboard.domain.model.CurrentWeather
import com.aura.dashboard.domain.model.DailyForecast
import com.aura.dashboard.domain.model.HourlyForecast
import com.aura.dashboard.domain.model.TemperatureUnit
import com.aura.dashboard.domain.model.WeatherCondition
import com.aura.dashboard.domain.model.WeatherLocation
import com.aura.dashboard.domain.model.WeatherSnapshot
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

object WeatherMapping {

    /** WMO weather interpretation codes, as documented by Open-Meteo. */
    fun condition(code: Int?): WeatherCondition = when (code) {
        0 -> WeatherCondition.CLEAR
        1, 2 -> WeatherCondition.PARTLY_CLOUDY
        3 -> WeatherCondition.CLOUDY
        45, 48 -> WeatherCondition.FOG
        51, 53, 55 -> WeatherCondition.DRIZZLE
        56, 57, 66, 67 -> WeatherCondition.FREEZING_RAIN
        61, 63, 65, 80, 81, 82 -> WeatherCondition.RAIN
        71, 73, 75, 77, 85, 86 -> WeatherCondition.SNOW
        95, 96, 99 -> WeatherCondition.THUNDERSTORM
        else -> WeatherCondition.CLOUDY
    }

    /** Returns null when the response lacks the essentials (current conditions). */
    fun toSnapshot(
        dto: ForecastDto,
        location: WeatherLocation,
        unit: TemperatureUnit,
        fetchedAt: Instant,
    ): WeatherSnapshot? {
        val current = dto.current ?: return null
        val temperature = current.temperature ?: return null
        val zone: ZoneId = dto.timezone?.let { runCatching { ZoneId.of(it) }.getOrNull() }
            ?: ZoneOffset.ofTotalSeconds(dto.utcOffsetSeconds)
        fun instant(local: String?): Instant? =
            local?.let { runCatching { LocalDateTime.parse(it).atZone(zone).toInstant() }.getOrNull() }

        val hourly = dto.hourly?.let { h ->
            h.time.indices.mapNotNull { i ->
                val time = instant(h.time[i]) ?: return@mapNotNull null
                val temp = h.temperature.getOrNull(i) ?: return@mapNotNull null
                HourlyForecast(
                    time = time,
                    temperature = temp,
                    condition = condition(h.weatherCode.getOrNull(i)),
                    precipitationChance = h.precipitationProbability.getOrNull(i) ?: 0,
                    isDay = (h.isDay.getOrNull(i) ?: 1) == 1,
                )
            }
        }.orEmpty()

        val daily = dto.daily?.let { d ->
            d.time.indices.mapNotNull { i ->
                val date = runCatching { LocalDate.parse(d.time[i]) }.getOrNull() ?: return@mapNotNull null
                val high = d.temperatureMax.getOrNull(i) ?: return@mapNotNull null
                val low = d.temperatureMin.getOrNull(i) ?: return@mapNotNull null
                DailyForecast(
                    date = date,
                    condition = condition(d.weatherCode.getOrNull(i)),
                    high = high,
                    low = low,
                    precipitationChance = d.precipitationProbabilityMax.getOrNull(i) ?: 0,
                    sunrise = instant(d.sunrise.getOrNull(i)),
                    sunset = instant(d.sunset.getOrNull(i)),
                    uvIndex = d.uvIndexMax.getOrNull(i),
                )
            }
        }.orEmpty()

        return WeatherSnapshot(
            location = location,
            unit = unit,
            current = CurrentWeather(
                temperature = temperature,
                feelsLike = current.apparentTemperature ?: temperature,
                humidity = current.humidity ?: 0,
                windSpeed = current.windSpeed ?: 0.0,
                condition = condition(current.weatherCode),
                isDay = (current.isDay ?: 1) == 1,
            ),
            hourly = hourly,
            daily = daily,
            fetchedAt = fetchedAt,
            zone = zone,
        )
    }
}
