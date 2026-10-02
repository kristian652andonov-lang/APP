package com.aura.dashboard.ui.settings

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.AlarmOn
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Source
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.dashboard.R
import com.aura.dashboard.data.news.Countries
import com.aura.dashboard.domain.model.AccentColor
import com.aura.dashboard.domain.model.ClockStyle
import com.aura.dashboard.domain.model.IconStyle
import com.aura.dashboard.domain.model.TemperatureUnit
import com.aura.dashboard.ui.weather.LocationPickerSheet
import com.aura.dashboard.ui.weather.WeatherEvent
import com.aura.dashboard.ui.weather.WeatherFormatting
import com.aura.dashboard.ui.weather.WeatherViewModel
import com.aura.dashboard.domain.model.NewsCategory
import com.aura.dashboard.domain.model.NewsRefreshInterval
import com.aura.dashboard.domain.model.Priority
import com.aura.dashboard.domain.model.ThemeMode
import com.aura.dashboard.domain.model.UserPreferences
import com.aura.dashboard.ui.components.AuraChip
import com.aura.dashboard.ui.components.chipOnCardColor
import com.aura.dashboard.ui.components.Avatar
import com.aura.dashboard.ui.components.GlassCard
import com.aura.dashboard.ui.components.ScreenHeader
import com.aura.dashboard.ui.components.SoftButton
import com.aura.dashboard.ui.components.openBatteryOptimizationSettings
import com.aura.dashboard.ui.components.rememberExactAlarmPermissionState
import com.aura.dashboard.ui.components.rememberNotificationPermissionState
import com.aura.dashboard.ui.navigation.LegalDocument
import com.aura.dashboard.ui.navigation.LocalBottomBarPadding
import com.aura.dashboard.ui.theme.AuraTheme
import com.aura.dashboard.ui.theme.accentPalette
import com.aura.dashboard.ui.util.Formatting

