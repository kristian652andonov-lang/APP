package com.aura.dashboard

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.dashboard.data.apps.AppIconLoader
import com.aura.dashboard.notifications.DeepLink
import com.aura.dashboard.ui.components.LocalAppIconLoader
import com.aura.dashboard.ui.navigation.AuraApp
import com.aura.dashboard.ui.theme.AuraTheme
import com.aura.dashboard.ui.theme.isDark
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var appIconLoader: AppIconLoader

    private val viewModel: MainViewModel by viewModels()

    /** Pending destination from a notification tap; consumed once navigation happened. */
    private val pendingDeepLink = MutableStateFlow<DeepLink?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { viewModel.uiState.value is MainUiState.Loading }
        if (savedInstanceState == null) pendingDeepLink.value = DeepLink.from(intent)

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val ready = state as? MainUiState.Ready ?: return@setContent
            val dark = ready.preferences.themeMode.isDark()

            DisposableEffect(dark) {
                val style = if (dark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, DARK_SCRIM)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }

            AuraTheme(darkTheme = dark, accent = ready.preferences.accentColor) {
                CompositionLocalProvider(LocalAppIconLoader provides appIconLoader) {
                    val deepLink by pendingDeepLink.collectAsStateWithLifecycle()
                    AuraApp(
                        preferences = ready.preferences,
                        deepLink = deepLink,
                        onDeepLinkConsumed = { pendingDeepLink.value = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        DeepLink.from(intent)?.let { pendingDeepLink.value = it }
    }

    private companion object {
        /** Used only on old devices that can't draw dark navigation-bar icons. */
        val DARK_SCRIM = Color.argb(0x80, 0x1B, 0x1B, 0x1B)
    }
}
