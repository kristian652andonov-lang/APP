package com.aura.dashboard.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aura.dashboard.ui.theme.AuraTheme

/**
 * Round checkbox with a springy fill and a check mark that draws itself in.
 * Touch target is at least 48dp; [label] is announced by screen readers.
 */
@Composable
fun AuraCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 26.dp,
    color: Color = AuraTheme.colors.accent.primary,
) {
    val haptics = LocalHapticFeedback.current
    val colors = AuraTheme.colors
    val fill by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "checkFill",
    )
    val check by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = if (checked) 260 else 120, delayMillis = if (checked) 80 else 0),
        label = "checkDraw",
    )
    val ring by animateColorAsState(
        targetValue = if (checked) color else colors.textTertiary.copy(alpha = 0.7f),
        label = "checkRing",
    )
    val path = remember { Path() }
    val segment = remember { Path() }
    val measure = remember { PathMeasure() }

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .toggleable(
                value = checked,
                interactionSource = null,
                indication = ripple(bounded = false, radius = 20.dp),
                role = Role.Checkbox,
                onValueChange = { newValue ->
                    haptics.performHapticFeedback(
                        if (newValue) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff,
                    )
                    onCheckedChange(newValue)
                },
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    val pop = 1f + 0.08f * (fill * (1f - fill)) * 4f
                    scaleX = pop
                    scaleY = pop
                },
        ) {
            val strokeWidth = 2.dp.toPx()
            val radius = this.size.minDimension / 2f
            drawCircle(color = ring, radius = radius - strokeWidth / 2f, style = Stroke(strokeWidth))
            if (fill > 0f) drawCircle(color = color, radius = radius * fill.coerceIn(0f, 1.05f))
            if (check > 0f) {
                val w = this.size.width
                path.reset()
                path.moveTo(w * 0.28f, w * 0.52f)
                path.lineTo(w * 0.44f, w * 0.67f)
                path.lineTo(w * 0.73f, w * 0.36f)
                measure.setPath(path, false)
                segment.reset()
                measure.getSegment(0f, measure.length * check, segment, true)
                drawPath(
                    path = segment,
                    color = Color.White,
                    style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }
    }
}
