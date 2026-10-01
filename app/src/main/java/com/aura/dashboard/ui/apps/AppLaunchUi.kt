package com.aura.dashboard.ui.apps

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aura.dashboard.R

/** Details for the friendly "app not installed" dialog. */
data class NotInstalledInfo(val packageName: String, val label: String, val webUrl: String?)

@Composable
fun NotInstalledDialog(
    info: NotInstalledInfo,
    onOpenStore: () -> Unit,
    onOpenWeb: () -> Unit,
    onDismiss: () -> Unit,
    onRemove: (() -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.apps_not_installed_title, info.label)) },
        text = {
            Text(
                stringResource(
                    if (info.webUrl != null) R.string.apps_not_installed_message_web else R.string.apps_not_installed_message,
                    info.label,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = onOpenStore) { Text(stringResource(R.string.apps_get_app)) }
        },
        dismissButton = {
            when {
                info.webUrl != null -> TextButton(onClick = onOpenWeb) { Text(stringResource(R.string.apps_open_website)) }
                onRemove != null -> TextButton(onClick = onRemove) { Text(stringResource(R.string.apps_remove)) }
                else -> TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
            }
        },
    )
}
