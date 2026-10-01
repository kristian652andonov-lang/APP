package com.aura.dashboard.ui.apps

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.dashboard.R
import com.aura.dashboard.domain.model.InstalledApp
import com.aura.dashboard.domain.model.QuickApp
import com.aura.dashboard.ui.components.AppIcon
import com.aura.dashboard.ui.components.EmptyState
import com.aura.dashboard.ui.components.GlassCard
import com.aura.dashboard.ui.components.ScreenHeader
import com.aura.dashboard.ui.components.SearchField
import com.aura.dashboard.ui.components.SectionHeader
import com.aura.dashboard.ui.components.SkeletonBlock
import com.aura.dashboard.ui.navigation.LocalBottomBarPadding
import com.aura.dashboard.ui.navigation.LocalSnackbarHostState
import com.aura.dashboard.ui.theme.AuraTheme
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private const val PINNED_KEY = "pinned:"

@Composable
fun AppsScreen(viewModel: AppsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = AuraTheme.colors
    val context = LocalContext.current
    val resources = LocalResources.current
    val haptics = LocalHapticFeedback.current
    val snackbar = LocalSnackbarHostState.current
    val bottomPadding = LocalBottomBarPadding.current
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    var notInstalled by remember { mutableStateOf<NotInstalledInfo?>(null) }
    var renaming by remember { mutableStateOf<QuickApp?>(null) }

    // Local copy so drag-reordering is smooth; persisted when the drag ends.
    var pinned by remember { mutableStateOf(state.pinned) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(state.pinned) { if (!dragging) pinned = state.pinned }

    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val fromKey = from.key as? String ?: return@rememberReorderableLazyListState
        val toKey = to.key as? String ?: return@rememberReorderableLazyListState
        if (!fromKey.startsWith(PINNED_KEY) || !toKey.startsWith(PINNED_KEY)) return@rememberReorderableLazyListState
        val fromIndex = pinned.indexOfFirst { PINNED_KEY + it.packageName == fromKey }
        val toIndex = pinned.indexOfFirst { PINNED_KEY + it.packageName == toKey }
        if (fromIndex < 0 || toIndex < 0) return@rememberReorderableLazyListState
        pinned = pinned.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
        haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }

    fun move(app: QuickApp, delta: Int) {
        val index = pinned.indexOf(app)
        val target = index + delta
        if (index < 0 || target !in pinned.indices) return
        pinned = pinned.toMutableList().apply { add(target, removeAt(index)) }
        viewModel.reorder(pinned.map { it.packageName })
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is AppsEvent.NotInstalled -> notInstalled = event.info
                is AppsEvent.Added -> snackbar.showSnackbar(resources.getString(R.string.apps_added, event.label))
                is AppsEvent.Removed -> {
                    val result = snackbar.showSnackbar(
                        resources.getString(R.string.apps_removed, event.app.label),
                        actionLabel = resources.getString(R.string.action_undo),
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoRemove(event.app)
                }
                AppsEvent.LaunchFailed -> snackbar.showSnackbar(resources.getString(R.string.apps_launch_failed))
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = topPadding + 16.dp, bottom = bottomPadding + 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "header") {
            ScreenHeader(
                title = stringResource(R.string.apps_title),
                subtitle = stringResource(R.string.apps_subtitle),
            )
        }
        item(key = "search") {
            SearchField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                placeholder = stringResource(R.string.apps_search_hint),
                clearLabel = stringResource(R.string.action_clear),
                modifier = Modifier.padding(vertical = 6.dp),
            )
        }
        item(key = "pinned-header") {
            SectionHeader(
                title = stringResource(R.string.apps_quick_apps_section),
                trailing = {
                    if (state.canReorder && pinned.size > 1) {
                        Text(
                            stringResource(R.string.apps_reorder_hint),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textSecondary,
                        )
                    }
                },
            )
        }
        if (pinned.isEmpty()) {
            item(key = "pinned-empty") {
                GlassCard(Modifier.fillMaxWidth()) {
                    EmptyState(
                        icon = if (state.query.isBlank()) Icons.Rounded.Widgets else Icons.Rounded.SearchOff,
                        title = stringResource(if (state.query.isBlank()) R.string.apps_empty_title else R.string.apps_no_match_title),
                        message = stringResource(if (state.query.isBlank()) R.string.apps_empty_message else R.string.apps_no_match_message),
                    )
                }
            }
        }
        items(pinned, key = { PINNED_KEY + it.packageName }) { app ->
            ReorderableItem(reorderState, key = PINNED_KEY + app.packageName, enabled = state.canReorder) { isDragging ->
                val elevation by animateDpAsState(if (isDragging) 18.dp else 0.dp, label = "dragElevation")
                PinnedAppRow(
                    app = app,
                    canReorder = state.canReorder,
                    dragHandle = Modifier.draggableHandle(
                        onDragStarted = {
                            dragging = true
                            haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                        },
                        onDragStopped = {
                            dragging = false
                            viewModel.reorder(pinned.map { it.packageName })
                        },
                    ),
                    onLaunch = { viewModel.launch(app.packageName, app.label) },
                    onToggleHome = { viewModel.setShowOnHome(app, !app.showOnHome) },
                    onRename = { renaming = app },
                    onRemove = { viewModel.remove(app) },
                    onMoveUp = { move(app, -1) },
                    onMoveDown = { move(app, 1) },
                    modifier = Modifier.shadow(elevation, RoundedCornerShape(22.dp)),
                )
            }
        }

        item(key = "all-header") {
            SectionHeader(
                title = stringResource(R.string.apps_all_section),
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        if (state.isLoadingInstalled) {
            items(6, key = { "loading-$it" }) {
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    SkeletonBlock(Modifier.size(44.dp), RoundedCornerShape(14.dp))
                    Spacer(Modifier.width(14.dp))
                    SkeletonBlock(Modifier.width(140.dp).height(14.dp))
                }
            }
        } else if (state.available.isEmpty()) {
            item(key = "all-empty") {
                Text(
                    text = stringResource(if (state.query.isBlank()) R.string.apps_all_pinned else R.string.apps_no_match_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                )
            }
        } else {
            items(state.available, key = { "app:" + it.packageName }) { app ->
                InstalledAppRow(
                    app = app,
                    onLaunch = { viewModel.launch(app.packageName, app.label) },
                    onAdd = { viewModel.add(app) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }

    notInstalled?.let { info ->
        NotInstalledDialog(
            info = info,
            onOpenStore = {
                viewModel.openStore(info.packageName)
                notInstalled = null
            },
            onOpenWeb = {
                info.webUrl?.let(viewModel::openWeb)
                notInstalled = null
            },
            onRemove = {
                state.pinned.firstOrNull { it.packageName == info.packageName }?.let(viewModel::remove)
                notInstalled = null
            },
            onDismiss = { notInstalled = null },
        )
    }

    renaming?.let { app ->
        RenameDialog(
            app = app,
            onConfirm = { label ->
                viewModel.rename(app, label)
                renaming = null
            },
            onDismiss = { renaming = null },
        )
    }
}

@Composable
private fun PinnedAppRow(
    app: QuickApp,
    canReorder: Boolean,
    modifier: Modifier = Modifier,
    dragHandle: Modifier,
    onLaunch: () -> Unit,
    onToggleHome: () -> Unit,
    onRename: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    val colors = AuraTheme.colors
    var menuOpen by remember { mutableStateOf(false) }
    val moveUpLabel = stringResource(R.string.apps_move_up)
    val moveDownLabel = stringResource(R.string.apps_move_down)
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(moveUpLabel) { onMoveUp(); true },
                    CustomAccessibilityAction(moveDownLabel) { onMoveDown(); true },
                )
            },
        onClick = onLaunch,
        onClickLabel = stringResource(R.string.apps_open_label, app.label),
        shape = RoundedCornerShape(22.dp),
        contentPadding = PaddingValues(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        elevation = 8.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (canReorder) {
                Box(
                    modifier = dragHandle.size(40.dp, 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.DragIndicator, contentDescription = stringResource(R.string.apps_drag_handle), tint = colors.textTertiary)
                }
            } else {
                Spacer(Modifier.width(12.dp))
            }
            AppIcon(packageName = app.packageName, label = app.label, size = 44.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = when {
                        !app.isInstalled -> stringResource(R.string.apps_not_installed_short)
                        app.showOnHome -> stringResource(R.string.apps_on_home)
                        else -> stringResource(R.string.apps_hidden_from_home)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (!app.isInstalled) colors.warning else colors.textSecondary,
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(
                        role = Role.Switch,
                        onClickLabel = stringResource(if (app.showOnHome) R.string.apps_hide_from_home else R.string.apps_show_on_home),
                        onClick = onToggleHome,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (app.showOnHome) Icons.Rounded.Home else Icons.Outlined.Home,
                    contentDescription = stringResource(if (app.showOnHome) R.string.apps_hide_from_home else R.string.apps_show_on_home),
                    tint = if (app.showOnHome) colors.accent.primary else colors.textTertiary,
                )
            }
            Box {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = { menuOpen = true }),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.action_more), tint = colors.textSecondary)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.apps_rename)) }, onClick = { menuOpen = false; onRename() })
                    DropdownMenuItem(text = { Text(moveUpLabel) }, onClick = { menuOpen = false; onMoveUp() })
                    DropdownMenuItem(text = { Text(moveDownLabel) }, onClick = { menuOpen = false; onMoveDown() })
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.apps_remove), color = colors.danger) },
                        onClick = { menuOpen = false; onRemove() },
                    )
                }
            }
        }
    }
}

@Composable
private fun InstalledAppRow(app: InstalledApp, onLaunch: () -> Unit, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AuraTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.apps_open_label, app.label), onClick = onLaunch)
            .padding(start = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(packageName = app.packageName, label = app.label, size = 44.dp)
        Spacer(Modifier.width(14.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onAdd),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .then(Modifier),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.apps_add_label, app.label),
                    tint = colors.accent.primary,
                )
            }
        }
    }
}

@Composable
private fun RenameDialog(app: QuickApp, onConfirm: (String?) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(app.customLabel ?: app.defaultLabel) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.apps_rename_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(40) },
                singleLine = true,
                label = { Text(stringResource(R.string.apps_rename_label)) },
                shape = RoundedCornerShape(16.dp),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.takeIf { it.isNotBlank() && it != app.defaultLabel }) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = { onConfirm(null) }) { Text(stringResource(R.string.apps_rename_reset)) }
        },
    )
}
