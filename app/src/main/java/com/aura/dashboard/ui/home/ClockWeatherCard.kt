package com.aura.dashboard.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddLocationAlt
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aura.dashboard.R
import com.aura.dashboard.data.repository.WeatherState
import com.aura.dashboard.domain.model.ClockStyle
import com.aura.dashboard.ui.components.AnalogClock
import com.aura.dashboard.ui.components.DigitalClock
import com.aura.dashboard.ui.components.GlassCard
import com.aura.dashboard.ui.components.SkeletonBlock
import com.aura.dashboard.ui.theme.AuraTheme
import com.aura.dashboard.ui.weather.WeatherFormatting
import com.aura.dashboard.ui.weather.WeatherIcon

/** Live clock next to the current weather; tapping opens the full forecast. */
@Composable
fun ClockWeatherCard(
    weather: WeatherState,
    clockStyle: ClockStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val context = LocalContext.current
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        onClickLabel = stringResource(R.string.home_open_weather),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                if (clockStyle == ClockStyle.ANALOG) AnalogClock(size = 104.dp) else DigitalClock()
            }
            Spacer(Modifier.width(12.dp))
            val snapshot = weather.snapshot
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center) {
                when {
                    snapshot != null -> {
                        val condition = stringResource(WeatherFormatting.conditionRes(snapshot.current.condition, snapshot.current.isDay))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            WeatherIcon(snapshot.current.condition, snapshot.current.isDay, size = 48.dp, contentDescription = condition)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                WeatherFormatting.temperature(snapshot.current.temperature),
                                style = MaterialTheme.typography.headlineLarge,
                                color = colors.textPrimary,
                            )
                        }
                        Text(condition, style = MaterialTheme.typography.labelLarge, color = colors.textPrimary, maxLines = 1)
                        snapshot.daily.firstOrNull()?.let { today ->
                            Text(
                                stringResource(R.string.weather_high_low, WeatherFormatting.temperature(today.high), WeatherFormatting.temperature(today.low)),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Place, contentDescription = null, tint = colors.accent.primary, modifier = Modifier.size(14.dp))
                            Text(
                                (weather.location ?: snapshot.location).let { WeatherFormatting.locationName(context, it) }.substringBefore(","),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    weather.location == null -> {
                        Icon(Icons.Rounded.AddLocationAlt, contentDescription = null, tint = colors.accent.primary, modifier = Modifier.size(32.dp))
                        Text(
                            stringResource(R.string.weather_set_location_short),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textSecondary,
                            textAlign = TextAlign.End,
                        )
                    }
                    else -> {
                        SkeletonBlock(Modifier.size(width = 88.dp, height = 36.dp), RoundedCornerShape(12.dp))
                        Spacer(Modifier.height(8.dp))
                        SkeletonBlock(Modifier.size(width = 64.dp, height = 12.dp))
                    }
                }
            }
        }
    }
}
