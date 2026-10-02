package com.aura.dashboard.ui.weather

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.EditLocationAlt
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.dashboard.R
import com.aura.dashboard.domain.model.DailyForecast
import com.aura.dashboard.domain.model.WeatherError
import com.aura.dashboard.domain.model.WeatherSnapshot
import com.aura.dashboard.ui.components.EmptyState
import com.aura.dashboard.ui.components.GlassCard
import com.aura.dashboard.ui.components.GlassIconButton
import com.aura.dashboard.ui.components.OfflinePill
import com.aura.dashboard.ui.components.ScreenHeader
import com.aura.dashboard.ui.components.SectionHeader
import com.aura.dashboard.ui.components.SkeletonBlock
import com.aura.dashboard.ui.components.SoftButton
import com.aura.dashboard.ui.navigation.LocalSnackbarHostState
import com.aura.dashboard.ui.theme.AuraTheme
import com.aura.dashboard.ui.util.Formatting
import java.time.format.TextStyle

@Composable
fun WeatherScreen(onBack: () -> Unit, viewModel: WeatherViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val snackbar = LocalSnackbarHostState.current
    val colors = AuraTheme.colors
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    var pickingLocation by rememberSaveable { mutableStateOf(false) }
    val weather = state.weather
    val snapshot = weather.snapshot

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is WeatherEvent.LocationFailed -> {
                    // Here "unavailable" means the phone couldn't get a fix, not that no place is set.
                    val message = if (event.error == WeatherError.LOCATION_UNAVAILABLE) {
                        R.string.weather_error_no_fix
                    } else {
                        WeatherFormatting.errorRes(event.error)
                    }
                    snackbar.showSnackbar(resources.getString(message))
                }
                WeatherEvent.LocationUpdated -> pickingLocation = false
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = topPadding + 12.dp, bottom = bottomPadding + 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "header") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back), onBack)
                Spacer(Modifier.width(12.dp))
                ScreenHeader(
                    title = stringResource(R.string.weather_title),
                    subtitle = snapshot?.let {
                        stringResource(R.string.weather_updated, Formatting.relativeTime(it.fetchedAt, state.now))
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    GlassIconButton(Icons.Rounded.Refresh, stringResource(R.string.weather_refresh), viewModel::refresh)
                }
            }
        }
        item(key = "location") {
            val location = weather.location ?: snapshot?.location
            Row(verticalAlignment = Alignment.CenterVertically) {
                SoftButton(
                    text = location?.let { WeatherFormatting.locationName(context, it) } ?: stringResource(R.string.weather_choose_location),
                    icon = Icons.Rounded.EditLocationAlt,
                    onClick = { pickingLocation = true },
                )
                if (weather.error == WeatherError.OFFLINE) {
                    Spacer(Modifier.width(8.dp))
                    OfflinePill()
                }
            }
        }

        when {
            snapshot != null -> {
                item(key = "current") { CurrentCard(snapshot) }
                item(key = "hourly") { HourlyRow(snapshot) }
                item(key = "daily") { DailyCard(snapshot) }
                item(key = "attribution") {
                    Text(
                        text = stringResource(R.string.weather_attribution),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(role = Role.Button) {
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, "https://open-meteo.com/".toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                    )
                                }
                            }
                            .padding(12.dp),
                    )
                }
            }
            weather.isRefreshing || (weather.error == null && weather.location != null) -> item(key = "loading") {
                GlassCard(Modifier.fillMaxWidth()) {
                    SkeletonBlock(Modifier.size(120.dp), RoundedCornerShape(60.dp))
                    Spacer(Modifier.height(16.dp))
                    SkeletonBlock(Modifier.fillMaxWidth(0.5f).height(28.dp))
                }
            }
            else -> item(key = "error") {
                GlassCard(Modifier.fillMaxWidth()) {
                    EmptyState(
                        icon = Icons.Rounded.WbSunny,
                        title = stringResource(R.string.weather_unavailable_title),
                        message = stringResource(WeatherFormatting.errorRes(weather.error ?: WeatherError.LOCATION_UNAVAILABLE)),
                        actionLabel = stringResource(if (weather.location == null) R.string.weather_choose_location else R.string.action_retry),
                        onAction = { if (weather.location == null) pickingLocation = true else viewModel.refresh() },
                    )
                }
            }
        }
    }

    if (pickingLocation) {
        LocationPickerSheet(viewModel = viewModel, onDismiss = { pickingLocation = false })
    }
}

