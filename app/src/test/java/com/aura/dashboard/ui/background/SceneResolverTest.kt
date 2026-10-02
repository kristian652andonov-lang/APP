package com.aura.dashboard.ui.background

import com.aura.dashboard.domain.model.BackgroundEffect
import com.aura.dashboard.domain.model.CurrentWeather
import com.aura.dashboard.domain.model.WeatherCondition
import org.junit.Test
import kotlin.test.assertEquals

class SceneResolverTest {

    private fun weather(condition: WeatherCondition, isDay: Boolean = true) =
        CurrentWeather(temperature = 12.0, feelsLike = 11.0, humidity = 50, windSpeed = 5.0, condition = condition, isDay = isDay)

    @Test
    fun `an explicit choice always wins`() {
        assertEquals(
            BackgroundEffect.PETALS,
            SceneResolver.resolve(BackgroundEffect.PETALS, weather(WeatherCondition.SNOW), hour = 3),
        )
        assertEquals(BackgroundEffect.NONE, SceneResolver.resolve(BackgroundEffect.NONE, null, hour = 12))
    }

    @Test
    fun `auto follows the weather`() {
        fun auto(condition: WeatherCondition, isDay: Boolean = true) =
            SceneResolver.resolve(BackgroundEffect.AUTO, weather(condition, isDay), hour = 12)

        assertEquals(BackgroundEffect.SUNNY, auto(WeatherCondition.CLEAR))
        assertEquals(BackgroundEffect.NIGHT_SKY, auto(WeatherCondition.CLEAR, isDay = false))
        assertEquals(BackgroundEffect.CLOUDS, auto(WeatherCondition.PARTLY_CLOUDY))
        assertEquals(BackgroundEffect.CLOUDS, auto(WeatherCondition.FOG))
        assertEquals(BackgroundEffect.RAIN, auto(WeatherCondition.DRIZZLE))
        assertEquals(BackgroundEffect.SNOW, auto(WeatherCondition.SNOW))
        assertEquals(BackgroundEffect.THUNDER, auto(WeatherCondition.THUNDERSTORM))
    }

    @Test
    fun `auto without weather uses the time of day`() {
        assertEquals(BackgroundEffect.FLOWING_LINES, SceneResolver.resolve(BackgroundEffect.AUTO, null, hour = 10))
        assertEquals(BackgroundEffect.NIGHT_SKY, SceneResolver.resolve(BackgroundEffect.AUTO, null, hour = 23))
    }
}
