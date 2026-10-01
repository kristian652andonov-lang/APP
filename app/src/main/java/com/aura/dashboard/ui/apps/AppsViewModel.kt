package com.aura.dashboard.ui.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.dashboard.data.apps.AppLauncher
import com.aura.dashboard.data.apps.LaunchResult
import com.aura.dashboard.data.repository.QuickAppRepository
import com.aura.dashboard.domain.model.InstalledApp
import com.aura.dashboard.domain.model.QuickApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class AppsUiState(
    val pinned: List<QuickApp> = emptyList(),
    val available: List<InstalledApp> = emptyList(),
    val query: String = "",
    val isLoadingInstalled: Boolean = true,
    val hasPinnedApps: Boolean = false,
) {
    val canReorder: Boolean get() = query.isBlank()
}

sealed interface AppsEvent {
    data class NotInstalled(val info: NotInstalledInfo) : AppsEvent
    data class Added(val label: String) : AppsEvent
    data class Removed(val app: QuickApp) : AppsEvent
    data object LaunchFailed : AppsEvent
}

@HiltViewModel
class AppsViewModel @Inject constructor(
    private val quickApps: QuickAppRepository,
    private val launcher: AppLauncher,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val _events = Channel<AppsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val installed = quickApps.observePackageChanges()
        .map<Unit, List<InstalledApp>?> { quickApps.installedApps() }
        .onStart { emit(null) }

    val uiState: StateFlow<AppsUiState> = combine(quickApps.observeQuickApps(), installed, query) { pinned, installed, query ->
        val needle = query.trim().lowercase(Locale.getDefault())
        val pinnedPackages = pinned.mapTo(HashSet()) { it.packageName }
        AppsUiState(
            pinned = pinned.filter { needle.isEmpty() || it.label.lowercase(Locale.getDefault()).contains(needle) },
            available = installed.orEmpty().filter { app ->
                app.packageName !in pinnedPackages &&
                    (needle.isEmpty() || app.label.lowercase(Locale.getDefault()).contains(needle))
            },
            query = query,
            isLoadingInstalled = installed == null,
            hasPinnedApps = pinned.isNotEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppsUiState())

    fun setQuery(value: String) {
        query.value = value
    }

    fun launch(packageName: String, label: String) {
        viewModelScope.launch {
            when (val result = launcher.launch(packageName)) {
                LaunchResult.Launched -> Unit
                is LaunchResult.NotInstalled -> _events.send(AppsEvent.NotInstalled(NotInstalledInfo(packageName, label, result.webUrl)))
                LaunchResult.Failed -> _events.send(AppsEvent.LaunchFailed)
            }
        }
    }

    fun add(app: InstalledApp) {
        viewModelScope.launch {
            quickApps.add(app.packageName)
            _events.send(AppsEvent.Added(app.label))
        }
    }

    fun remove(app: QuickApp) {
        viewModelScope.launch {
            quickApps.remove(app.packageName)
            _events.send(AppsEvent.Removed(app))
        }
    }

    fun undoRemove(app: QuickApp) {
        viewModelScope.launch {
            quickApps.add(app.packageName)
            if (app.customLabel != null) quickApps.rename(app.packageName, app.customLabel)
            if (!app.showOnHome) quickApps.setShowOnHome(app.packageName, false)
            // Put it back where it was.
            val order = uiState.value.pinned.map { it.packageName }.filter { it != app.packageName }.toMutableList()
            order.add(app.position.coerceIn(0, order.size), app.packageName)
            quickApps.reorder(order)
        }
    }

    fun rename(app: QuickApp, label: String?) {
        viewModelScope.launch { quickApps.rename(app.packageName, label) }
    }

    fun setShowOnHome(app: QuickApp, show: Boolean) {
        viewModelScope.launch { quickApps.setShowOnHome(app.packageName, show) }
    }

    fun reorder(orderedPackages: List<String>) {
        viewModelScope.launch { quickApps.reorder(orderedPackages) }
    }

    fun openStore(packageName: String) = launcher.openStoreListing(packageName)

    fun openWeb(url: String) = launcher.openWeb(url)
}
