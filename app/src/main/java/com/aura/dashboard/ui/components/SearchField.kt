package com.aura.dashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.aura.dashboard.ui.theme.AuraTheme

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    clearLabel: String,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val focusManager = LocalFocusManager.current
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(colors.glass)
            .border(1.dp, glassBorderBrush(), shape)
            .padding(start = 18.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.accent.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = placeholder },
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = colors.textTertiary)
                    inner()
                }
            },
        )
        if (value.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClickLabel = clearLabel) { onValueChange("") },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Close, contentDescription = clearLabel, tint = colors.textSecondary)
            }
        }
    }
}
