package com.aura.dashboard.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aura.dashboard.ui.components.GlassCard
import com.aura.dashboard.ui.components.IconBubble
import com.aura.dashboard.ui.theme.AuraTheme

@Composable
fun SettingsGroup(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = AuraTheme.colors.textSecondary,
            modifier = Modifier
                .padding(start = 8.dp, bottom = 8.dp)
                .semantics { heading() },
        )
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 6.dp),
            shape = RoundedCornerShape(26.dp),
            elevation = 10.dp,
            content = content,
        )
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    showChevron: Boolean = onClick != null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val colors = AuraTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 60.dp)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBubble(icon = icon, size = 36.dp, tinted = false)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
        }
        trailing()
        if (showChevron) {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
fun SwitchRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    val colors = AuraTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 60.dp)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBubble(icon = icon, size = 36.dp, tinted = false)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = if (enabled) colors.textPrimary else colors.textTertiary)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = colors.accent.primary),
        )
    }
}

@Composable
fun GroupDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 66.dp, end = 16.dp),
        color = AuraTheme.colors.divider,
    )
}
