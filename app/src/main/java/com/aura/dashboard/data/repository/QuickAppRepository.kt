package com.aura.dashboard.data.repository

import com.aura.dashboard.core.di.IoDispatcher
import com.aura.dashboard.core.time.AppClock
import com.aura.dashboard.data.apps.AppIconLoader
import com.aura.dashboard.data.apps.InstalledAppsSource
import com.aura.dashboard.data.apps.KnownApps
import com.aura.dashboard.data.local.AuraDatabase
import com.aura.dashboard.data.local.entity.QuickAppEntity
import com.aura.dashboard.data.preferences.UserPreferencesRepository
import com.aura.dashboard.domain.model.InstalledApp
import com.aura.dashboard.domain.model.QuickApp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

interface QuickAppRepository {
    /** Pinned apps in user order, refreshed when apps are installed or removed. */
    fun observeQuickApps(): Flow<List<QuickApp>>
    fun observePackageChanges(): Flow<Unit>
    suspend fun installedApps(): List<InstalledApp>
    suspend fun add(packageName: String)
    suspend fun remove(packageName: String)
    suspend fun rename(packageName: String, customLabel: String?)
    suspend fun setShowOnHome(packageName: String, show: Boolean)
    suspend fun reorder(orderedPackageNames: List<String>)

    /** First run: pins popular apps that are actually installed. */
    suspend fun seedDefaultsIfNeeded()

    /** One-time additions for existing users (e.g. pinning Claude in version 2). */
    suspend fun applyDefaultUpdates()
}

@Singleton
class DeviceQuickAppRepository @Inject constructor(
    database: AuraDatabase,
    private val installedApps: InstalledAppsSource,
    private val iconLoader: AppIconLoader,
    private val preferences: UserPreferencesRepository,
    private val clock: AppClock,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : QuickAppRepository {

    private val dao = database.quickAppDao()

    override fun observeQuickApps(): Flow<List<QuickApp>> =
        combine(dao.observeAll(), observePackageChanges()) { entities, _ ->
            entities.map { entity ->
                val installedLabel = installedApps.labelFor(entity.packageName)
                val defaultLabel = installedLabel
                    ?: KnownApps.forPackage(entity.packageName)?.name
                    ?: entity.packageName.substringAfterLast('.')
                QuickApp(
                    packageName = entity.packageName,
                    label = entity.customLabel?.takeIf { it.isNotBlank() } ?: defaultLabel,
                    defaultLabel = defaultLabel,
                    customLabel = entity.customLabel,
                    isInstalled = installedApps.isLaunchable(entity.packageName),
                    showOnHome = entity.showOnHome,
                    position = entity.position,
                )
            }
        }.flowOn(ioDispatcher)

    override fun observePackageChanges(): Flow<Unit> =
        installedApps.packageChanges().onEach { iconLoader.invalidate() }

    override suspend fun installedApps(): List<InstalledApp> = installedApps.launchableApps()

    override suspend fun add(packageName: String) {
        dao.insert(
            QuickAppEntity(
                packageName = packageName,
                customLabel = null,
                position = dao.maxPosition() + 1,
                showOnHome = true,
                addedAt = clock.now().toEpochMilli(),
            ),
        )
    }

    override suspend fun remove(packageName: String) = dao.delete(packageName)

    override suspend fun rename(packageName: String, customLabel: String?) =
        dao.setCustomLabel(packageName, customLabel?.trim()?.takeIf { it.isNotEmpty() })

    override suspend fun setShowOnHome(packageName: String, show: Boolean) = dao.setShowOnHome(packageName, show)

    override suspend fun reorder(orderedPackageNames: List<String>) = dao.reorder(orderedPackageNames)

    override suspend fun seedDefaultsIfNeeded() {
        if (preferences.current().quickAppsSeeded) return
        if (dao.getAll().isEmpty()) {
            KnownApps.all
                .mapNotNull { known -> known.packageNames.firstOrNull(installedApps::isLaunchable) }
                .take(DEFAULT_SEED_COUNT)
                .forEach { add(it) }
        }
        preferences.setQuickAppsSeeded()
        preferences.setQuickAppsSeedVersion(CURRENT_SEED_VERSION)
    }

    override suspend fun applyDefaultUpdates() {
        val prefs = preferences.current()
        if (prefs.quickAppsSeedVersion >= CURRENT_SEED_VERSION) return
        if (prefs.quickAppsSeeded && prefs.quickAppsSeedVersion < 2) {
            // Version 2 adds Claude, pinned first so it shows on the Home screen.
            val existing = dao.getAll()
            if (existing.none { it.packageName == KnownApps.CLAUDE_PACKAGE }) {
                add(KnownApps.CLAUDE_PACKAGE)
                dao.reorder(listOf(KnownApps.CLAUDE_PACKAGE) + existing.map { it.packageName })
            }
        }
        preferences.setQuickAppsSeedVersion(CURRENT_SEED_VERSION)
    }

    companion object {
        private const val DEFAULT_SEED_COUNT = 8
        const val CURRENT_SEED_VERSION = 2
    }
}
