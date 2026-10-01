package com.aura.dashboard.ui.reminders

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.dashboard.R
import com.aura.dashboard.domain.model.Repeat
import com.aura.dashboard.ui.components.AuraChip
import com.aura.dashboard.ui.components.chipOnCardColor
import com.aura.dashboard.ui.components.AuraDatePickerDialog
import com.aura.dashboard.ui.components.AuraTimePickerDialog
import com.aura.dashboard.ui.components.GradientButton
import com.aura.dashboard.ui.components.SoftButton
import com.aura.dashboard.ui.tasks.EditorLabel
import com.aura.dashboard.ui.tasks.EditorTextField
import com.aura.dashboard.ui.tasks.ValueChip
import com.aura.dashboard.ui.theme.AuraTheme
import com.aura.dashboard.ui.util.Formatting
import kotlinx.coroutines.launch

@Composable
fun ReminderEditorSheet(viewModel: RemindersViewModel) {
    val state by viewModel.editor.collectAsStateWithLifecycle()
    if (!state.isOpen) return

    val colors = AuraTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    val titleFocus = remember { FocusRequester() }
    val draft = state.draft

    fun close() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { viewModel.dismissEditor() }
    }

    ModalBottomSheet(
        onDismissRequest = viewModel::dismissEditor,
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
                text = stringResource(if (state.isNew) R.string.reminder_editor_new else R.string.reminder_editor_edit),
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(16.dp))
            EditorTextField(
                value = draft.title,
                onValueChange = { value -> viewModel.updateDraft { it.copy(title = value) } },
                placeholder = stringResource(R.string.reminder_editor_title_hint),
                large = true,
                modifier = Modifier.focusRequester(titleFocus),
            )
            Spacer(Modifier.height(8.dp))
            EditorTextField(
                value = draft.description,
                onValueChange = { value -> viewModel.updateDraft { it.copy(description = value) } },
                placeholder = stringResource(R.string.reminder_editor_description_hint),
                large = false,
            )

            EditorLabel(stringResource(R.string.reminder_editor_when))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ValueChip(
                    icon = Icons.Rounded.CalendarToday,
                    text = Formatting.relativeDate(context, draft.date, state.today),
                    active = true,
                    onClick = { showDatePicker = true },
                    onClear = null,
                    clearLabel = "",
                )
                ValueChip(
                    icon = Icons.Rounded.Schedule,
                    text = Formatting.time(context, draft.time),
                    active = true,
                    onClick = { showTimePicker = true },
                    onClear = null,
                    clearLabel = "",
                )
            }
            if (state.timeInPast) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.reminder_editor_time_in_past),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.danger,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                )
            }

            EditorLabel(stringResource(R.string.reminder_editor_repeat))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Repeat.entries.forEach { repeat ->
                    AuraChip(
                        unselectedColor = chipOnCardColor(),
                        label = Formatting.repeatLabel(context, repeat),
                        selected = draft.repeat == repeat,
                        onClick = { viewModel.updateDraft { it.copy(repeat = repeat) } },
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
                    text = stringResource(if (state.isNew) R.string.reminder_editor_set else R.string.action_save),
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
            initial = draft.date,
            minDate = state.today,
            onConfirm = { date ->
                viewModel.updateDraft { it.copy(date = date) }
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
    if (showTimePicker) {
        AuraTimePickerDialog(
            initial = draft.time,
            onConfirm = { time ->
                viewModel.updateDraft { it.copy(time = time) }
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
        )
    }
}
