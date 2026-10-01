package com.aura.dashboard.ui.home

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddAlert
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.dashboard.R
import com.aura.dashboard.domain.model.QuickApp
import com.aura.dashboard.domain.model.Repeat
import com.aura.dashboard.ui.apps.NotInstalledDialog
import com.aura.dashboard.ui.apps.NotInstalledInfo
import com.aura.dashboard.ui.components.AppIcon
import com.aura.dashboard.ui.components.Avatar
import com.aura.dashboard.ui.components.EmptyState
import com.aura.dashboard.ui.components.GlassCard
import com.aura.dashboard.ui.components.IconBubble
import com.aura.dashboard.ui.components.InfoPill
import com.aura.dashboard.ui.components.OfflinePill
import com.aura.dashboard.ui.components.ProgressRing
import com.aura.dashboard.ui.components.SectionHeader
import com.aura.dashboard.ui.components.SoftButton
import com.aura.dashboard.ui.navigation.LocalBottomBarPadding
import com.aura.dashboard.ui.navigation.LocalSnackbarHostState
import com.aura.dashboard.ui.news.FeaturedArticleCard
import com.aura.dashboard.ui.news.FeaturedArticleSkeleton
import com.aura.dashboard.ui.news.messageRes
import com.aura.dashboard.ui.news.openArticle
import com.aura.dashboard.ui.news.titleRes
import com.aura.dashboard.ui.tasks.TaskEditorEvents
import com.aura.dashboard.ui.tasks.TaskEditorSheet
import com.aura.dashboard.ui.tasks.TaskEditorViewModel
import com.aura.dashboard.ui.tasks.TaskLine
import com.aura.dashboard.ui.theme.AuraTheme
import com.aura.dashboard.ui.util.Formatting
import kotlinx.coroutines.launch
import java.time.Duration

private const val HOME_TASK_PREVIEW = 5

@Composable
fun HomeScreen(
    onOpenTasks: () -> Unit,
    onOpenReminders: () -> Unit,
    onAddReminder: () -> Unit,
    onOpenNews: () -> Unit,
    onOpenApps: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    editor: TaskEditorViewModel = hiltViewModel(key = "taskEditor"),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val snackbar = LocalSnackbarHostState.current
    val bottomPadding = LocalBottomBarPadding.current
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    var notInstalled by remember { mutableStateOf<NotInstalledInfo?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.AppNotInstalled -> notInstalled = event.info
                HomeEvent.LaunchFailed -> snackbar.showSnackbar(resources.getString(R.string.apps_launch_failed))
            }
        }
    }
    TaskEditorEvents(editor)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = topPadding + 16.dp, bottom = bottomPadding + 8.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item(key = "greeting") {
            Greeting(state = state, onOpenSettings = onOpenSettings, modifier = Modifier.padding(horizontal = 20.dp))
        }
        item(key = "progress") {
            ProgressCard(state = state, onClick = onOpenTasks, modifier = Modifier.padding(horizontal = 20.dp))
        }
        item(key = "reminder") {
            NextReminderCard(
                state = state,
                onOpen = onOpenReminders,
                onAdd = onAddReminder,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
        item(key = "apps") {
            QuickAppsCard(
                apps = state.quickApps,
                onLaunch = viewModel::launchApp,
                onEdit = onOpenApps,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
        item(key = "news") {
            Column {
                SectionHeader(
                    title = stringResource(R.string.home_latest_news),
                    actionLabel = stringResource(R.string.action_see_all),
                    onAction = onOpenNews,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(6.dp))
                HomeNews(
                    state = state,
                    onRetry = viewModel::refreshNews,
                    onNoBrowser = { scope.launch { snackbar.showSnackbar(resources.getString(R.string.error_no_browser)) } },
                )
            }
        }
        item(key = "tasks") {
            TodayTasksCard(
                state = state,
                onToggle = viewModel::toggleTask,
                onOpenTask = { editor.openExisting(it.id) },
                onAdd = { editor.openNew(dueToday = true) },
                onSeeAll = onOpenTasks,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
    }

    TaskEditorSheet(editor)

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
            onDismiss = { notInstalled = null },
        )
    }

}

@Composable
private fun Greeting(state: HomeUiState, onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AuraTheme.colors
    val greeting = stringResource(
        when (state.greeting) {
            Greeting.MORNING -> R.string.greeting_morning
            Greeting.AFTERNOON -> R.string.greeting_afternoon
            Greeting.EVENING -> R.string.greeting_evening
        },
    )
    val title = if (state.userName.isNotBlank()) "$greeting, ${state.userName}" else greeting
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = colors.textPrimary,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = Formatting.longDate(state.today),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textSecondary,
                )
                AnimatedVisibility(visible = !state.isOnline) {
                    OfflinePill(Modifier.padding(start = 10.dp))
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClickLabel = stringResource(R.string.home_open_settings), onClick = onOpenSettings),
            contentAlignment = Alignment.Center,
        ) {
            Avatar(name = state.userName, imagePath = state.avatarPath, size = 48.dp)
        }
    }
}

