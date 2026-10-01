package com.aura.dashboard.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aura.dashboard.ui.theme.AuraTheme

/** Primary call to action: a pill with the accent gradient. */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val colors = AuraTheme.colors
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 52.dp)
            .shadow(if (enabled) 12.dp else 0.dp, shape, ambientColor = colors.accent.primary, spotColor = colors.accent.primary)
            .clip(shape)
            .background(
                if (enabled) {
                    Brush.linearGradient(listOf(colors.accent.gradientStart, colors.accent.primary))
                } else {
                    Brush.linearGradient(listOf(colors.divider, colors.divider))
                },
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val contentColor = if (enabled) Color.White else colors.textTertiary
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = contentColor)
    }
}

/** Secondary action: a soft tinted pill. */
@Composable
fun SoftButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    contentColor: Color = AuraTheme.colors.accent.primary,
) {
    val colors = AuraTheme.colors
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .clip(RoundedCornerShape(50))
            .background(contentColor.copy(alpha = if (colors.isDark) 0.16f else 0.10f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .alpha(if (enabled) 1f else 0.5f),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = contentColor, maxLines = 1)
    }
}

/** Round translucent icon button with a 48dp touch target. */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = AuraTheme.colors.textPrimary,
    highlighted: Boolean = false,
) {
    val colors = AuraTheme.colors
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (highlighted) colors.accent.primary.copy(alpha = 0.14f) else colors.glass)
            .border(1.dp, glassBorderBrush(), CircleShape)
            .clickable(role = Role.Button, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = if (highlighted) colors.accent.primary else tint, modifier = Modifier.size(22.dp))
    }
}

/** Selectable pill used for filters and options. Selection is conveyed by color and semantics. */
@Composable
fun AuraChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    unselectedColor: Color = AuraTheme.colors.glass,
) {
    val colors = AuraTheme.colors
    val background by animateColorAsState(
        if (selected) colors.accent.primary else unselectedColor,
        label = "chipBackground",
    )
    val content by animateColorAsState(
        if (selected) colors.accent.onPrimary else colors.textSecondary,
        label = "chipContent",
    )
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(50))
            .background(background)
            .border(1.dp, if (selected) Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)) else glassBorderBrush(), RoundedCornerShape(50))
            .clickable(onClick = onClick, indication = ripple(), interactionSource = null)
            .semantics {
                this.selected = selected
                role = Role.Tab
            }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(label, style = MaterialTheme.typography.labelLarge, color = content, maxLines = 1)
    }
}

/** Unselected chip color for chips placed on cards and sheets, where white glass would vanish. */
@Composable
fun chipOnCardColor(): Color {
    val colors = AuraTheme.colors
    return colors.accent.primary.copy(alpha = if (colors.isDark) 0.16f else 0.08f)
}

/** Small informational pill (e.g. "Offline", "High priority"). */
@Composable
fun InfoPill(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    color: Color = AuraTheme.colors.textSecondary,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val colors = AuraTheme.colors
    Row(
        modifier = modifier.defaultMinSize(minHeight = 40.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        trailing()
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = colors.accent.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(role = Role.Button, onClick = onAction)
                    .padding(horizontal = 10.dp, vertical = 10.dp),
            )
        }
    }
}

/** Gradient circle containing an icon, used as a decorative leading element. */
@Composable
fun IconBubble(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 44.dp,
    tinted: Boolean = true,
) {
    val colors = AuraTheme.colors
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.36f))
            .background(
                if (tinted) {
                    Brush.linearGradient(listOf(colors.accent.gradientStart, colors.accent.gradientEnd))
                } else {
                    Brush.linearGradient(listOf(colors.accent.container, colors.accent.container))
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (tinted) Color.White else colors.accent.onContainer,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

val ScreenPadding = PaddingValues(horizontal = 20.dp)
