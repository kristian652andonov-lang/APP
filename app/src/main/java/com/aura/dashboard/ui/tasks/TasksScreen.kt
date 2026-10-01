package com.aura.dashboard.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.dashboard.R
import com.aura.dashboard.domain.model.TaskSort
import com.aura.dashboard.domain.schedule.TaskFilter
import com.aura.dashboard.ui.components.AuraChip
import com.aura.dashboard.ui.components.AuraFab
import com.aura.dashboard.ui.components.EmptyState
import com.aura.dashboard.ui.components.GlassIconButton
import com.aura.dashboard.ui.components.ScreenHeader
import com.aura.dashboard.ui.navigation.LocalBottomBarPadding
import com.aura.dashboard.ui.navigation.LocalSnackbarHostState
import com.aura.dashboard.ui.navigation.NO_ID
import com.aura.dashboard.ui.theme.AuraTheme
import com.aura.dashboard.ui.util.Formatting

@Composable
fun TasksScreen(
    openTaskId: Long,
    onOpenReminders: () -> Unit,
    viewModel: TasksViewModel = hiltViewModel(),
    editor: TaskEditorViewModel = hiltViewModel(key = "taskEditor"),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = AuraTheme.colors
    val context = LocalContext.current
    val bottomPadding = LocalBottomBarPadding.current
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    var handledOpenId by rememberSaveable { mutableLongStateOf(NO_ID) }
    var showSortMenu by rememberSaveable { mutableStateOf(false) }
    var showMoreMenu by rememberSaveable { mutableStateOf(false) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(openTaskId) {
        if (openTaskId != NO_ID && openTaskId != handledOpenId) {
            handledOpenId = openTaskId
            editor.openExisting(openTaskId)
        }
    }
    TaskEditorEvents(editor)

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = topPadding + 16.dp, bottom = bottomPadding + 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "header") {
                ScreenHeader(
                    title = stringResource(R.string.tasks_title),
                    subtitle = stringResource(R.string.tasks_subtitle, state.openCount, state.completedTodayCount),
                ) {
                    GlassIconButton(
                        icon = Icons.Rounded.NotificationsNone,
                        contentDescription = stringResource(R.string.reminders_title),
                        onClick = onOpenReminders,
                    )
                    Box {
                        GlassIconButton(
                            icon = Icons.AutoMirrored.Rounded.Sort,
                            contentDescription = stringResource(R.string.tasks_sort),
                            onClick = { showSortMenu = true },
                        )
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            TaskSort.entries.forEach { sort ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(sortLabel(sort))) },
                                    onClick = {
                                        viewModel.setSort(sort)
                                        showSortMenu = false
                                    },
                                    trailingIcon = {
                                        if (sort == state.sort) Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.state_selected))
                                    },
                                )
                            }
                        }
                    }
                    Box {
                        GlassIconButton(
                            icon = Icons.Rounded.MoreVert,
                            contentDescription = stringResource(R.string.action_more),
                            onClick = { showMoreMenu = true },
                        )
                        DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                            DropdownMenuItem(
                                text = {
                                    Text(stringResource(if (state.showCompleted) R.string.tasks_hide_completed else R.string.tasks_show_completed))
                                },
                                onClick = {
                                    viewModel.setShowCompleted(!state.showCompleted)
                                    showMoreMenu = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.tasks_clear_completed)) },
                                enabled = state.hasCompleted,
                                onClick = {
                                    confirmClear = true
                                    showMoreMenu = false
                                },
                            )
                        }
                    }
                }
            }
            item(key = "filters") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
                ) {
                    items(TaskFilter.entries.toList()) { filter ->
                        AuraChip(
                            label = stringResource(filterLabel(filter)),
                            selected = state.filter == filter,
                            onClick = { viewModel.setFilter(filter) },
                        )
                    }
                }
            }

            if (!state.isLoading && state.isEmpty) {
                item(key = "empty") {
                    val (icon, title, message) = when (state.filter) {
                        TaskFilter.ALL -> Triple(Icons.Rounded.TaskAlt, R.string.tasks_empty_all_title, R.string.tasks_empty_all_message)
                        TaskFilter.TODAY -> Triple(Icons.Rounded.WbSunny, R.string.tasks_empty_today_title, R.string.tasks_empty_today_message)
                        TaskFilter.UPCOMING -> Triple(Icons.Rounded.EventAvailable, R.string.tasks_empty_upcoming_title, R.string.tasks_empty_upcoming_message)
                        TaskFilter.COMPLETED -> Triple(Icons.Rounded.DoneAll, R.string.tasks_empty_completed_title, R.string.tasks_empty_completed_message)
                    }
                    EmptyState(
                        icon = icon,
                        title = stringResource(title),
                        message = stringResource(message),
                        actionLabel = if (state.filter != TaskFilter.COMPLETED) stringResource(R.string.task_add) else null,
                        onAction = { editor.openNew(dueToday = state.filter == TaskFilter.TODAY) },
                    )
                }
            }

            state.sections.forEach { section ->
                section.key?.let { key ->
                    item(key = "section-$key") {
                        Text(
                            text = sectionTitle(key, context),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (key == TaskSectionKey.Overdue) colors.danger else colors.textSecondary,
                            modifier = Modifier
                                .animateItem()
                                .padding(top = 10.dp, start = 4.dp)
                                .semantics { heading() },
                        )
                    }
                }
                items(section.tasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        today = state.today,
                        now = state.now,
                        onToggle = { viewModel.toggle(task, it) },
                        onClick = { editor.openExisting(task.id) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }

        AuraFab(
            contentDescription = stringResource(R.string.task_add),
            onClick = { editor.openNew(dueToday = state.filter == TaskFilter.TODAY) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = bottomPadding + 4.dp),
        )
    }

    TaskEditorSheet(editor)

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.tasks_clear_completed_title)) },
            text = { Text(stringResource(R.string.tasks_clear_completed_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearCompleted()
                    confirmClear = false
                }) { Text(stringResource(R.string.action_delete), color = colors.danger) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

/** Shows snackbars for editor results (shared by Home and Tasks). */
@Composable
fun TaskEditorEvents(editor: TaskEditorViewModel) {
    val snackbar = LocalSnackbarHostState.current
    val context = LocalContext.current
    val resources = LocalResources.current
    LaunchedEffect(editor) {
        editor.events.collect { event ->
            when (event) {
                is TaskEditorEvent.Saved -> snackbar.showSnackbar(
                    resources.getString(if (event.isNew) R.string.task_added else R.string.task_updated),
                    duration = SnackbarDuration.Short,
                )
                is TaskEditorEvent.Deleted -> {
                    val result = snackbar.showSnackbar(
                        message = resources.getString(R.string.task_deleted),
                        actionLabel = resources.getString(R.string.action_undo),
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) editor.undoDelete(event.task)
                }
                TaskEditorEvent.Error -> snackbar.showSnackbar(resources.getString(R.string.error_generic))
            }
        }
    }
}

private fun filterLabel(filter: TaskFilter): Int = when (filter) {
    TaskFilter.ALL -> R.string.tasks_filter_all
    TaskFilter.TODAY -> R.string.tasks_filter_today
    TaskFilter.UPCOMING -> R.string.tasks_filter_upcoming
    TaskFilter.COMPLETED -> R.string.tasks_filter_completed
}

private fun sortLabel(sort: TaskSort): Int = when (sort) {
    TaskSort.TIME -> R.string.tasks_sort_time
    TaskSort.PRIORITY -> R.string.tasks_sort_priority
    TaskSort.CREATED -> R.string.tasks_sort_created
}

private fun sectionTitle(key: TaskSectionKey, context: android.content.Context): String = when (key) {
    TaskSectionKey.Overdue -> context.getString(R.string.tasks_section_overdue)
    TaskSectionKey.Today -> context.getString(R.string.date_today)
    TaskSectionKey.Tomorrow -> context.getString(R.string.date_tomorrow)
    is TaskSectionKey.Day -> Formatting.longDate(key.date)
    TaskSectionKey.NoDate -> context.getString(R.string.tasks_section_no_date)
    TaskSectionKey.Completed -> context.getString(R.string.tasks_filter_completed)
}
