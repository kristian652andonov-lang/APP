package com.aura.dashboard.data.apps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.aura.dashboard.core.di.IoDispatcher
import com.aura.dashboard.domain.model.InstalledApp
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import java.text.Collator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads launchable apps through the PackageManager. Visibility on Android 11+ comes from the
 * `<queries>` launcher intent declared in the manifest, so QUERY_ALL_PACKAGES is not needed.
 */
@Singleton
class InstalledAppsSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val packageManager: PackageManager = context.packageManager

    suspend fun launchableApps(): List<InstalledApp> = withContext(ioDispatcher) {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val collator = Collator.getInstance()
        runCatching { packageManager.queryIntentActivities(intent, 0) }
            .getOrDefault(emptyList())
            .asSequence()
            .map { it.activityInfo }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .map { InstalledApp(it.packageName, it.loadLabel(packageManager).toString().trim()) }
            .sortedWith { a, b -> collator.compare(a.label, b.label) }
            .toList()
    }

    fun isLaunchable(packageName: String): Boolean =
        runCatching { packageManager.getLaunchIntentForPackage(packageName) != null }.getOrDefault(false)

    fun labelFor(packageName: String): String? = runCatching {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
    }.getOrNull()

    /** Emits whenever an app is installed, removed or updated (and once on collection). */
    fun packageChanges(): Flow<Unit> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(Unit)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        awaitClose { context.unregisterReceiver(receiver) }
    }.onStart { emit(Unit) }
}