@Composable
private fun ProgressCard(state: HomeUiState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AuraTheme.colors
    GlassCard(modifier = modifier.fillMaxWidth(), onClick = onClick, onClickLabel = stringResource(R.string.home_open_tasks)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(progress = state.progress, size = 96.dp, strokeWidth = 11.dp) {
                Text(
                    text = "${state.completedCount}/${state.totalCount}",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                )
            }
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.home_todays_progress),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (state.totalCount == 0) {
                        stringResource(R.string.home_no_tasks_short)
                    } else {
                        stringResource(R.string.home_completed_of, state.completedCount, state.totalCount)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(
                        when {
                            state.totalCount == 0 -> R.string.home_motivation_empty
                            state.completedCount == state.totalCount -> R.string.home_motivation_done
                            state.progress >= 0.5f -> R.string.home_motivation_half
                            else -> R.string.home_motivation_start
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun NextReminderCard(state: HomeUiState, onOpen: () -> Unit, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AuraTheme.colors
    val context = LocalContext.current
    val reminder = state.nextReminder
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onOpen,
        onClickLabel = stringResource(R.string.home_open_reminders),
        contentPadding = PaddingValues(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBubble(icon = if (reminder != null) Icons.Rounded.NotificationsActive else Icons.Rounded.AddAlert)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_next_reminder),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary,
                )
                Text(
                    text = reminder?.title ?: stringResource(R.string.home_no_reminders),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (reminder?.nextTriggerAt != null) {
                    val zoned = reminder.nextTriggerAt.atZone(java.time.ZoneId.systemDefault())
                    val dateText = Formatting.relativeDate(context, zoned.toLocalDate(), state.today)
                    val repeatText = if (reminder.repeat != Repeat.NEVER) " · ${Formatting.repeatLabel(context, reminder.repeat)}" else ""
                    Text(
                        text = dateText + repeatText,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                    )
                }
            }
            if (reminder?.nextTriggerAt != null) {
                val zoned = reminder.nextTriggerAt.atZone(java.time.ZoneId.systemDefault())
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Formatting.time(context, zoned.toLocalTime()),
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.accent.primary,
                    )
                    val minutes = Duration.between(state.now, reminder.nextTriggerAt).toMinutes()
                    if (minutes in 0..(24 * 60)) {
                        Text(
                            text = if (minutes < 60) {
                                pluralStringResource(R.plurals.in_minutes, minutes.toInt().coerceAtLeast(1), minutes.toInt().coerceAtLeast(1))
                            } else {
                                pluralStringResource(R.plurals.in_hours, (minutes / 60).toInt(), (minutes / 60).toInt())
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary,
                        )
                    }
                }
            } else {
                SoftButton(text = stringResource(R.string.action_add), icon = Icons.Rounded.Add, onClick = onAdd)
            }
        }
    }
}

