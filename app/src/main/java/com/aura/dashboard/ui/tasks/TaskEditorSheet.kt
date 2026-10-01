package com.aura.dashboard.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.dashboard.R
import com.aura.dashboard.domain.model.Priority
import com.aura.dashboard.domain.model.Repeat
import com.aura.dashboard.ui.components.AuraChip
import com.aura.dashboard.ui.components.chipOnCardColor
import com.aura.dashboard.ui.components.AuraDatePickerDialog
import com.aura.dashboard.ui.components.AuraTimePickerDialog
import com.aura.dashboard.ui.components.GradientButton
import com.aura.dashboard.ui.components.SoftButton
import com.aura.dashboard.ui.components.rememberNotificationPermissionState
import com.aura.dashboard.ui.theme.AuraTheme
import com.aura.dashboard.ui.util.Formatting
import kotlinx.coroutines.launch

/** Bottom sheet for creating and editing a task. Renders nothing while closed. */
@Composable
fun TaskEditorSheet(viewModel: TaskEditorViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (!state.isOpen) return

    val colors = AuraTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val notificationPermission = rememberNotificationPermissionState()
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    val titleFocus = remember { FocusRequester() }
    val draft = state.draft

    fun close() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { viewModel.dismiss() }
    }

    ModalBottomSheet(
        onDismissRequest = viewModel::dismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Text(
                text = stringResource(if (state.isNew) R.string.task_editor_new else R.string.task_editor_edit),
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(16.dp))

            EditorTextField(
                value = draft.title,
                onValueChange = { value -> viewModel.update { it.copy(title = value) } },
                placeholder = stringResource(R.string.task_editor_title_hint),
                large = true,
                modifier = Modifier.focusRequester(titleFocus),
            )
            Spacer(Modifier.height(8.dp))
            EditorTextField(
                value = draft.notes,
                onValueChange = { value -> viewModel.update { it.copy(notes = value) } },
                placeholder = stringResource(R.string.task_editor_notes_hint),
                large = false,
            )

            EditorLabel(stringResource(R.string.task_editor_when))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ValueChip(
                    icon = Icons.Rounded.CalendarToday,
                    text = draft.dueDate?.let { Formatting.relativeDate(context, it, state.today) }
                        ?: stringResource(R.string.task_editor_no_date),
                    active = draft.dueDate != null,
                    onClick = { showDatePicker = true },
                    onClear = if (draft.dueDate != null) ({ viewModel.update { it.copy(dueDate = null) } }) else null,
                    clearLabel = stringResource(R.string.task_editor_clear_date),
                )
                if (draft.dueDate != null) {
                    ValueChip(
                        icon = Icons.Rounded.Schedule,
                        text = draft.dueTime?.let { Formatting.time(context, it) } ?: stringResource(R.string.task_editor_add_time),
                        active = draft.dueTime != null,
                        onClick = { showTimePicker = true },
                        onClear = if (draft.dueTime != null) ({ viewModel.update { it.copy(dueTime = null) } }) else null,
                        clearLabel = stringResource(R.string.task_editor_clear_time),
                    )
                }
            }

            EditorLabel(stringResource(R.string.task_editor_priority))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Priority.entries.forEach { priority ->
                    AuraChip(
                        unselectedColor = chipOnCardColor(),
                        label = Formatting.priorityLabel(context, priority),
                        selected = draft.priority == priority,
                        icon = if (priority == Priority.HIGH) Icons.Rounded.Flag else null,
                        onClick = { viewModel.update { it.copy(priority = priority) } },
                    )
                }
            }

            if (draft.dueDate != null) {
                EditorLabel(stringResource(R.string.task_editor_repeat))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Repeat.entries.forEach { repeat ->
                        AuraChip(
                            unselectedColor = chipOnCardColor(),
                            label = Formatting.repeatLabel(context, repeat),
                            selected = draft.repeat == repeat,
                            onClick = { viewModel.update { it.copy(repeat = repeat) } },
                        )
                    }
                }
            }

            if (draft.dueDate != null && draft.dueTime != null) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.accent.primary.copy(alpha = 0.08f))
                        .clickable(role = Role.Switch) { viewModel.update { it.copy(remind = !it.remind) } }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = colors.accent.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.task_editor_notify),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = draft.remind,
                        onCheckedChange = null,
                        colors = SwitchDefaults.colors(checkedTrackColor = colors.accent.primary),
                    )
                }
                if (draft.remind && !notificationPermission.granted) {
                    Spacer(Modifier.height(8.dp))
                    PermissionHint(
                        text = stringResource(R.string.permission_notifications_hint),
                        action = stringResource(R.string.action_allow),
                        onAction = notificationPermission::request,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!state.isNew) {
                    SoftButton(
                        text = stringResource(R.string.action_delete),
                        icon = Icons.Rounded.DeleteOutline,
                        contentColor = colors.danger,
                        onClick = { scope.launch { if (viewModel.delete()) close() } },
                    )
                    Spacer(Modifier.width(12.dp))
                }
                GradientButton(
                    text = stringResource(if (state.isNew) R.string.task_editor_add else R.string.action_save),
                    enabled = state.canSave,
                    onClick = { scope.launch { if (viewModel.save()) close() } },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        if (state.isNew) runCatching { titleFocus.requestFocus() }
    }

    if (showDatePicker) {
        AuraDatePickerDialog(
            initial = draft.dueDate ?: state.today,
            onConfirm = { date ->
                viewModel.update { it.copy(dueDate = date) }
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
    if (showTimePicker) {
        AuraTimePickerDialog(
            initial = draft.dueTime,
            onConfirm = { time ->
                viewModel.update { it.copy(dueTime = time) }
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
        )
    }
}

@Composable
internal fun EditorTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    large: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val style = if (large) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyLarge
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = style.copy(color = colors.textPrimary),
        cursorBrush = SolidColor(colors.accent.primary),
        singleLine = large,
        maxLines = if (large) 1 else 6,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = if (large) ImeAction.Next else ImeAction.Default,
        ),
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = placeholder }
            .clip(RoundedCornerShape(16.dp))
            .background(colors.accent.primary.copy(alpha = if (colors.isDark) 0.08f else 0.05f))
            .padding(horizontal = 16.dp, vertical = if (large) 14.dp else 12.dp),
        decorationBox = { inner ->
            if (value.isEmpty()) {
                Text(placeholder, style = style, color = colors.textTertiary)
            }
            inner()
        },
    )
}

