package com.aura.dashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.aura.dashboard.ui.theme.AuraTheme

/** Gradient floating action button. */
@Composable
fun AuraFab(contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AuraTheme.colors
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = modifier
            .size(60.dp)
            .shadow(16.dp, shape, ambientColor = colors.accent.primary, spotColor = colors.accent.primary)
            .clip(shape)
            .background(Brush.linearGradient(listOf(colors.accent.gradientStart, colors.accent.primary)))
            .clickable(role = Role.Button, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Add, contentDescription = contentDescription, tint = Color.White, modifier = Modifier.size(28.dp))
    }
}
