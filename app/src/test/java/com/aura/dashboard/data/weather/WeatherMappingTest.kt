package com.aura.dashboard.data.weather

import com.aura.dashboard.data.news.Countries
import com.aura.dashboard.domain.model.TemperatureUnit
import com.aura.dashboard.domain.model.WeatherCondition
import com.aura.dashboard.domain.model.WeatherLocation
import kotlinx.serialization.json.Json
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class WeatherMappingTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val sofia = WeatherLocation("Sofia", null, "BG", 42.70, 23.32)
    private val fetchedAt = Instant.parse("2026-10-02T08:00:00Z")

    // Trimmed response captured from https://api.open-meteo.com/v1/forecast for Sofia.
    private val response = """
        {
          "latitude": 42.7, "longitude": 23.32, "generationtime_ms": 0.2,
          "utc_offset_seconds": 10800, "timezone": "Europe/Sofia", "timezone_abbreviation": "GMT+3",
          "current_units": { "temperature_2m": "°C" },
          "current": {
            "time": "2026-10-02T11:00", "interval": 900, "temperature_2m": 17.4,
            "apparent_temperature": 15.9, "relative_humidity_2m": 61, "is_day": 1,
            "weather_code": 2, "wind_speed_10m": 9.7
          },
          "hourly": {
            "time": ["2026-10-02T11:00", "2026-10-02T12:00", "2026-10-02T21:00"],
            "temperature_2m": [17.4, 18.9, null],
            "weather_code": [2, 61, 0],
            "precipitation_probability": [5, 70, 0],
            "is_day": [1, 1, 0]
          },
          "daily": {
            "time": ["2026-10-02", "2026-10-03"],
            "weather_code": [61, 95],
            "temperature_2m_max": [19.8, 16.2],
            "temperature_2m_min": [8.1, 7.4],
            "sunrise": ["2026-10-02T07:15", "2026-10-02T07:16"],
            "sunset": ["2026-10-02T18:57", "2026-10-03T18:55"],
            "precipitation_probability_max": [70, 90],
            "uv_index_max": [4.1, 2.3]
          }
        }
    """.trimIndent()

    @Test
    fun `wmo codes map to conditions`() {
        assertEquals(WeatherCondition.CLEAR, WeatherMapping.condition(0))
        assertEquals(WeatherCondition.PARTLY_CLOUDY, WeatherMapping.condition(2))
        assertEquals(WeatherCondition.CLOUDY, WeatherMapping.condition(3))
        assertEquals(WeatherCondition.FOG, WeatherMapping.condition(48))
        assertEquals(WeatherCondition.DRIZZLE, WeatherMapping.condition(53))
        assertEquals(WeatherCondition.FREEZING_RAIN, WeatherMapping.condition(67))
        assertEquals(WeatherCondition.RAIN, WeatherMapping.condition(81))
        assertEquals(WeatherCondition.SNOW, WeatherMapping.condition(86))
        assertEquals(WeatherCondition.THUNDERSTORM, WeatherMapping.condition(99))
        assertEquals(WeatherCondition.CLOUDY, WeatherMapping.condition(null))
    }

    @Test
    fun `forecast response parses into a snapshot in the location's time zone`() {
        val dto = json.decodeFromString<ForecastDto>(response)
        val snapshot = assertNotNull(WeatherMapping.toSnapshot(dto, sofia, TemperatureUnit.CELSIUS, fetchedAt))
        val zone = ZoneId.of("Europe/Sofia")

        assertEquals(17.4, snapshot.current.temperature)
        assertEquals(15.9, snapshot.current.feelsLike)
        assertEquals(61, snapshot.current.humidity)
        assertEquals(WeatherCondition.PARTLY_CLOUDY, snapshot.current.condition)

        // The hour with a missing temperature is dropped instead of shown as 0°.
        assertEquals(2, snapshot.hourly.size)
        assertEquals(LocalDateTime.parse("2026-10-02T12:00").atZone(zone).toInstant(), snapshot.hourly[1].time)
        assertEquals(WeatherCondition.RAIN, snapshot.hourly[1].condition)
        assertEquals(70, snapshot.hourly[1].precipitationChance)

        assertEquals(2, snapshot.daily.size)
        val tomorrow = snapshot.daily[1]
        assertEquals(LocalDate.parse("2026-10-03"), tomorrow.date)
        assertEquals(WeatherCondition.THUNDERSTORM, tomorrow.condition)
        assertEquals(16.2, tomorrow.high)
        assertEquals(7.4, tomorrow.low)
        assertEquals(LocalDateTime.parse("2026-10-03T18:55").atZone(zone).toInstant(), tomorrow.sunset)
        assertEquals(fetchedAt, snapshot.fetchedAt)
        assertEquals(zone, snapshot.zone)
    }

    @Test
    fun `night time is reported`() {
        val dto = json.decodeFromString<ForecastDto>(response.replace("\"is_day\": 1,", "\"is_day\": 0,"))
        val snapshot = assertNotNull(WeatherMapping.toSnapshot(dto, sofia, TemperatureUnit.CELSIUS, fetchedAt))
        assertFalse(snapshot.current.isDay)
    }

    @Test
    fun `responses without current conditions are rejected`() {
        val dto = json.decodeFromString<ForecastDto>("""{ "timezone": "Europe/Sofia", "daily": { "time": [] } }""")
        assertNull(WeatherMapping.toSnapshot(dto, sofia, TemperatureUnit.CELSIUS, fetchedAt))
    }

    @Test
    fun `every supported country has a capital fallback`() {
        Countries.all.forEach { country ->
            assertNotNull(CapitalCities.forCountry(country.code), country.code)
        }
        assertEquals("Sofia", CapitalCities.forCountry("bg")?.name)
    }
}