@Composable
private fun CurrentCard(snapshot: WeatherSnapshot) {
    val colors = AuraTheme.colors
    val context = LocalContext.current
    val current = snapshot.current
    val today = snapshot.daily.firstOrNull()
    val condition = stringResource(WeatherFormatting.conditionRes(current.condition, current.isDay))
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            WeatherIcon(current.condition, current.isDay, size = 112.dp, contentDescription = condition)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = WeatherFormatting.temperature(current.temperature),
                    style = MaterialTheme.typography.displayLarge,
                    color = colors.textPrimary,
                )
                Text(condition, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Text(
                    stringResource(R.string.weather_feels_like, WeatherFormatting.temperature(current.feelsLike)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
                if (today != null) {
                    Text(
                        stringResource(R.string.weather_high_low, WeatherFormatting.temperature(today.high), WeatherFormatting.temperature(today.low)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        ) {
            val statModifier = Modifier.weight(1f).fillMaxHeight()
            Stat(Icons.Rounded.WaterDrop, stringResource(R.string.weather_humidity), "${current.humidity}%", statModifier)
            Stat(Icons.Rounded.Air, stringResource(R.string.weather_wind), WeatherFormatting.wind(context, current.windSpeed, snapshot.unit), statModifier)
            val rise = today?.sunrise?.atZone(snapshot.zone)?.toLocalTime()?.let { Formatting.time(context, it) }
            val set = today?.sunset?.atZone(snapshot.zone)?.toLocalTime()?.let { Formatting.time(context, it) }
            if (rise != null && set != null) {
                Stat(
                    icon = Icons.Rounded.WbTwilight,
                    lines = listOf(stringResource(R.string.weather_sunrise) to rise, stringResource(R.string.weather_sunset) to set),
                    modifier = statModifier,
                )
            }
        }
    }
}

@Composable
private fun Stat(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) =
    Stat(icon, listOf(label to value), modifier)

/** A small tile with an icon and one or more label/value pairs. */
@Composable
private fun Stat(icon: ImageVector, lines: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    val colors = AuraTheme.colors
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(colors.accent.primary.copy(alpha = if (colors.isDark) 0.12f else 0.07f))
            .padding(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = colors.accent.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(6.dp))
        lines.forEachIndexed { index, (label, value) ->
            if (index > 0) Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
            Text(value, style = MaterialTheme.typography.labelLarge, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun HourlyRow(snapshot: WeatherSnapshot) {
    val colors = AuraTheme.colors
    val context = LocalContext.current
    Column {
        SectionHeader(title = stringResource(R.string.weather_next_24h))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 6.dp)) {
            items(snapshot.hourly.take(24), key = { it.time.toEpochMilli() }) { forecast ->
                val local = forecast.time.atZone(snapshot.zone).toLocalTime()
                val isNow = forecast == snapshot.hourly.first()
                // "Now" shows the live reading rather than the value forecast for the top of the hour.
                val hour = if (isNow) {
                    forecast.copy(temperature = snapshot.current.temperature, condition = snapshot.current.condition, isDay = snapshot.current.isDay)
                } else {
                    forecast
                }
                GlassCard(
                    shape = RoundedCornerShape(22.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    elevation = 6.dp,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isNow) stringResource(R.string.weather_now) else Formatting.time(context, local.withMinute(0)),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isNow) colors.accent.primary else colors.textSecondary,
                        )
                        Spacer(Modifier.height(6.dp))
                        WeatherIcon(hour.condition, hour.isDay, size = 34.dp, animate = false)
                        Spacer(Modifier.height(6.dp))
                        Text(WeatherFormatting.temperature(hour.temperature), style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                        Text(
                            text = if (hour.precipitationChance > 0) "${hour.precipitationChance}%" else " ",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.accent.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyCard(snapshot: WeatherSnapshot) {
    val colors = AuraTheme.colors
    val weekLow = snapshot.daily.minOfOrNull { it.low } ?: 0.0
    val weekHigh = snapshot.daily.maxOfOrNull { it.high } ?: 1.0
    Column {
        SectionHeader(title = stringResource(R.string.weather_7_days))
        GlassCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
            snapshot.daily.forEachIndexed { index, day ->
                DayRow(day, isToday = index == 0, weekLow = weekLow, weekHigh = weekHigh)
                if (index < snapshot.daily.lastIndex) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.divider.copy(alpha = 0.6f)))
                }
            }
        }
    }
}

@Composable
private fun DayRow(day: DailyForecast, isToday: Boolean, weekLow: Double, weekHigh: Double) {
    val colors = AuraTheme.colors
    val locale = LocalConfiguration.current.locales[0]
    val label = if (isToday) {
        stringResource(R.string.date_today)
    } else {
        day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).replaceFirstChar { it.titlecase(locale) }
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary, modifier = Modifier.width(64.dp))
        WeatherIcon(day.condition, isDay = true, size = 30.dp, animate = false)
        Text(
            text = if (day.precipitationChance >= 20) "${day.precipitationChance}%" else "",
            style = MaterialTheme.typography.labelSmall,
            color = colors.accent.primary,
            modifier = Modifier.width(44.dp).padding(start = 6.dp),
        )
        Text(WeatherFormatting.temperature(day.low), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
        TemperatureBar(day.low, day.high, weekLow, weekHigh, Modifier.weight(1f).padding(horizontal = 10.dp))
        Text(WeatherFormatting.temperature(day.high), style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary, modifier = Modifier.width(40.dp))
    }
}

/** The day's range drawn on the week's overall scale. */
@Composable
private fun TemperatureBar(low: Double, high: Double, weekLow: Double, weekHigh: Double, modifier: Modifier = Modifier) {
    val colors = AuraTheme.colors
    val span = (weekHigh - weekLow).takeIf { it > 0 } ?: 1.0
    val start = ((low - weekLow) / span).toFloat().coerceIn(0f, 1f)
    val end = ((high - weekLow) / span).toFloat().coerceIn(start, 1f)
    BoxWithConstraints(modifier.height(6.dp).clip(RoundedCornerShape(50)).background(colors.accent.primary.copy(alpha = 0.12f))) {
        Box(
            Modifier
                .offset(x = maxWidth * start)
                .width(maxWidth * (end - start).coerceAtLeast(0.06f))
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(colors.accent.gradientEnd, colors.accent.gradientStart))),
        )
    }
}
