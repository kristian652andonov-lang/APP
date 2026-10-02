package com.aura.dashboard.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aura.dashboard.domain.model.AppStyle
import com.aura.dashboard.domain.model.BackgroundEffect
import com.aura.dashboard.ui.background.LiveBackground
import com.aura.dashboard.ui.theme.AuraTheme

/**
 * Soft pastel backdrop: a light base with large, blurred-looking radial glows, plus an optional
 * animated [effect] (snow, rain, stars…) drawn between the backdrop and the content.
 */
@Composable
fun AuraBackground(
    modifier: Modifier = Modifier,
    effect: BackgroundEffect = BackgroundEffect.NONE,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = AuraTheme.colors
    val glowAlpha = ((if (colors.isDark) 0.75f else 1f) * colors.glowStrength).coerceAtMost(1f)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .drawBehind {
                val w = size.width
                val h = size.height
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(colors.glowPrimary.copy(alpha = glowAlpha), Color.Transparent),
                        center = Offset(w * 0.05f, h * 0.02f),
                        radius = w * 1.0f,
                    ),
                    radius = w * 1.0f,
                    center = Offset(w * 0.05f, h * 0.02f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(colors.glowSecondary.copy(alpha = glowAlpha), Color.Transparent),
                        center = Offset(w * 1.0f, h * 0.18f),
                        radius = w * 0.85f,
                    ),
                    radius = w * 0.85f,
                    center = Offset(w * 1.0f, h * 0.18f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(colors.glowTertiary.copy(alpha = glowAlpha * 0.8f), Color.Transparent),
                        center = Offset(w * 0.2f, h * 0.85f),
                        radius = w * 0.9f,
                    ),
                    radius = w * 0.9f,
                    center = Offset(w * 0.2f, h * 0.85f),
                )
            },
    ) {
        LiveBackground(effect)
        content()
    }
}

/**
 * Frosted "glass" card: translucent surface, hairline light border and a soft tinted shadow.
 * When [onClick] is set the card gets a ripple and a subtle press-scale.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    shape: Shape = RoundedCornerShape(28.dp),
    contentPadding: PaddingValues = PaddingValues(20.dp),
    elevation: Dp = 16.dp,
    strong: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = AuraTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.98f else 1f,
        animationSpec = spring(stiffness = 600f),
        label = "cardScale",
    )
    val shadowColor = if (colors.isDark) colors.shadow.copy(alpha = 0.5f) else colors.shadow.copy(alpha = 0.16f)
    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(elevation * colors.elevationScale, shape, clip = false, ambientColor = shadowColor, spotColor = shadowColor)
            .clip(shape)
            .background(if (strong) colors.glassStrong else colors.glass)
            .then(if (colors.borderWidth > 0f) Modifier.border(colors.borderWidth.dp, glassBorderBrush(), shape) else Modifier)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = ripple(),
                        role = Role.Button,
                        onClickLabel = onClickLabel,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .padding(contentPadding),
        content = content,
    )
}

@Composable
fun glassBorderBrush(): Brush {
    val colors = AuraTheme.colors
    // Glass catches light on its top edge; the other styles use an even hairline.
    val fade = if (colors.style == AppStyle.GLASS) 0.25f else 0.8f
    return Brush.linearGradient(
        listOf(colors.glassBorder, colors.glassBorder.copy(alpha = colors.glassBorder.alpha * fade)),
    )
}
