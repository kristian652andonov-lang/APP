package com.aura.dashboard.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aura.dashboard.R
import com.aura.dashboard.domain.model.AppStyle
import com.aura.dashboard.domain.model.BackgroundEffect
import com.aura.dashboard.ui.background.LiveBackground
import com.aura.dashboard.ui.theme.AuraTheme
import com.aura.dashboard.ui.theme.auraColors

@StringRes
fun AppStyle.labelRes(): Int = when (this) {
    AppStyle.GLASS -> R.string.style_glass
    AppStyle.TRANSPARENT -> R.string.style_transparent
    AppStyle.SOLID -> R.string.style_solid
    AppStyle.PASTEL -> R.string.style_pastel
    AppStyle.MINIMAL -> R.string.style_minimal
}

@StringRes
fun BackgroundEffect.labelRes(): Int = when (this) {
    BackgroundEffect.NONE -> R.string.bg_none
    BackgroundEffect.AUTO -> R.string.bg_auto
    BackgroundEffect.SUNNY -> R.string.bg_sunny
    BackgroundEffect.CLOUDS -> R.string.bg_clouds
    BackgroundEffect.RAIN -> R.string.bg_rain
    BackgroundEffect.THUNDER -> R.string.bg_thunder
    BackgroundEffect.SNOW -> R.string.bg_snow
    BackgroundEffect.NIGHT_SKY -> R.string.bg_night_sky
    BackgroundEffect.SHOOTING_STARS -> R.string.bg_shooting_stars
    BackgroundEffect.FLOWING_LINES -> R.string.bg_flowing_lines
    BackgroundEffect.AURORA -> R.string.bg_aurora
    BackgroundEffect.BUBBLES -> R.string.bg_bubbles
    BackgroundEffect.FIREFLIES -> R.string.bg_fireflies
    BackgroundEffect.PETALS -> R.string.bg_petals
}

/** Horizontal row of miniature screens, each rendered in one appearance style. */
@Composable
fun StylePicker(selected: AppStyle, onSelect: (AppStyle) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
        items(AppStyle.entries) { style -> StylePreview(style, selected == style) { onSelect(style) } }
    }
}

@Composable
private fun StylePreview(style: AppStyle, selected: Boolean, onClick: () -> Unit) {
    val current = AuraTheme.colors
    val preview = auraColors(current.isDark, current.accent, style)
    val label = stringResource(style.labelRes())
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 76.dp, height = 104.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(listOf(preview.glowPrimary, preview.background, preview.glowSecondary)))
                .border(if (selected) 2.dp else 1.dp, if (selected) preview.accent.primary else preview.divider, RoundedCornerShape(18.dp))
                .padding(8.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(2) { index ->
                    val shape = RoundedCornerShape(10.dp)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(if (index == 0) 38.dp else 28.dp)
                            .shadow(6.dp * preview.elevationScale, shape)
                            .clip(shape)
                            .background(preview.glass)
                            .then(if (preview.borderWidth > 0f) Modifier.border(preview.borderWidth.dp, preview.glassBorder, shape) else Modifier)
                            .padding(6.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(if (index == 0) 3 else 2) {
                                Box(
                                    Modifier
                                        .size(10.dp)
                                        .clip(if (style == AppStyle.MINIMAL) CircleShape else RoundedCornerShape(3.dp))
                                        .background(if (style == AppStyle.SOLID) preview.accent.primary else preview.accent.primary.copy(alpha = 0.5f)),
                                )
                            }
                        }
                    }
                }
            }
            if (selected) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = preview.accent.primary,
                    modifier = Modifier.align(Alignment.BottomEnd).size(18.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = if (selected) current.accent.primary else current.textSecondary)
    }
}

/** Grid of live previews for every animated background. */
@Composable
fun BackgroundPickerSheet(
    selected: BackgroundEffect,
    autoPreview: BackgroundEffect,
    onSelect: (BackgroundEffect) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = AuraTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(Modifier.navigationBarsPadding().fillMaxHeight(0.88f)) {
            Text(
                stringResource(R.string.settings_background),
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            )
            Text(
                stringResource(R.string.settings_background_hint),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(12.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(BackgroundEffect.entries) { effect ->
                    BackgroundTile(
                        effect = effect,
                        previewEffect = if (effect == BackgroundEffect.AUTO) autoPreview else effect,
                        selected = effect == selected,
                        onClick = { onSelect(effect) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BackgroundTile(effect: BackgroundEffect, previewEffect: BackgroundEffect, selected: Boolean, onClick: () -> Unit) {
    val colors = AuraTheme.colors
    val shape = RoundedCornerShape(20.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(2.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(shape)
                .background(Brush.linearGradient(listOf(colors.glowPrimary, colors.background, colors.glowSecondary)))
                .border(if (selected) 2.5.dp else 1.dp, if (selected) colors.accent.primary else colors.divider, shape),
        ) {
            CompositionLocalProvider {
                LiveBackground(previewEffect, Modifier.fillMaxSize())
            }
            if (selected) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = colors.accent.primary,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(20.dp).background(Color.White, CircleShape),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(effect.labelRes()),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) colors.accent.primary else colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(110.dp),
        )
    }
}