@Composable
fun SettingsScreen(
    onOpenWeather: () -> Unit,
    onOpenLegal: (LegalDocument) -> Unit,
    onManageApps: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = state.preferences
    val context = LocalContext.current
    val colors = AuraTheme.colors
    val bottomPadding = LocalBottomBarPadding.current
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val notifications = rememberNotificationPermissionState()
    val exactAlarms = rememberExactAlarmPermissionState()
    var editingName by rememberSaveable { mutableStateOf(false) }
    var pickingCountry by rememberSaveable { mutableStateOf(false) }
    var avatarMenu by rememberSaveable { mutableStateOf(false) }
    var pickingBackground by rememberSaveable { mutableStateOf(false) }
    var pickingWeatherLocation by rememberSaveable { mutableStateOf(false) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.setAvatar(uri)
    }
    val country = Countries.byCode(prefs.countryCode)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = topPadding + 16.dp, bottom = bottomPadding + 8.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item { ScreenHeader(title = stringResource(R.string.settings_title)) }

        item {
            ProfileCard(
                prefs = prefs,
                onEditName = { editingName = true },
                onAvatarClick = { avatarMenu = true },
                avatarMenu = avatarMenu,
                onDismissAvatarMenu = { avatarMenu = false },
                onPickPhoto = {
                    avatarMenu = false
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onRemovePhoto = {
                    avatarMenu = false
                    viewModel.removeAvatar()
                },
            )
        }

        item {
            SettingsGroup(stringResource(R.string.settings_section_profile)) {
                SettingsRow(
                    icon = Icons.Rounded.Person,
                    title = stringResource(R.string.settings_name),
                    subtitle = prefs.userName.ifBlank { stringResource(R.string.settings_name_not_set) },
                    onClick = { editingName = true },
                )
                GroupDivider()
                SettingsRow(
                    icon = Icons.Rounded.Public,
                    title = stringResource(R.string.settings_country),
                    subtitle = "${country.flag} ${country.displayName}",
                    onClick = { pickingCountry = true },
                )
            }
        }

        item {
            SettingsGroup(stringResource(R.string.settings_section_appearance)) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.selectableGroup()) {
                        ThemeMode.entries.forEach { mode ->
                            AuraChip(
                                unselectedColor = chipOnCardColor(),
                                label = stringResource(
                                    when (mode) {
                                        ThemeMode.SYSTEM -> R.string.settings_theme_system
                                        ThemeMode.LIGHT -> R.string.settings_theme_light
                                        ThemeMode.DARK -> R.string.settings_theme_dark
                                    },
                                ),
                                selected = prefs.themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(stringResource(R.string.settings_accent), style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.selectableGroup()) {
                        AccentColor.entries.forEach { accent ->
                            AccentSwatch(
                                accent = accent,
                                selected = prefs.accentColor == accent,
                                onClick = { viewModel.setAccent(accent) },
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(stringResource(R.string.settings_style), style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
                    Spacer(Modifier.height(10.dp))
                }
                Box(Modifier.padding(bottom = 6.dp).selectableGroup()) {
                    StylePicker(selected = prefs.appStyle, onSelect = viewModel::setAppStyle)
                }
                GroupDivider()
                SettingsRow(
                    icon = Icons.Rounded.AutoAwesome,
                    title = stringResource(R.string.settings_background),
                    subtitle = stringResource(prefs.backgroundEffect.labelRes()),
                    onClick = { pickingBackground = true },
                )
                GroupDivider()
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Schedule, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_clock_style), style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.selectableGroup()) {
                        ClockStyle.entries.forEach { style ->
                            AuraChip(
                                unselectedColor = chipOnCardColor(),
                                label = stringResource(if (style == ClockStyle.DIGITAL) R.string.clock_digital else R.string.clock_analog),
                                selected = prefs.clockStyle == style,
                                onClick = { viewModel.setClockStyle(style) },
                            )
                        }
                    }
                }
            }
        }

        item {
            SettingsGroup(stringResource(R.string.settings_section_weather)) {
                SettingsRow(
                    icon = Icons.Rounded.Place,
                    title = stringResource(R.string.settings_weather_location),
                    subtitle = state.weatherLocation?.let { WeatherFormatting.locationName(context, it) }
                        ?: stringResource(R.string.weather_choose_location),
                    onClick = { pickingWeatherLocation = true },
                )
                GroupDivider()
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Thermostat, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_temperature_unit), style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.selectableGroup()) {
                        TemperatureUnit.entries.forEach { unit ->
                            AuraChip(
                                unselectedColor = chipOnCardColor(),
                                label = stringResource(if (unit == TemperatureUnit.CELSIUS) R.string.unit_celsius else R.string.unit_fahrenheit),
                                selected = prefs.temperatureUnit == unit,
                                onClick = { viewModel.setTemperatureUnit(unit) },
                            )
                        }
                    }
                }
                GroupDivider()
                SettingsRow(
                    icon = Icons.Rounded.WbSunny,
                    title = stringResource(R.string.settings_open_forecast),
                    onClick = onOpenWeather,
                )
            }
        }

        item {
            SettingsGroup(stringResource(R.string.settings_section_notifications)) {
                SwitchRow(
                    icon = Icons.Rounded.NotificationsActive,
                    title = stringResource(R.string.settings_reminders_enabled),
                    subtitle = stringResource(R.string.settings_reminders_enabled_subtitle),
                    checked = prefs.remindersEnabled,
                    onCheckedChange = viewModel::setRemindersEnabled,
                )
                GroupDivider()
                SwitchRow(
                    icon = Icons.AutoMirrored.Rounded.VolumeUp,
                    title = stringResource(R.string.settings_sound),
                    checked = prefs.notificationSound,
                    onCheckedChange = viewModel::setSound,
                    enabled = prefs.remindersEnabled,
                )
                GroupDivider()
                SwitchRow(
                    icon = Icons.Rounded.Vibration,
                    title = stringResource(R.string.settings_vibration),
                    checked = prefs.notificationVibration,
                    onCheckedChange = viewModel::setVibration,
                    enabled = prefs.remindersEnabled,
                )
                GroupDivider()
                SettingsRow(
                    icon = Icons.Rounded.Shield,
                    title = stringResource(R.string.settings_permission),
                    subtitle = stringResource(
                        if (notifications.granted) R.string.settings_permission_granted else R.string.settings_permission_denied,
                    ),
                    showChevron = false,
                ) {
                    if (!notifications.granted) SoftButton(text = stringResource(R.string.action_allow), onClick = notifications::request)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    GroupDivider()
                    SettingsRow(
                        icon = Icons.Rounded.AlarmOn,
                        title = stringResource(R.string.settings_exact_alarms),
                        subtitle = stringResource(
                            if (exactAlarms.granted) R.string.settings_exact_alarms_granted else R.string.settings_exact_alarms_denied,
                        ),
                        showChevron = false,
                    ) {
                        if (!exactAlarms.granted) SoftButton(text = stringResource(R.string.action_allow), onClick = exactAlarms::request)
                    }
                }
                GroupDivider()
                SettingsRow(
                    icon = Icons.Rounded.BatterySaver,
                    title = stringResource(R.string.settings_battery),
                    subtitle = stringResource(R.string.settings_battery_subtitle),
                    onClick = { openBatteryOptimizationSettings(context) },
                )
            }
        }

        item {
            SettingsGroup(stringResource(R.string.settings_section_news)) {
                SettingsRow(
                    icon = Icons.Rounded.Public,
                    title = stringResource(R.string.settings_news_country),
                    subtitle = "${country.flag} ${country.displayName}",
                    onClick = { pickingCountry = true },
                )
                GroupDivider()
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Category, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_news_categories), style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
                    }
                    Spacer(Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.newsSource.categories.forEach { category ->
                            AuraChip(
                                unselectedColor = chipOnCardColor(),
                                label = Formatting.categoryLabel(context, category),
                                selected = category in prefs.newsCategories || category == NewsCategory.TOP,
                                icon = if (category in prefs.newsCategories || category == NewsCategory.TOP) Icons.Rounded.Check else null,
                                onClick = { if (category != NewsCategory.TOP) viewModel.toggleCategory(category) },
                            )
                        }
                    }
                }
                GroupDivider()
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Rounded.Article, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_news_count), style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.selectableGroup()) {
                        UserPreferences.ARTICLE_COUNT_OPTIONS.forEach { count ->
                            AuraChip(unselectedColor = chipOnCardColor(), label = count.toString(), selected = prefs.newsArticleCount == count, onClick = { viewModel.setArticleCount(count) })
                        }
                    }
                }
                GroupDivider()
                RefreshIntervalRow(prefs.newsRefreshInterval, viewModel::setRefreshInterval)
                GroupDivider()
                SettingsRow(
                    icon = Icons.Rounded.Source,
                    title = stringResource(R.string.settings_news_source),
                    subtitle = if (state.newsSource.isAvailable) {
                        val publishers = state.newsSource.publishers.joinToString(", ")
                        if (publishers.isNotEmpty()) "${state.newsSource.providerName}: $publishers" else state.newsSource.providerName
                    } else {
                        stringResource(R.string.settings_news_source_unavailable)
                    },
                    showChevron = false,
                )
            }
        }

        item {
            SettingsGroup(stringResource(R.string.settings_section_apps)) {
                SettingsRow(
                    icon = Icons.Rounded.Apps,
                    title = stringResource(R.string.settings_manage_apps),
                    subtitle = stringResource(R.string.settings_manage_apps_subtitle),
                    onClick = onManageApps,
                )
                GroupDivider()
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(stringResource(R.string.settings_icon_style), style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
                    Text(stringResource(R.string.settings_icon_style_hint), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.selectableGroup()) {
                        IconStyle.entries.forEach { style ->
                            AuraChip(
                                unselectedColor = chipOnCardColor(),
                                label = stringResource(if (style == IconStyle.THEMED) R.string.icons_themed else R.string.icons_original),
                                selected = prefs.iconStyle == style,
                                onClick = { viewModel.setIconStyle(style) },
                            )
                        }
                    }
                }
            }
        }

        item {
            SettingsGroup(stringResource(R.string.settings_section_tasks)) {
                SwitchRow(
                    icon = Icons.Rounded.CheckCircle,
                    title = stringResource(R.string.settings_show_completed),
                    checked = prefs.showCompletedTasks,
                    onCheckedChange = viewModel::setShowCompleted,
                )
                GroupDivider()
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Flag, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_default_priority), style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.selectableGroup()) {
                        Priority.entries.forEach { priority ->
                            AuraChip(
                                unselectedColor = chipOnCardColor(),
                                label = Formatting.priorityLabel(context, priority),
                                selected = prefs.defaultPriority == priority,
                                onClick = { viewModel.setDefaultPriority(priority) },
                            )
                        }
                    }
                }
            }
        }

        item {
            SettingsGroup(stringResource(R.string.settings_section_about)) {
                SettingsRow(
                    icon = Icons.Rounded.Info,
                    title = stringResource(R.string.settings_version),
                    subtitle = state.appVersion,
                    showChevron = false,
                )
                GroupDivider()
                SettingsRow(
                    icon = Icons.Rounded.Shield,
                    title = stringResource(R.string.settings_privacy),
                    onClick = { onOpenLegal(LegalDocument.PRIVACY) },
                )
                GroupDivider()
                SettingsRow(
                    icon = Icons.Rounded.Tune,
                    title = stringResource(R.string.settings_terms),
                    onClick = { onOpenLegal(LegalDocument.TERMS) },
                )
                GroupDivider()
                SettingsRow(
                    icon = Icons.Rounded.Code,
                    title = stringResource(R.string.settings_licenses),
                    onClick = { onOpenLegal(LegalDocument.LICENSES) },
                )
            }
        }
    }

    if (editingName) {
        NameDialog(initial = prefs.userName, onConfirm = { viewModel.setName(it); editingName = false }, onDismiss = { editingName = false })
    }
    if (pickingBackground) {
        BackgroundPickerSheet(
            selected = prefs.backgroundEffect,
            autoPreview = state.autoBackground,
            onSelect = viewModel::setBackground,
            onDismiss = { pickingBackground = false },
        )
    }
    if (pickingWeatherLocation) {
        val weatherViewModel: WeatherViewModel = hiltViewModel()
        LaunchedEffect(weatherViewModel) {
            weatherViewModel.events.collect { if (it is WeatherEvent.LocationUpdated) pickingWeatherLocation = false }
        }
        LocationPickerSheet(viewModel = weatherViewModel, onDismiss = { pickingWeatherLocation = false })
    }
    if (pickingCountry) {
        CountryPickerSheet(
            selectedCode = prefs.countryCode,
            onSelect = {
                viewModel.setCountry(it.code)
                pickingCountry = false
            },
            onDismiss = { pickingCountry = false },
        )
    }
}

