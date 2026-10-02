package com.aura.dashboard.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aura.dashboard.data.apps.AppIconLoader
import com.aura.dashboard.data.apps.InstalledAppsSource
import com.aura.dashboard.data.apps.KnownApps
import com.aura.dashboard.data.local.AuraDatabase
import com.aura.dashboard.domain.model.UserPreferences
import com.aura.dashboard.testing.FakeClock
import com.aura.dashboard.testing.FakePreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class QuickAppRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val database = Room.inMemoryDatabaseBuilder(context, AuraDatabase::class.java).allowMainThreadQueries().build()

    private fun repository(prefs: FakePreferences): DeviceQuickAppRepository {
        val dispatcher = StandardTestDispatcher()
        return DeviceQuickAppRepository(
            database = database,
            installedApps = InstalledAppsSource(context, dispatcher),
            iconLoader = AppIconLoader(context, dispatcher),
            preferences = prefs,
            clock = FakeClock(Instant.parse("2026-10-02T08:00:00Z")),
            ioDispatcher = dispatcher,
        )
    }

    private suspend fun pinned(): List<String> = database.quickAppDao().getAll().sortedBy { it.position }.map { it.packageName }

    @After
    fun tearDown() = database.close()

    @Test
    fun `existing users get Claude pinned first, once`() = runTest {
        val prefs = FakePreferences(UserPreferences(quickAppsSeeded = true, quickAppsSeedVersion = 1))
        val repository = repository(prefs)
        repository.add("com.spotify.music")
        repository.add("com.instagram.android")

        repository.applyDefaultUpdates()
        assertEquals(listOf(KnownApps.CLAUDE_PACKAGE, "com.spotify.music", "com.instagram.android"), pinned())
        assertEquals(QuickAppRepositoryVersion, prefs.preferences.first().quickAppsSeedVersion)

        // Removing it afterwards sticks: the update never runs again.
        repository.remove(KnownApps.CLAUDE_PACKAGE)
        repository.applyDefaultUpdates()
        assertEquals(listOf("com.spotify.music", "com.instagram.android"), pinned())
    }

    @Test
    fun `an already pinned Claude keeps its place`() = runTest {
        val prefs = FakePreferences(UserPreferences(quickAppsSeeded = true, quickAppsSeedVersion = 1))
        val repository = repository(prefs)
        repository.add("com.spotify.music")
        repository.add(KnownApps.CLAUDE_PACKAGE)

        repository.applyDefaultUpdates()
        assertEquals(listOf("com.spotify.music", KnownApps.CLAUDE_PACKAGE), pinned())
    }

    @Test
    fun `fresh installs are left to the first-run seeding`() = runTest {
        val prefs = FakePreferences()
        repository(prefs).applyDefaultUpdates()
        assertEquals(emptyList(), pinned())
        assertEquals(QuickAppRepositoryVersion, prefs.preferences.first().quickAppsSeedVersion)
    }

    private companion object {
        const val QuickAppRepositoryVersion = DeviceQuickAppRepository.CURRENT_SEED_VERSION
    }
}
