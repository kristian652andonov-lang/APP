package com.aura.dashboard.ui.components

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect

@Stable
class PermissionState(granted: Boolean, private val onRequest: () -> Unit) {
    var granted by mutableStateOf(granted)
        internal set

    fun request() = onRequest()
}

fun Context.canPostNotifications(): Boolean {
    val runtimeGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    return runtimeGranted && NotificationManagerCompat.from(this).areNotificationsEnabled()
}

fun Context.canScheduleExactAlarms(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true

/**
 * Notification permission (Android 13+) with a graceful path to system settings once the
 * user has permanently declined or turned notifications off for the app.
 */
@Composable
fun rememberNotificationPermissionState(onResult: (Boolean) -> Unit = {}): PermissionState {
    val context = LocalContext.current
    lateinit var state: PermissionState
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        state.granted = context.canPostNotifications()
        val activity = context.findActivity()
        if (!granted && activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
        ) {
            openAppNotificationSettings(context)
        }
        onResult(state.granted)
    }
    state = remember {
        PermissionState(context.canPostNotifications()) {
            val runtimeMissing = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            if (runtimeMissing) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                openAppNotificationSettings(context)
            }
        }
    }
    LifecycleResumeEffect(state) {
        state.granted = context.canPostNotifications()
        onPauseOrDispose { }
    }
    return state
}

/** "Alarms & reminders" special access (Android 12+), needed for minute-exact reminders. */
@Composable
fun rememberExactAlarmPermissionState(): PermissionState {
    val context = LocalContext.current
    val state = remember {
        PermissionState(context.canScheduleExactAlarms()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.startSettingsSafely(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:${context.packageName}".toUri()),
                )
            }
        }
    }
    LifecycleResumeEffect(state) {
        state.granted = context.canScheduleExactAlarms()
        onPauseOrDispose { }
    }
    return state
}

fun openAppNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    context.startSettingsSafely(intent)
}

/** Opens the list of battery-optimization exemptions so users can exclude the app. */
fun openBatteryOptimizationSettings(context: Context) {
    context.startSettingsSafely(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
}

private fun Context.startSettingsSafely(intent: Intent) {
    try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (notFound: ActivityNotFoundException) {
        // Some OEM builds lack specific settings screens; fall back to the app details page.
        runCatching {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