@Composable
private fun ProfileCard(
    prefs: UserPreferences,
    onEditName: () -> Unit,
    onAvatarClick: () -> Unit,
    avatarMenu: Boolean,
    onDismissAvatarMenu: () -> Unit,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
) {
    val colors = AuraTheme.colors
    GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onEditName) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClickLabel = stringResource(R.string.settings_change_photo), onClick = onAvatarClick),
                ) {
                    Avatar(name = prefs.userName, imagePath = prefs.avatarPath, size = 68.dp)
                }
                DropdownMenu(expanded = avatarMenu, onDismissRequest = onDismissAvatarMenu) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.settings_choose_photo)) }, onClick = onPickPhoto)
                    if (prefs.avatarPath != null) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.settings_remove_photo)) }, onClick = onRemovePhoto)
                    }
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = prefs.userName.ifBlank { stringResource(R.string.settings_name_not_set) },
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                )
                Text(
                    text = stringResource(R.string.settings_profile_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun AccentSwatch(accent: AccentColor, selected: Boolean, onClick: () -> Unit) {
    val colors = AuraTheme.colors
    val palette = accentPalette(accent, colors.isDark)
    val label = stringResource(
        when (accent) {
            AccentColor.LAVENDER -> R.string.accent_lavender
            AccentColor.OCEAN -> R.string.accent_ocean
            AccentColor.ROSE -> R.string.accent_rose
            AccentColor.MINT -> R.string.accent_mint
            AccentColor.PEACH -> R.string.accent_peach
        },
    )
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(palette.gradientStart, palette.primary)))
                .then(if (selected) Modifier.border(3.dp, Color.White.copy(alpha = 0.9f), CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun RefreshIntervalRow(current: NewsRefreshInterval, onSelect: (NewsRefreshInterval) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    Box {
        SettingsRow(
            icon = Icons.Rounded.Schedule,
            title = stringResource(R.string.settings_news_refresh),
            subtitle = stringResource(refreshLabel(current)),
            onClick = { open = true },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            NewsRefreshInterval.entries.forEach { interval ->
                DropdownMenuItem(
                    text = { Text(stringResource(refreshLabel(interval))) },
                    onClick = {
                        onSelect(interval)
                        open = false
                    },
                    trailingIcon = { if (interval == current) Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.state_selected)) },
                )
            }
        }
    }
}

private fun refreshLabel(interval: NewsRefreshInterval): Int = when (interval) {
    NewsRefreshInterval.MANUAL -> R.string.refresh_manual
    NewsRefreshInterval.HOURLY -> R.string.refresh_hourly
    NewsRefreshInterval.EVERY_3_HOURS -> R.string.refresh_3h
    NewsRefreshInterval.EVERY_6_HOURS -> R.string.refresh_6h
    NewsRefreshInterval.EVERY_12_HOURS -> R.string.refresh_12h
    NewsRefreshInterval.DAILY -> R.string.refresh_daily
}

@Composable
private fun NameDialog(initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_name)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(40) },
                singleLine = true,
                label = { Text(stringResource(R.string.onboarding_name_hint)) },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(name) }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
