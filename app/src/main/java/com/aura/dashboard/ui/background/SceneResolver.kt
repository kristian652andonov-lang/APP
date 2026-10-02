package com.aura.dashboard.ui.background

import com.aura.dashboard.domain.model.BackgroundEffect
import com.aura.dashboard.domain.model.CurrentWeather
import com.aura.dashboard.domain.model.WeatherCondition

object SceneResolver {
    /** Picks the scene for AUTO: the current weather, or a neutral scene before weather loads. */
    fun resolve(setting: BackgroundEffect, weather: CurrentWeather?, hour: Int): BackgroundEffect {
        if (setting != BackgroundEffect.AUTO) return setting
        if (weather == null) return if (hour in 7..19) BackgroundEffect.FLOWING_LINES else BackgroundEffect.NIGHT_SKY
        return when (weather.condition) {
            WeatherCondition.CLEAR -> if (weather.isDay) BackgroundEffect.SUNNY else BackgroundEffect.NIGHT_SKY
            WeatherCondition.PARTLY_CLOUDY -> if (weather.isDay) BackgroundEffect.CLOUDS else BackgroundEffect.NIGHT_SKY
            WeatherCondition.CLOUDY, WeatherCondition.FOG -> BackgroundEffect.CLOUDS
            WeatherCondition.DRIZZLE, WeatherCondition.RAIN, WeatherCondition.FREEZING_RAIN -> BackgroundEffect.RAIN
            WeatherCondition.SNOW -> BackgroundEffect.SNOW
            WeatherCondition.THUNDERSTORM -> BackgroundEffect.THUNDER
        }
    }
}
