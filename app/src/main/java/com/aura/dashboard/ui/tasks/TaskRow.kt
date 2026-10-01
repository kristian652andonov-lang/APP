package com.aura.dashboard.ui.tasks

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aura.dashboard.R
import com.aura.dashboard.domain.model.Priority
import com.aura.dashboard.domain.model.Repeat
import com.aura.dashboard.domain.model.Task
import com.aura.dashboard.domain.schedule.TaskQueries
import com.aura.dashboard.ui.components.AuraCheckbox
import com.aura.dashboard.ui.components.GlassCard
import com.aura.dashboard.ui.components.InfoPill
import com.aura.dashboard.ui.theme.AuraTheme
import com.aura.dashboard.ui.util.Formatting
import java.time.LocalDate
import java.time.LocalTime

/** A task as a rounded card. Completion is shown by check, strikethrough and fade, not color alone. */
@Composable
fun TaskCard(
    task: Task,
    today: LocalDate,
    now: LocalTime,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showMeta: Boolean = true,
) {
    val completedState = stringResource(if (task.isCompleted) R.string.task_state_completed else R.string.task_state_open)
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics { stateDescription = completedState },
        onClick = onClick,
        onClickLabel = stringResource(R.string.task_action_edit),
        shape = RoundedCornerShape(22.dp),
        contentPadding = PaddingValues(start = 6.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
        elevation = 10.dp,
    ) {
        TaskLine(task, today, now, onToggle, showMeta)
    }
}

/** The checkbox + title + metadata line, reusable inside other cards (e.g. on Home). */
@Composable
fun TaskLine(
    task: Task,
    today: LocalDate,
    now: LocalTime,
    onToggle: (Boolean) -> Unit,
    showMeta: Boolean = true,
) {
    val colors = AuraTheme.colors
    val context = LocalContext.current
    val contentAlpha by animateFloatAsState(if (task.isCompleted) 0.55f else 1f, label = "taskAlpha")
    val titleColor by animateColorAsState(
        if (task.isCompleted) colors.textSecondary else colors.textPrimary,
        label = "taskTitleColor",
    )
    val overdue = TaskQueries.isOverdue(task, today, now)

    Row(verticalAlignment = Alignment.CenterVertically) {
        AuraCheckbox(
            checked = task.isCompleted,
            onCheckedChange = onToggle,
            label = task.title,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp)
                .alpha(contentAlpha),
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = titleColor,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val hasMeta = task.dueDate != null || task.priority != Priority.NORMAL ||
                task.repeat != Repeat.NEVER || task.notes.isNotBlank()
            if (showMeta && hasMeta) {
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    task.dueDate?.let { date ->
                        val dateText = Formatting.relativeDate(context, date, today)
                        val text = task.dueTime?.let { "$dateText · ${Formatting.time(context, it)}" } ?: dateText
                        if (overdue) {
                            InfoPill(
                                text = stringResource(R.string.task_overdue_format, text),
                                icon = Icons.Rounded.WarningAmber,
                                color = colors.danger,
                            )
                        } else {
                            InfoPill(text = text, icon = Icons.Rounded.CalendarToday, color = colors.textSecondary)
                        }
                    }
                    if (task.repeat != Repeat.NEVER) {
                        InfoPill(
                            text = Formatting.repeatLabel(context, task.repeat),
                            icon = Icons.Rounded.Repeat,
                            color = colors.accent.primary,
                        )
                    }
                    when (task.priority) {
                        Priority.HIGH -> InfoPill(
                            text = stringResource(R.string.priority_high),
                            icon = Icons.Rounded.Flag,
                            color = colors.danger,
                        )
                        Priority.LOW -> InfoPill(
                            text = stringResource(R.string.priority_low),
                            color = colors.textSecondary,
                        )
                        Priority.NORMAL -> Unit
                    }
                    if (task.notes.isNotBlank()) {
                        InfoPill(
                            text = stringResource(R.string.task_has_notes),
                            icon = Icons.AutoMirrored.Rounded.Notes,
                            color = colors.textSecondary,
                        )
                    }
                }
            }
        }
    }
}