@Composable
internal fun EditorLabel(text: String) {
    Spacer(Modifier.height(20.dp))
    Text(text, style = MaterialTheme.typography.labelMedium, color = AuraTheme.colors.textSecondary)
    Spacer(Modifier.height(8.dp))
}

/** A tappable value (date/time) with an optional clear button. */
@Composable
internal fun ValueChip(
    icon: ImageVector,
    text: String,
    active: Boolean,
    onClick: () -> Unit,
    onClear: (() -> Unit)?,
    clearLabel: String,
) {
    val colors = AuraTheme.colors
    Row(
        modifier = Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(50))
            .background(if (active) colors.accent.container else colors.accent.primary.copy(alpha = 0.06f))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 14.dp, end = if (onClear != null) 4.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = if (active) colors.accent.onContainer else colors.textSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = if (active) colors.accent.onContainer else colors.textSecondary)
        if (onClear != null) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = clearLabel,
                tint = colors.accent.onContainer,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(36.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable(role = Role.Button, onClick = onClear)
                    .padding(9.dp),
            )
        }
    }
}

@Composable
internal fun PermissionHint(text: String, action: String, onAction: () -> Unit) {
    val colors = AuraTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.warning.copy(alpha = 0.12f))
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = colors.textPrimary, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        SoftButton(text = action, onClick = onAction, contentColor = colors.warning)
    }
}
