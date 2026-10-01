package com.aura.dashboard

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.aura.dashboard.core.di.ApplicationScope
import com.aura.dashboard.data.preferences.UserPreferencesRepository
import com.aura.dashboard.notifications.AlarmHandler
import com.aura.dashboard.notifications.NotificationChannels
import com.aura.dashboard.work.NewsRefreshScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath
import javax.inject.Inject

@HiltAndroidApp
class AuraApplication : Application(), SingletonImageLoader.Factory {

    @Inject lateinit var okHttpClient: dagger.Lazy<OkHttpClient>
    @Inject lateinit var preferences: UserPreferencesRepository
    @Inject lateinit var alarmHandler: AlarmHandler
    @Inject lateinit var newsRefreshScheduler: NewsRefreshScheduler

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            // Alarms are cleared by force-stop and some OEM task killers; restore them on start.
            runCatching { alarmHandler.rescheduleAll() }
        }
        applicationScope.launch {
            preferences.preferences
                .map { Triple(it.notificationSound, it.notificationVibration, it.newsRefreshInterval) }
                .distinctUntilChanged()
                .collect { (sound, vibration, interval) ->
                    runCatching { NotificationChannels.ensureRemindersChannel(this@AuraApplication, sound, vibration) }
                    newsRefreshScheduler.apply(interval)
                }
        }
        applicationScope.launch {
            // Turning reminders off cancels every alarm; turning them back on restores them.
            preferences.preferences
                .map { it.remindersEnabled }
                .distinctUntilChanged()
                .drop(1)
                .collect { runCatching { alarmHandler.rescheduleAll() } }
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient.get() })) }
            .memoryCache { MemoryCache.Builder().maxSizePercent(context, 0.20).build() }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizeBytes(100L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
}
