package com.aura.dashboard.ui.navigation

import androidx.annotation.Keep
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.aura.dashboard.R
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

const val NO_ID = -1L

@Serializable data object OnboardingRoute

@Serializable data object HomeRoute

@Serializable data class TasksRoute(val openTaskId: Long = NO_ID)

@Serializable data object NewsRoute

@Serializable data object AppsRoute

@Serializable data object SettingsRoute

@Serializable data class RemindersRoute(val highlightId: Long = NO_ID, val create: Boolean = false)

@Serializable data class LegalRoute(val document: LegalDocument)

@Keep
enum class LegalDocument { PRIVACY, TERMS, LICENSES }

enum class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    val icon: ImageVector,
    @param:StringRes val label: Int,
) {
    HOME(HomeRoute, HomeRoute::class, Icons.Rounded.Home, R.string.nav_home),
    TASKS(TasksRoute(), TasksRoute::class, Icons.Rounded.CheckCircle, R.string.nav_tasks),
    NEWS(NewsRoute, NewsRoute::class, Icons.Rounded.Newspaper, R.string.nav_news),
    APPS(AppsRoute, AppsRoute::class, Icons.Rounded.Apps, R.string.nav_apps),
    SETTINGS(SettingsRoute, SettingsRoute::class, Icons.Rounded.Settings, R.string.nav_settings),
}
