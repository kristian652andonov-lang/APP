package com.aura.dashboard.data.apps

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

sealed interface LaunchResult {
    data object Launched : LaunchResult

    /** The app isn't installed (anymore). [webUrl] is a usable web alternative, if any. */
    data class NotInstalled(val packageName: String, val webUrl: String?) : LaunchResult
    data object Failed : LaunchResult
}

/** Launches real installed apps via their launcher intent, with web/Play Store fallbacks. */
@Singleton
class AppLauncher @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun launch(packageName: String): LaunchResult {
        val intent = runCatching { context.packageManager.getLaunchIntentForPackage(packageName) }.getOrNull()
            ?: return LaunchResult.NotInstalled(packageName, KnownApps.forPackage(packageName)?.webUrl)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return try {
            context.startActivity(intent)
            LaunchResult.Launched
        } catch (notFound: ActivityNotFoundException) {
            LaunchResult.NotInstalled(packageName, KnownApps.forPackage(packageName)?.webUrl)
        } catch (security: SecurityException) {
            LaunchResult.Failed
        }
    }

    /** Opens the store listing, preferring the Play Store app and falling back to the website. */
    fun openStoreListing(packageName: String): Boolean =
        startView("market://details?id=$packageName") ||
            startView("https://play.google.com/store/apps/details?id=$packageName")

    fun openWeb(url: String): Boolean = startView(url)

    private fun startView(uri: String): Boolean = try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (notFound: ActivityNotFoundException) {
        false
    }
}
