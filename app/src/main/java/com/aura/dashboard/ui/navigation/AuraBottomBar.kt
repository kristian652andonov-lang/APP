package com.aura.dashboard.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aura.dashboard.domain.model.AppStyle
import com.aura.dashboard.ui.components.glassBorderBrush
import com.aura.dashboard.ui.theme.AuraTheme
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur

/** Height of the floating bar itself, excluding the system navigation bar inset. */
val BottomBarHeight = 68.dp
val BottomBarVerticalMargin = 10.dp

/** Floating frosted-glass navigation bar. The selected tab gets a soft pill highlight. */
@Composable
fun AuraBottomBar(
    current: TopLevelDestination?,
    onSelect: (TopLevelDestination) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val haptics = LocalHapticFeedback.current
    val shape = RoundedCornerShape(34.dp)
    val surface = if (colors.isDark) Color(0xFF1E1B29) else Color.White
    val tint = when (colors.style) {
        AppStyle.GLASS -> surface.copy(alpha = 0.72f)
        AppStyle.TRANSPARENT -> surface.copy(alpha = if (colors.isDark) 0.35f else 0.45f)
        AppStyle.SOLID -> surface
        AppStyle.PASTEL -> colors.glassStrong
        AppStyle.MINIMAL -> colors.background.copy(alpha = 0.92f)
    }
    Row(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = BottomBarVerticalMargin)
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .height(BottomBarHeight)
            .shadow(
                elevation = 24.dp * colors.elevationScale.coerceAtLeast(0.25f),
                shape = shape,
                ambientColor = colors.shadow.copy(alpha = if (colors.isDark) 0.6f else 0.25f),
                spotColor = colors.shadow.copy(alpha = if (colors.isDark) 0.6f else 0.25f),
            )
            .clip(shape)
            .hazeBlur(
                input = HazeInput.Sources(hazeState),
                style = HazeBlurStyle {
                    blurRadius(28.dp)
                    backgroundColor(colors.background)
                    colorEffects(listOf(HazeColorEffect.tint(tint)))
                    fallbackColorEffect(HazeColorEffect.tint(if (colors.isDark) Color(0xF21E1B29) else Color(0xF2FFFFFF)))
                },
            )
            .then(if (colors.borderWidth > 0f) Modifier.border(colors.borderWidth.dp, glassBorderBrush(), shape) else Modifier)
            .padding(horizontal = 6.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TopLevelDestination.entries.forEach { destination ->
            val selected = destination == current
            val contentColor by animateColorAsState(
                if (selected) colors.accent.primary else colors.textSecondary,
                label = "navContent",
            )
            val pillColor by animateColorAsState(
                if (selected) colors.accent.primary.copy(alpha = if (colors.isDark) 0.20f else 0.12f) else Color.Transparent,
                label = "navPill",
            )
            val pillWidth by animateDpAsState(if (selected) 52.dp else 40.dp, label = "navPillWidth")
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(24.dp))
                    .selectable(
                        selected = selected,
                        role = Role.Tab,
                        interactionSource = null,
                        indication = ripple(bounded = true),
                        onClick = {
                            if (!selected) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            onSelect(destination)
                        },
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .size(width = pillWidth, height = 30.dp)
                        .clip(RoundedCornerShape(50))
                        .background(pillColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(destination.icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = stringResource(destination.label),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
