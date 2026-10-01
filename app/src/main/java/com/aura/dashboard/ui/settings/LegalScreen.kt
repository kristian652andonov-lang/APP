package com.aura.dashboard.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aura.dashboard.R
import com.aura.dashboard.ui.components.GlassCard
import com.aura.dashboard.ui.components.GlassIconButton
import com.aura.dashboard.ui.components.ScreenHeader
import com.aura.dashboard.ui.navigation.LegalDocument
import com.aura.dashboard.ui.theme.AuraTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class OpenSourceComponent(val name: String, val author: String, val license: License)

private enum class License(val title: String, val asset: String) {
    APACHE_2("Apache License 2.0", "licenses/APACHE-2.0.txt"),
    OFL("SIL Open Font License 1.1", "licenses/OFL-Manrope.txt"),
}

private val components = listOf(
    OpenSourceComponent("Kotlin, kotlinx.coroutines, kotlinx.serialization", "JetBrains s.r.o.", License.APACHE_2),
    OpenSourceComponent("AndroidX & Jetpack Compose", "The Android Open Source Project", License.APACHE_2),
    OpenSourceComponent("Material Components for Compose (Material 3)", "The Android Open Source Project", License.APACHE_2),
    OpenSourceComponent("Room, DataStore, WorkManager, Navigation", "The Android Open Source Project", License.APACHE_2),
    OpenSourceComponent("Dagger & Hilt", "The Dagger Authors", License.APACHE_2),
    OpenSourceComponent("Retrofit", "Square, Inc.", License.APACHE_2),
    OpenSourceComponent("OkHttp & Okio", "Square, Inc.", License.APACHE_2),
    OpenSourceComponent("Coil", "Coil Contributors", License.APACHE_2),
    OpenSourceComponent("Haze", "Chris Banes", License.APACHE_2),
    OpenSourceComponent("Reorderable", "Calvin Liang", License.APACHE_2),
    OpenSourceComponent("Manrope typeface", "The Manrope Project Authors", License.OFL),
)

@Composable
fun LegalScreen(document: LegalDocument, onBack: () -> Unit) {
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val title = stringResource(
        when (document) {
            LegalDocument.PRIVACY -> R.string.settings_privacy
            LegalDocument.TERMS -> R.string.settings_terms
            LegalDocument.LICENSES -> R.string.settings_licenses
        },
    )
    var openLicense by rememberSaveable { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = topPadding + 12.dp, bottom = bottomPadding + 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back), onBack)
                Spacer(Modifier.width(12.dp))
                ScreenHeader(title = title, modifier = Modifier.weight(1f))
            }
        }
        when (document) {
            LegalDocument.PRIVACY -> item { MarkdownLiteCard(stringResource(R.string.legal_privacy_body)) }
            LegalDocument.TERMS -> item { MarkdownLiteCard(stringResource(R.string.legal_terms_body)) }
            LegalDocument.LICENSES -> {
                item {
                    Text(
                        stringResource(R.string.legal_licenses_intro),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AuraTheme.colors.textSecondary,
                    )
                }
                items(components) { component ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { openLicense = component.license.name },
                        shape = RoundedCornerShape(22.dp),
                        contentPadding = PaddingValues(16.dp),
                        elevation = 8.dp,
                    ) {
                        Text(component.name, style = MaterialTheme.typography.titleSmall, color = AuraTheme.colors.textPrimary)
                        Text(component.author, style = MaterialTheme.typography.bodySmall, color = AuraTheme.colors.textSecondary)
                        Text(component.license.title, style = MaterialTheme.typography.labelMedium, color = AuraTheme.colors.accent.primary)
                    }
                }
            }
        }
    }

    openLicense?.let { name ->
        val license = License.valueOf(name)
        LicenseDialog(license, onDismiss = { openLicense = null })
    }
}

/** Renders paragraphs; lines starting with "## " become headings. */
@Composable
private fun MarkdownLiteCard(text: String) {
    val colors = AuraTheme.colors
    GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp)) {
        text.split("\n\n").forEachIndexed { index, block ->
            val trimmed = block.trim()
            if (trimmed.startsWith("## ")) {
                Text(
                    text = trimmed.removePrefix("## "),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    modifier = Modifier
                        .padding(top = if (index == 0) 0.dp else 16.dp, bottom = 6.dp)
                        .semantics { heading() },
                )
            } else if (trimmed.isNotEmpty()) {
                Text(
                    text = trimmed,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun LicenseDialog(license: License, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val text by produceState(initialValue = "", license) {
        value = withContext(Dispatchers.IO) {
            runCatching { context.assets.open(license.asset).bufferedReader().use { it.readText() } }.getOrDefault("")
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(license.title) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                Text(text, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } },
    )
}
