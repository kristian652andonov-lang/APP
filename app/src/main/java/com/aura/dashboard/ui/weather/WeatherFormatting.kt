package com.aura.dashboard.ui.weather

import android.content.Context
import androidx.annotation.StringRes
import com.aura.dashboard.R
import com.aura.dashboard.domain.model.TemperatureUnit
import com.aura.dashboard.domain.model.WeatherCondition
import com.aura.dashboard.domain.model.WeatherError
import com.aura.dashboard.domain.model.WeatherLocation
import com.aura.dashboard.data.news.Countries
import kotlin.math.roundToInt

object WeatherFormatting {
    fun temperature(value: Double): String = "${value.roundToInt()}°"

    fun wind(context: Context, value: Double, unit: TemperatureUnit): String =
        context.getString(if (unit == TemperatureUnit.FAHRENHEIT) R.string.weather_wind_mph else R.string.weather_wind_kmh, value.roundToInt())

    @StringRes
    fun conditionRes(condition: WeatherCondition, isDay: Boolean): Int = when (condition) {
        WeatherCondition.CLEAR -> if (isDay) R.string.weather_clear_day else R.string.weather_clear_night
        WeatherCondition.PARTLY_CLOUDY -> R.string.weather_partly_cloudy
        WeatherCondition.CLOUDY -> R.string.weather_cloudy
        WeatherCondition.FOG -> R.string.weather_fog
        WeatherCondition.DRIZZLE -> R.string.weather_drizzle
        WeatherCondition.RAIN -> R.string.weather_rain
        WeatherCondition.FREEZING_RAIN -> R.string.weather_freezing_rain
        WeatherCondition.SNOW -> R.string.weather_snow
        WeatherCondition.THUNDERSTORM -> R.string.weather_thunderstorm
    }

    @StringRes
    fun errorRes(error: WeatherError): Int = when (error) {
        WeatherError.OFFLINE -> R.string.weather_error_offline
        WeatherError.LOCATION_UNAVAILABLE -> R.string.weather_error_location
        WeatherError.PERMISSION_DENIED -> R.string.weather_error_permission
        WeatherError.SERVER, WeatherError.INVALID_RESPONSE -> R.string.weather_error_generic
    }

    /** "Plovdiv, Bulgaria" (or "Current location" when the device couldn't name the place). */
    fun locationName(context: Context, location: WeatherLocation): String {
        val name = location.name.ifBlank { context.getString(R.string.weather_current_location) }
        val suffix = location.countryCode?.let { Countries.byCode(it).displayName } ?: location.region
        return if (suffix.isNullOrBlank() || suffix == name) name else "$name, $suffix"
    }
}
