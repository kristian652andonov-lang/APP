package com.aura.dashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aura.dashboard.R
import com.aura.dashboard.ui.theme.AuraTheme

/** Friendly placeholder for empty lists and error states. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = AuraTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(colors.accent.gradientStart.copy(alpha = 0.35f), colors.accent.gradientEnd.copy(alpha = 0.08f)),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = colors.accent.primary, modifier = Modifier.size(38.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(18.dp))
            SoftButton(text = actionLabel, onClick = onAction)
        }
    }
}

@Composable
fun OfflinePill(modifier: Modifier = Modifier) {
    InfoPill(
        text = stringResource(R.string.offline_indicator),
        icon = Icons.Rounded.CloudOff,
        color = AuraTheme.colors.warning,
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}
