package com.aura.dashboard.ui.reminders

import android.os.Build
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AlarmOn
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.dashboard.R
import com.aura.dashboard.domain.model.Reminder
import com.aura.dashboard.domain.model.Repeat
import com.aura.dashboard.ui.components.AuraFab
import com.aura.dashboard.ui.components.EmptyState
import com.aura.dashboard.ui.components.GlassCard
import com.aura.dashboard.ui.components.GlassIconButton
import com.aura.dashboard.ui.components.InfoPill
import com.aura.dashboard.ui.components.ScreenHeader
import com.aura.dashboard.ui.components.SoftButton
import com.aura.dashboard.ui.components.rememberExactAlarmPermissionState
import com.aura.dashboard.ui.components.rememberNotificationPermissionState
import com.aura.dashboard.ui.navigation.LocalSnackbarHostState
import com.aura.dashboard.ui.navigation.NO_ID
import com.aura.dashboard.ui.theme.AuraTheme
import com.aura.dashboard.ui.util.Formatting
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun RemindersScreen(
    highlightId: Long,
    startCreating: Boolean,
    onBack: () -> Unit,
    viewModel: RemindersViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val snackbar = LocalSnackbarHostState.current
    val notifications = rememberNotificationPermissionState()
    val exactAlarms = rememberExactAlarmPermissionState()
    val listState = rememberLazyListState()
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    var handledCreate by rememberSaveable { mutableStateOf(false) }
    var handledHighlight by rememberSaveable { mutableLongStateOf(NO_ID) }
    var activeHighlight by rememberSaveable { mutableLongStateOf(NO_ID) }

    LaunchedEffect(startCreating) {
        if (startCreating && !handledCreate) {
            handledCreate = true
            viewModel.openNew()
        }
    }
    LaunchedEffect(highlightId, state.isLoading) {
        if (highlightId != NO_ID && highlightId != handledHighlight && !state.isLoading) {
            handledHighlight = highlightId
            activeHighlight = highlightId
            val index = (state.upcoming + state.paused + state.past).indexOfFirst { it.id == highlightId }
            if (index >= 0) listState.animateScrollToItem(index)
            kotlinx.coroutines.delay(4_000)
            activeHighlight = NO_ID
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ReminderEvent.Saved -> {
                    val reminder = event.reminder
                    val whenText = reminder?.nextTriggerAt?.atZone(ZoneId.systemDefault())?.let {
                        "${Formatting.relativeDate(context, it.toLocalDate(), LocalDate.now())}, ${Formatting.time(context, it.toLocalTime())}"
                    }
                    snackbar.showSnackbar(
                        if (whenText != null) resources.getString(R.string.reminder_set_for, whenText)
                        else resources.getString(R.string.reminder_saved),
                    )
                }
                is ReminderEvent.Deleted -> {
                    val result = snackbar.showSnackbar(
                        resources.getString(R.string.reminder_deleted),
                        actionLabel = resources.getString(R.string.action_undo),
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete(event.reminder)
                }
                ReminderEvent.SchedulingFailed -> snackbar.showSnackbar(resources.getString(R.string.reminder_scheduling_failed))
                ReminderEvent.Error -> snackbar.showSnackbar(resources.getString(R.string.error_generic))
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = topPadding + 12.dp, bottom = bottomPadding + 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "header") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlassIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        onClick = onBack,
                    )
                    Spacer(Modifier.width(12.dp))
                    ScreenHeader(
                        title = stringResource(R.string.reminders_title),
                        subtitle = pluralStringResource(R.plurals.reminders_subtitle, state.upcoming.size, state.upcoming.size),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (!state.remindersEnabled) {
                item(key = "disabled") {
                    Banner(
                        text = stringResource(R.string.reminders_globally_disabled),
                        action = null,
                        onAction = {},
                    )
                }
            } else if (!notifications.granted) {
                item(key = "notif-permission") {
                    Banner(
                        text = stringResource(R.string.permission_notifications_banner),
                        action = stringResource(R.string.action_allow),
                        onAction = notifications::request,
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !exactAlarms.granted && state.upcoming.isNotEmpty()) {
                item(key = "exact-permission") {
                    Banner(
                        text = stringResource(R.string.permission_exact_alarm_banner),
                        action = stringResource(R.string.action_allow),
                        onAction = exactAlarms::request,
                    )
                }
            }

            if (!state.isLoading && state.isEmpty) {
                item(key = "empty") {
                    EmptyState(
                        icon = Icons.Rounded.AlarmOn,
                        title = stringResource(R.string.reminders_empty_title),
                        message = stringResource(R.string.reminders_empty_message),
                        actionLabel = stringResource(R.string.reminder_add),
                        onAction = viewModel::openNew,
                    )
                }
            }

            reminderSection(R.string.reminders_section_upcoming, state.upcoming, state, activeHighlight, viewModel, showSwitch = true)
            reminderSection(R.string.reminders_section_paused, state.paused, state, activeHighlight, viewModel, showSwitch = true)
            reminderSection(R.string.reminders_section_past, state.past, state, activeHighlight, viewModel, showSwitch = false)
        }

        AuraFab(
            contentDescription = stringResource(R.string.reminder_add),
            onClick = viewModel::openNew,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = bottomPadding + 20.dp),
        )
    }

    ReminderEditorSheet(viewModel)
}

private fun androidx.compose.foundation.lazy.LazyListScope.reminderSection(
    title: Int,
    reminders: List<Reminder>,
    state: RemindersUiState,
    highlightId: Long,
    viewModel: RemindersViewModel,
    showSwitch: Boolean,
) {
    if (reminders.isEmpty()) return
    item(key = "section-$title") {
        Text(
            text = androidx.compose.ui.res.stringResource(title),
            style = MaterialTheme.typography.labelLarge,
            color = AuraTheme.colors.textSecondary,
            modifier = Modifier
                .animateItem()
                .padding(top = 8.dp, start = 4.dp)
                .semantics { heading() },
        )
    }
    items(reminders, key = { it.id }) { reminder ->
        ReminderCard(
            reminder = reminder,
            today = state.today,
            highlighted = reminder.id == highlightId,
            showSwitch = showSwitch,
            onToggle = { viewModel.setEnabled(reminder, it) },
            onClick = { viewModel.openExisting(reminder.id) },
            modifier = Modifier.animateItem(),
        )
    }
}

@Composable
private fun ReminderCard(
    reminder: Reminder,
    today: LocalDate,
    highlighted: Boolean,
    showSwitch: Boolean,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val context = LocalContext.current
    val active = reminder.isEnabled && !reminder.isFinished
    val pulse = if (highlighted) {
        val transition = rememberInfiniteTransition(label = "highlight")
        transition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "highlightAlpha").value
    } else {
        0f
    }
    val trigger = (reminder.nextTriggerAt ?: reminder.lastTriggeredAt)?.atZone(ZoneId.systemDefault())
    val date = trigger?.toLocalDate() ?: reminder.startDate
    val time = trigger?.toLocalTime() ?: reminder.time

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .border(2.dp, colors.accent.primary.copy(alpha = pulse), RoundedCornerShape(26.dp)),
        onClick = onClick,
        onClickLabel = stringResource(R.string.reminder_action_edit),
        shape = RoundedCornerShape(26.dp),
        contentPadding = PaddingValues(start = 20.dp, end = 14.dp, top = 16.dp, bottom = 16.dp),
        elevation = 10.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = Formatting.time(context, time),
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (active) colors.accent.primary else colors.textTertiary,
                )
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (active) colors.textPrimary else colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (reminder.description.isNotBlank()) {
                    Text(
                        text = reminder.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    InfoPill(
                        text = if (reminder.isFinished) {
                            stringResource(R.string.reminder_done_on, Formatting.relativeDate(context, date, today))
                        } else {
                            Formatting.relativeDate(context, date, today)
                        },
                        icon = Icons.Rounded.Timer,
                        color = colors.textSecondary,
                    )
                    if (reminder.repeat != Repeat.NEVER) {
                        InfoPill(
                            text = Formatting.repeatLabel(context, reminder.repeat),
                            icon = Icons.Rounded.Repeat,
                            color = colors.accent.primary,
                        )
                    }
                    if (!reminder.isEnabled) {
                        InfoPill(
                            text = stringResource(R.string.reminder_paused),
                            icon = Icons.Rounded.NotificationsOff,
                            color = colors.textSecondary,
                        )
                    }
                }
            }
            if (showSwitch) {
                val description = stringResource(R.string.reminder_toggle_label, reminder.title)
                Switch(
                    checked = reminder.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = colors.accent.primary,
                        uncheckedBorderColor = Color.Transparent,
                    ),
                    modifier = Modifier.semantics { contentDescription = description },
                )
            }
        }
    }
}

@Composable
private fun Banner(text: String, action: String?, onAction: () -> Unit) {
    val colors = AuraTheme.colors
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 18.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
        shape = RoundedCornerShape(22.dp),
        elevation = 6.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary, modifier = Modifier.weight(1f))
            if (action != null) {
                Spacer(Modifier.width(10.dp))
                SoftButton(text = action, onClick = onAction)
            }
        }
    }
}
