package com.aura.dashboard.ui.weather

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.dashboard.R
import com.aura.dashboard.data.news.Countries
import com.aura.dashboard.ui.components.SearchField
import com.aura.dashboard.ui.theme.AuraTheme

/**
 * Choose where the weather is for: search any city, use the device's approximate location
 * (permission asked only now), or go back to the default (your country's capital).
 */
@Composable
fun LocationPickerSheet(viewModel: WeatherViewModel, onDismiss: () -> Unit) {
    val search by viewModel.search.collectAsStateWithLifecycle()
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = AuraTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.useDeviceLocation() else viewModel.onLocationPermissionDenied()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .fillMaxHeight(0.85f),
        ) {
            Text(
                text = stringResource(R.string.weather_location_title),
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            SearchField(
                value = search.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = stringResource(R.string.weather_location_search_hint),
                clearLabel = stringResource(R.string.action_clear),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                if (search.query.isBlank()) {
                    item {
                        LocationRow(
                            icon = Icons.Rounded.MyLocation,
                            title = stringResource(R.string.weather_use_current_location),
                            subtitle = stringResource(R.string.weather_use_current_location_hint),
                            loading = search.isLocating,
                            onClick = { permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
                        )
                    }
                    if (!ui.isDefaultLocation) {
                        item {
                            LocationRow(
                                icon = Icons.Rounded.LocationCity,
                                title = stringResource(R.string.weather_use_default_location),
                                subtitle = stringResource(R.string.weather_use_default_location_hint),
                                onClick = {
                                    viewModel.selectLocation(null)
                                    onDismiss()
                                },
                            )
                        }
                    }
                }
                if (search.isSearching) {
                    item {
                        Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
                            CircularProgressIndicator(color = colors.accent.primary, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                        }
                    }
                } else if (search.failed) {
                    item {
                        Text(
                            stringResource(R.string.weather_error_offline),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                } else if (search.query.trim().length >= 2 && search.results.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.weather_location_no_results),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
                items(search.results, key = { "${it.latitude},${it.longitude}" }) { location ->
                    val flag = location.countryCode?.let { Countries.byCode(it).flag }.orEmpty()
                    LocationRow(
                        icon = Icons.Rounded.Place,
                        title = "$flag ${location.name}".trim(),
                        subtitle = location.region,
                        onClick = {
                            viewModel.selectLocation(location)
                            onDismiss()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    loading: Boolean = false,
) {
    val colors = AuraTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, enabled = !loading, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.layout.Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.accent.primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(color = colors.accent.primary, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            } else {
                Icon(icon, contentDescription = null, tint = colors.accent.primary)
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
        }
    }
}
