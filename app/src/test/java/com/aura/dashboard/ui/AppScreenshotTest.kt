package com.aura.dashboard.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.request.allowHardware
import coil3.imageDecoderEnabled
import com.aura.dashboard.MainActivity
import com.aura.dashboard.data.preferences.UserPreferencesRepository
import com.aura.dashboard.data.repository.NewsRepository
import com.aura.dashboard.data.repository.QuickAppRepository
import com.aura.dashboard.data.repository.ReminderRepository
import com.aura.dashboard.data.repository.TaskRepository
import com.aura.dashboard.domain.model.NewsCategory
import com.aura.dashboard.domain.model.Priority
import com.aura.dashboard.domain.model.Reminder
import com.aura.dashboard.domain.model.Repeat
import com.aura.dashboard.domain.model.Task
import com.aura.dashboard.domain.model.ThemeMode
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowNetworkCapabilities
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Drives the real app (real Hilt graph, Room, DataStore and live news feeds) on the JVM and saves
 * screenshots of every screen. Opt-in because it needs network access:
 *   ./gradlew :app:testDebugUnitTest --tests '*AppScreenshotTest*' -Paura.screenshots=true
 * Images are written to app/build/screenshots.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppScreenshotTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createEmptyComposeRule()

    @Inject lateinit var preferences: UserPreferencesRepository
    @Inject lateinit var tasks: TaskRepository
    @Inject lateinit var reminders: ReminderRepository
    @Inject lateinit var quickApps: QuickAppRepository
    @Inject lateinit var news: NewsRepository

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val outputDir = File(System.getProperty("aura.screenshotDir") ?: "build/screenshots").apply { mkdirs() }

    @Before
    fun setUp() {
        assumeTrue("Screenshot tests are opt-in (-Paura.screenshots=true)", System.getProperty("aura.screenshots") == "true")
        // Robolectric's default network has no INTERNET capability; the app would report "offline".
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        val capabilities = ShadowNetworkCapabilities.newInstance()
        shadowOf(capabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        shadowOf(connectivity).setNetworkCapabilities(connectivity.activeNetwork, capabilities)
        hiltRule.inject()
        // Robolectric implements BitmapFactory but not ImageDecoder, so decode the classic way.
        SingletonImageLoader.setUnsafe { ImageLoader.Builder(it).allowHardware(false).imageDecoderEnabled(false).build() }
    }

    @Test
    fun captureAllScreens() {
        installSampleApps()
        runBlocking { seed() }
        // Warm the cache with live headlines so screens render real news immediately.
        runBlocking { news.refresh("BG", NewsCategory.TOP, 20) }

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForNews()
            capture("01_home_light")

            tab("Tasks")
            capture("02_tasks_light")
            composeRule.onNode(hasContentDescription("Add task") and hasClickAction()).performClick()
            settle()
            composeRule.onNodeWithText("New task").assertExists()
            capture("03_task_editor_light")
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            settle()

            tab("News")
            settle(16)
            capture("04_news_light")

            tab("Apps")
            settle(8)
            capture("05_apps_light")

            tab("Settings")
            capture("06_settings_light")

            tab("Home")
            composeRule.onAllNodesWithText("Next reminder")[0].performClick()
            settle()
            capture("07_reminders_light")
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            settle()

            runBlocking { preferences.setThemeMode(ThemeMode.DARK) }
            settle(10)
            capture("08_home_dark")
            tab("Tasks")
            capture("09_tasks_dark")
            tab("News")
            settle(10)
            capture("10_news_dark")
            tab("Settings")
            capture("11_settings_dark")
        }
    }

    @Test
    fun captureOnboarding() {
        ActivityScenario.launch(MainActivity::class.java).use {
            settle()
            capture("20_onboarding_welcome")
            composeRule.onAllNodesWithText("Next")[0].performClick()
            settle()
            capture("21_onboarding_tasks")
            composeRule.onAllNodesWithText("Skip")[0].performClick()
            settle()
            capture("22_onboarding_name")
            composeRule.onAllNodesWithText("Continue")[0].performClick()
            settle()
            capture("23_onboarding_country")
            composeRule.onAllNodesWithText("Continue")[0].performClick()
            settle()
            capture("24_onboarding_notifications")
        }
    }

    private suspend fun seed() {
        preferences.completeOnboarding("Kristian", "BG")
        quickApps.seedDefaultsIfNeeded()
        val today = LocalDate.now()
        val now = LocalDateTime.now()
        tasks.save(Task(title = "Finish homework", dueDate = today, dueTime = LocalTime.of(17, 0), priority = Priority.HIGH))
        tasks.save(Task(title = "Go to the gym", dueDate = today, dueTime = LocalTime.of(19, 30), repeat = Repeat.WEEKDAYS))
        tasks.save(Task(title = "Buy groceries", dueDate = today, notes = "Milk, eggs, berries"))
        val clean = tasks.save(Task(title = "Clean room", dueDate = today))
        val read = tasks.save(Task(title = "Read 20 pages", dueDate = today, priority = Priority.LOW))
        tasks.save(Task(title = "Call grandma", dueDate = today.plusDays(1), dueTime = LocalTime.of(12, 0)))
        tasks.save(Task(title = "Plan weekend trip", dueDate = today.plusDays(3)))
        tasks.setCompleted(clean, true)
        tasks.setCompleted(read, true)

        val breakTime = now.plusMinutes(75).truncatedTo(ChronoUnit.MINUTES)
        reminders.save(Reminder(title = "Take a break", description = "Stretch and rest your eyes", startDate = breakTime.toLocalDate(), time = breakTime.toLocalTime()))
        reminders.save(Reminder(title = "Drink water", startDate = today, time = LocalTime.of(14, 30), repeat = Repeat.DAILY))
        reminders.save(Reminder(title = "Team stand-up", startDate = today, time = LocalTime.of(9, 30), repeat = Repeat.WEEKDAYS))
    }

    private fun tab(label: String) {
        composeRule.onNode(hasText(label) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)).performClick()
        settle()
    }

    private fun waitForNews() {
        composeRule.waitUntil(timeoutMillis = 60_000) {
            runBlocking { news.observeArticles("BG", NewsCategory.TOP).first().isNotEmpty() }
        }
        settle(16)
    }

    /** Lets background work (Room, network, image decoding) finish and recomposition settle. */
    private fun settle(rounds: Int = 4) {
        repeat(rounds) {
            Thread.sleep(250)
            composeRule.waitForIdle()
        }
    }

    /** Captures the screen plus a dialog/bottom sheet on top of it, if one is showing. */
    private fun capture(name: String) {
        composeRule.waitForIdle()
        val screen = composeRule.onAllNodes(isRoot())[0].captureToImage().asAndroidBitmap()
        val output = screen.copy(Bitmap.Config.ARGB_8888, true)
        ShadowDialog.getLatestDialog()?.takeIf { it.isShowing }?.window?.decorView?.let { decor ->
            // Robolectric's window capture only sees the activity, so draw the dialog view directly.
            if (decor.width > 0 && decor.height > 0) {
                val overlay = Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888)
                decor.draw(Canvas(overlay))
                Canvas(output).drawBitmap(overlay, 0f, (output.height - overlay.height).toFloat(), null)
            }
        }
        File(outputDir, "$name.png").outputStream().use { output.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun installSampleApps() {
        val packageManager = shadowOf(context.packageManager)
        listOf(
            Triple("com.zhiliaoapp.musically", "TikTok", intArrayOf(0xFF111111.toInt(), 0xFF25F4EE.toInt())),
            Triple("com.instagram.android", "Instagram", intArrayOf(0xFFFEDA75.toInt(), 0xFFD62976.toInt())),
            Triple("com.google.android.youtube", "YouTube", intArrayOf(0xFFFF4E45.toInt(), 0xFFFF0000.toInt())),
            Triple("com.spotify.music", "Spotify", intArrayOf(0xFF1ED760.toInt(), 0xFF1DB954.toInt())),
            Triple("com.discord", "Discord", intArrayOf(0xFF7289DA.toInt(), 0xFF5865F2.toInt())),
            Triple("com.whatsapp", "WhatsApp", intArrayOf(0xFF43D854.toInt(), 0xFF25D366.toInt())),
            Triple("com.viber.voip", "Viber", intArrayOf(0xFF8F5DB7.toInt(), 0xFF7360F2.toInt())),
            Triple("com.google.android.gm", "Gmail", intArrayOf(0xFFF4F4F4.toInt(), 0xFFEA4335.toInt())),
            Triple("org.telegram.messenger", "Telegram", intArrayOf(0xFF37AEE2.toInt(), 0xFF1E96C8.toInt())),
            Triple("com.android.chrome", "Chrome", intArrayOf(0xFFFBBC05.toInt(), 0xFF34A853.toInt())),
            Triple("com.example.notes", "Notes", intArrayOf(0xFFFFE08A.toInt(), 0xFFFFC107.toInt())),
            Triple("com.example.weather", "Weather", intArrayOf(0xFF8EC5FC.toInt(), 0xFF4A90E2.toInt())),
        ).forEach { (pkg, label, colors) ->
            val appInfo = ApplicationInfo().apply {
                packageName = pkg
                nonLocalizedLabel = label
                flags = ApplicationInfo.FLAG_INSTALLED
                enabled = true
            }
            packageManager.installPackage(PackageInfo().apply { packageName = pkg; applicationInfo = appInfo })
            val component = ComponentName(pkg, "$pkg.MainActivity")
            packageManager.addOrUpdateActivity(
                ActivityInfo().apply {
                    packageName = pkg
                    name = component.className
                    applicationInfo = appInfo
                    nonLocalizedLabel = label
                    exported = true
                    enabled = true
                },
            )
            packageManager.addIntentFilterForActivity(
                component,
                IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) },
            )
            packageManager.setApplicationIcon(pkg, BitmapDrawable(context.resources, icon(label, colors)))
        }
    }

    private fun icon(label: String, colors: IntArray): Bitmap {
        val size = 192
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, size.toFloat(), size.toFloat(), colors[0], colors[1], Shader.TileMode.CLAMP)
        }
        canvas.drawRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), 52f, 52f, paint)
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (label == "Gmail") 0xFFEA4335.toInt() else 0xFFFFFFFF.toInt()
            textSize = 96f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(label.take(1), size / 2f, size / 2f - (text.descent() + text.ascent()) / 2f, text)
        return bitmap
    }
}
