package com.aura.dashboard.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.tappableElement
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.aura.dashboard.domain.model.BackgroundEffect
import com.aura.dashboard.domain.model.UserPreferences
import com.aura.dashboard.notifications.DeepLink
import com.aura.dashboard.ui.apps.AppsScreen
import com.aura.dashboard.ui.components.AuraBackground
import com.aura.dashboard.ui.home.HomeScreen
import com.aura.dashboard.ui.news.NewsScreen
import com.aura.dashboard.ui.onboarding.OnboardingScreen
import com.aura.dashboard.ui.reminders.RemindersScreen
import com.aura.dashboard.ui.settings.LegalScreen
import com.aura.dashboard.ui.settings.SettingsScreen
import com.aura.dashboard.ui.tasks.TasksScreen
import com.aura.dashboard.ui.theme.AuraTheme
import com.aura.dashboard.ui.weather.WeatherScreen
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> { error("SnackbarHostState not provided") }

/** Space screens must leave at the bottom so content can scroll above the floating bar. */
val LocalBottomBarPadding = compositionLocalOf { 0.dp }

@Composable
fun AuraApp(
    preferences: UserPreferences,
    backgroundEffect: BackgroundEffect,
    deepLink: DeepLink?,
    onDeepLinkConsumed: () -> Unit,
) {
    val navController = rememberNavController()
    val hazeState = rememberHazeState()
    val snackbarHostState = remember { SnackbarHostState() }
    // Fixed for the lifetime of this NavHost; changing it later would reset the graph.
    val startDestination: Any = remember { if (preferences.onboardingCompleted) HomeRoute else OnboardingRoute }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTopLevel = TopLevelDestination.entries.firstOrNull { destination ->
        backStackEntry?.destination?.hierarchy?.any { it.hasRoute(destination.routeClass) } == true
    }
    val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomBarPadding: Dp = BottomBarHeight + BottomBarVerticalMargin * 2 + navBarInset + 8.dp

    LaunchedEffect(deepLink, preferences.onboardingCompleted) {
        if (deepLink == null || !preferences.onboardingCompleted) return@LaunchedEffect
        when (deepLink) {
            is DeepLink.Reminder -> navController.navigate(RemindersRoute(highlightId = deepLink.id)) {
                popUpTo<HomeRoute>()
            }
            is DeepLink.Task -> navController.navigate(TasksRoute(openTaskId = deepLink.id)) {
                popUpTo<HomeRoute>()
            }
        }
        onDeepLinkConsumed()
    }

    CompositionLocalProvider(
        LocalSnackbarHostState provides snackbarHostState,
        LocalBottomBarPadding provides bottomBarPadding,
    ) {
        AuraBackground(effect = backgroundEffect) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier
                    .fillMaxSize()
                    // Keep content clear of side navigation bars and cutouts in landscape.
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                    .hazeSource(hazeState),
                enterTransition = { fadeIn(tween(260)) + scaleIn(initialScale = 0.985f, animationSpec = tween(260)) },
                exitTransition = { fadeOut(tween(180)) },
                popEnterTransition = { fadeIn(tween(260)) },
                popExitTransition = { fadeOut(tween(180)) },
            ) {
                composable<OnboardingRoute> {
                    OnboardingScreen(
                        onFinished = {
                            navController.navigate(HomeRoute) { popUpTo<OnboardingRoute> { inclusive = true } }
                        },
                    )
                }
                composable<HomeRoute> {
                    HomeScreen(
                        onOpenTasks = { navController.navigateToTopLevel(TopLevelDestination.TASKS) },
                        onOpenReminders = { navController.navigate(RemindersRoute()) },
                        onAddReminder = { navController.navigate(RemindersRoute(create = true)) },
                        onOpenNews = { navController.navigateToTopLevel(TopLevelDestination.NEWS) },
                        onOpenApps = { navController.navigateToTopLevel(TopLevelDestination.APPS) },
                        onOpenSettings = { navController.navigateToTopLevel(TopLevelDestination.SETTINGS) },
                        onOpenWeather = { navController.navigate(WeatherRoute) },
                    )
                }
                composable<TasksRoute> { entry ->
                    TasksScreen(
                        openTaskId = entry.toRoute<TasksRoute>().openTaskId,
                        onOpenReminders = { navController.navigate(RemindersRoute()) },
                    )
                }
                composable<NewsRoute> { NewsScreen() }
                composable<AppsRoute> { AppsScreen() }
                composable<SettingsRoute> {
                    SettingsScreen(
                        onOpenWeather = { navController.navigate(WeatherRoute) },
                        onOpenLegal = { navController.navigate(LegalRoute(it)) },
                        onManageApps = { navController.navigateToTopLevel(TopLevelDestination.APPS) },
                    )
                }
                composable<RemindersRoute>(
                    enterTransition = { slideInHorizontally(tween(300)) { it / 4 } + fadeIn(tween(300)) },
                    popExitTransition = { slideOutHorizontally(tween(260)) { it / 4 } + fadeOut(tween(200)) },
                ) { entry ->
                    val route = entry.toRoute<RemindersRoute>()
                    RemindersScreen(
                        highlightId = route.highlightId,
                        startCreating = route.create,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable<WeatherRoute>(
                    enterTransition = { slideInHorizontally(tween(300)) { it / 4 } + fadeIn(tween(300)) },
                    popExitTransition = { slideOutHorizontally(tween(260)) { it / 4 } + fadeOut(tween(200)) },
                ) {
                    WeatherScreen(onBack = { navController.popBackStack() })
                }
                composable<LegalRoute>(
                    enterTransition = { slideInHorizontally(tween(300)) { it / 4 } + fadeIn(tween(300)) },
                    popExitTransition = { slideOutHorizontally(tween(260)) { it / 4 } + fadeOut(tween(200)) },
                ) { entry ->
                    LegalScreen(document = entry.toRoute<LegalRoute>().document, onBack = { navController.popBackStack() })
                }
            }

            SystemNavigationBarBackdrop(Modifier.align(Alignment.BottomCenter))

            AnimatedVisibility(
                visible = currentTopLevel != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                AuraBottomBar(
                    current = currentTopLevel,
                    onSelect = { navController.navigateToTopLevel(it) },
                    hazeState = hazeState,
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        PaddingValues(
                            start = 12.dp,
                            end = 12.dp,
                            bottom = if (currentTopLevel != null) bottomBarPadding - 8.dp else navBarInset + 12.dp,
                        ),
                    ),
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                    containerColor = AuraTheme.colors.textPrimary,
                    contentColor = AuraTheme.colors.background,
                    actionColor = AuraTheme.colors.accent.gradientStart,
                )
            }
        }
    }
}

/**
 * With three-button navigation the system buttons are drawn on top of the app (edge-to-edge),
 * so content scrolling underneath made them look misplaced. This paints the app background
 * behind the buttons, with a soft fade, so they sit on a calm, readable strip. With gesture
 * navigation the tappable inset is zero and nothing is drawn.
 */
@Composable
private fun SystemNavigationBarBackdrop(modifier: Modifier = Modifier) {
    val colors = AuraTheme.colors
    val buttonsHeight = WindowInsets.tappableElement.asPaddingValues().calculateBottomPadding()
    if (buttonsHeight <= 0.dp) return
    val fade = 18.dp
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(buttonsHeight + fade)
            .background(
                Brush.verticalGradient(
                    0f to colors.background.copy(alpha = 0f),
                    (fade / (buttonsHeight + fade)) to colors.background.copy(alpha = 0.96f),
                    1f to colors.background,
                ),
            ),
    )
}

fun NavController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo<HomeRoute> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
