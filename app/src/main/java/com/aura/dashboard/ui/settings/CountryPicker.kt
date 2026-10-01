package com.aura.dashboard.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.aura.dashboard.R
import com.aura.dashboard.data.news.Countries
import com.aura.dashboard.data.news.Country
import com.aura.dashboard.data.news.rss.RssFeeds
import com.aura.dashboard.ui.components.InfoPill
import com.aura.dashboard.ui.components.SearchField
import com.aura.dashboard.ui.theme.AuraTheme

/** Searchable list of countries; reused by onboarding and settings. */
@Composable
fun CountryList(
    selectedCode: String,
    onSelect: (Country) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    var query by rememberSaveable { mutableStateOf("") }
    val countries = remember { Countries.all.sortedWith(compareBy<Country> { it.code != "BG" }.thenBy { it.displayName }) }
    val filtered = remember(query, countries) {
        if (query.isBlank()) countries else countries.filter { it.displayName.contains(query.trim(), ignoreCase = true) || it.code.equals(query.trim(), true) }
    }
    Column(modifier) {
        SearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = stringResource(R.string.country_search_hint),
            clearLabel = stringResource(R.string.action_clear),
            modifier = Modifier.padding(horizontal = contentPadding.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr)),
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(contentPadding = contentPadding, modifier = Modifier.fillMaxHeight()) {
            items(countries.filter { it in filtered }, key = { it.code }) { country ->
                CountryRow(country, selected = country.code == selectedCode, onClick = { onSelect(country) })
            }
        }
    }
}

@Composable
private fun CountryRow(country: Country, selected: Boolean, onClick: () -> Unit) {
    val colors = AuraTheme.colors
    val keyless = RssFeeds.forCountry(country.code).isNotEmpty()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) colors.accent.primary.copy(alpha = 0.10f) else androidx.compose.ui.graphics.Color.Transparent)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(country.flag, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(country.displayName, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
            if (keyless) {
                Spacer(Modifier.height(2.dp))
                InfoPill(text = stringResource(R.string.country_feeds_builtin), color = colors.success)
            }
        }
        if (selected) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = stringResource(R.string.state_selected), tint = colors.accent.primary)
        }
    }
}

@Composable
fun CountryPickerSheet(selectedCode: String, onSelect: (Country) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(Modifier.navigationBarsPadding().fillMaxHeight(0.85f)) {
            Text(
                text = stringResource(R.string.settings_country),
                style = MaterialTheme.typography.titleLarge,
                color = AuraTheme.colors.textPrimary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            CountryList(
                selectedCode = selectedCode,
                onSelect = onSelect,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}