@Composable
private fun QuickAppsCard(
    apps: List<QuickApp>,
    onLaunch: (QuickApp) -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 14.dp)) {
        SectionHeader(
            title = stringResource(R.string.home_quick_apps),
            actionLabel = stringResource(R.string.action_edit),
            onAction = onEdit,
            modifier = Modifier.padding(start = 8.dp),
        )
        if (apps.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.home_quick_apps_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AuraTheme.colors.textSecondary,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                SoftButton(text = stringResource(R.string.action_add), icon = Icons.Rounded.Apps, onClick = onEdit)
            }
        } else {
            Spacer(Modifier.height(4.dp))
            apps.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { app ->
                        QuickAppTile(app = app, onClick = { onLaunch(app) }, modifier = Modifier.weight(1f))
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
fun QuickAppTile(app: QuickApp, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AuraTheme.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.apps_open_label, app.label), onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center) {
            AppIcon(packageName = app.packageName, label = app.label, size = 50.dp)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (app.isInstalled) colors.textPrimary else colors.textTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HomeNews(state: HomeUiState, onRetry: () -> Unit, onNoBrowser: () -> Unit) {
    val context = LocalContext.current
    val colors = AuraTheme.colors
    val news = state.news
    when {
        news.articles.isNotEmpty() -> LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(news.articles, key = { it.url }) { article ->
                FeaturedArticleCard(
                    article = article,
                    now = state.now,
                    imageHeight = 150.dp,
                    summaryLines = 0,
                    onClick = {
                        if (!openArticle(context, article.url, colors.background, colors.isDark)) onNoBrowser()
                    },
                    modifier = Modifier.width(290.dp),
                )
            }
        }
        news.isRefreshing -> Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            FeaturedArticleSkeleton(Modifier.width(290.dp), imageHeight = 150.dp)
            FeaturedArticleSkeleton(Modifier.width(290.dp), imageHeight = 150.dp)
        }
        else -> GlassCard(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp).fillMaxWidth()) {
            val error = news.error
            EmptyState(
                icon = Icons.Rounded.WbSunny,
                title = if (error != null) stringResource(error.titleRes()) else stringResource(R.string.news_empty_title),
                message = if (error != null) stringResource(error.messageRes()) else stringResource(R.string.news_empty_message),
                actionLabel = stringResource(R.string.action_retry),
                onAction = onRetry,
            )
        }
    }
    if (news.articles.isNotEmpty() && news.error != null && !news.isRefreshing) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InfoPill(text = stringResource(R.string.news_showing_saved), color = colors.textSecondary)
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Rounded.Refresh,
                contentDescription = stringResource(R.string.action_retry),
                tint = colors.accent.primary,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onRetry)
                    .padding(8.dp),
            )
        }
    }
}

@Composable
private fun TodayTasksCard(
    state: HomeUiState,
    onToggle: (com.aura.dashboard.domain.model.Task, Boolean) -> Unit,
    onOpenTask: (com.aura.dashboard.domain.model.Task) -> Unit,
    onAdd: () -> Unit,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    GlassCard(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 12.dp)) {
        SectionHeader(
            title = stringResource(R.string.home_todays_tasks),
            modifier = Modifier.padding(start = 12.dp),
        ) {
            SoftButton(text = stringResource(R.string.task_add_short), icon = Icons.Rounded.Add, onClick = onAdd)
        }
        if (state.todayTasks.isEmpty()) {
            Text(
                text = stringResource(R.string.home_no_tasks),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            )
        } else {
            state.todayTasks.take(HOME_TASK_PREVIEW).forEach { task ->
                Box(
                    Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(onClickLabel = stringResource(R.string.task_action_edit)) { onOpenTask(task) },
                ) {
                    TaskLine(task = task, today = state.today, now = state.nowTime, onToggle = { onToggle(task, it) }, showMeta = true)
                }
            }
            if (state.todayTasks.size > HOME_TASK_PREVIEW) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(role = Role.Button, onClick = onSeeAll)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = pluralStringResource(R.plurals.home_more_tasks, state.todayTasks.size - HOME_TASK_PREVIEW, state.todayTasks.size - HOME_TASK_PREVIEW),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.accent.primary,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = colors.accent.primary)
                }
            }
        }
    }
}
