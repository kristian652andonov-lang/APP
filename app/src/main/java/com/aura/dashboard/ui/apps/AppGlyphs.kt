package com.aura.dashboard.ui.apps

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Facebook
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.automirrored.rounded.StickyNote2
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Whatsapp
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.aura.dashboard.data.apps.KnownApps
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Simple glyphs for themed Quick App icons. They evoke each app (a camera, a play button…) in
 * the app's own visual language instead of reproducing brand logos; the label names the app.
 */
object AppGlyphs {

    private val byPackage: Map<String, ImageVector> by lazy {
        mapOf(
            KnownApps.CLAUDE_PACKAGE to Spark,
            "com.zhiliaoapp.musically" to Icons.Rounded.MusicNote,
            "com.ss.android.ugc.trill" to Icons.Rounded.MusicNote,
            "com.instagram.android" to Icons.Rounded.PhotoCamera,
            "com.google.android.youtube" to Icons.Rounded.SmartDisplay,
            "com.spotify.music" to Icons.Rounded.GraphicEq,
            "com.discord" to Icons.Rounded.Headset,
            "com.whatsapp" to Icons.Rounded.Whatsapp,
            "com.viber.voip" to Icons.Rounded.Call,
            "org.telegram.messenger" to Icons.AutoMirrored.Rounded.Send,
            "com.facebook.orca" to Icons.Rounded.ChatBubble,
            "com.facebook.katana" to Icons.Rounded.Facebook,
            "com.reddit.frontpage" to Icons.Rounded.Forum,
            "com.android.chrome" to Icons.Rounded.Public,
            "com.google.android.gm" to Icons.Rounded.Mail,
            "com.google.android.apps.maps" to Icons.Rounded.Map,
            "com.netflix.mediaclient" to Icons.Rounded.Movie,
            "tv.twitch.android.app" to Icons.Rounded.LiveTv,
            "com.pinterest" to Icons.Rounded.PushPin,
            "com.linkedin.android" to Icons.Rounded.Work,
        )
    }

    /** Common system apps, matched by words in their package name. */
    private val byKeyword: List<Pair<String, ImageVector>> by lazy {
        listOf(
            "camera" to Icons.Rounded.PhotoCamera,
            "dialer" to Icons.Rounded.Call,
            "contacts" to Icons.Rounded.Person,
            "messaging" to Icons.Rounded.Sms,
            "mms" to Icons.Rounded.Sms,
            "settings" to Icons.Rounded.Settings,
            "gallery" to Icons.Rounded.Image,
            "photos" to Icons.Rounded.Image,
            "calendar" to Icons.Rounded.CalendarMonth,
            "deskclock" to Icons.Rounded.Schedule,
            "clock" to Icons.Rounded.Schedule,
            "calculator" to Icons.Rounded.Calculate,
            "vending" to Icons.Rounded.ShoppingBag,
            "music" to Icons.Rounded.MusicNote,
            "maps" to Icons.Rounded.Map,
            "browser" to Icons.Rounded.Public,
            "email" to Icons.Rounded.Mail,
            "mail" to Icons.Rounded.Mail,
            "keep" to Icons.AutoMirrored.Rounded.StickyNote2,
            "notes" to Icons.AutoMirrored.Rounded.StickyNote2,
            "weather" to Icons.Rounded.WbSunny,
            "files" to Icons.Rounded.Folder,
            "documents" to Icons.Rounded.Folder,
        )
    }

    /** The glyph for an app, or null to fall back to a monogram. */
    fun forPackage(packageName: String): ImageVector? =
        byPackage[packageName] ?: byKeyword.firstOrNull { (word, _) -> word in packageName.lowercase() }?.second

    /** Hue for the Pastel style: the brand-inspired hue for known apps, a stable hash otherwise. */
    fun hueFor(packageName: String): Float =
        KnownApps.forPackage(packageName)?.hue ?: (abs(packageName.hashCode()) % 360).toFloat()

    /** A soft radiant burst used for Claude. */
    val Spark: ImageVector by lazy {
        ImageVector.Builder(name = "Spark", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .path(fill = SolidColor(Color.Black), pathFillType = PathFillType.NonZero) {
                val rays = 10
                val center = 12f
                for (i in 0 until rays) {
                    val angle = (i * 2 * PI / rays - PI / 2).toFloat()
                    val length = if (i % 2 == 0) 10.2f else 8.4f
                    val halfWidth = 2f
                    val dx = cos(angle)
                    val dy = sin(angle)
                    val nx = -dy * halfWidth
                    val ny = dx * halfWidth
                    // A tapered, rounded ray from the center outward.
                    moveTo(center + nx * 0.55f, center + ny * 0.55f)
                    lineTo(center + dx * length + nx * 0.42f, center + dy * length + ny * 0.42f)
                    quadTo(
                        center + dx * (length + 1.4f),
                        center + dy * (length + 1.4f),
                        center + dx * length - nx * 0.42f,
                        center + dy * length - ny * 0.42f,
                    )
                    lineTo(center - nx * 0.55f, center - ny * 0.55f)
                    close()
                }
                // Solid core so the rays read as one shape.
                moveTo(center + 2.8f, center)
                arcTo(2.8f, 2.8f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = center - 2.8f, y1 = center)
                arcTo(2.8f, 2.8f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = center + 2.8f, y1 = center)
                close()
            }
            .build()
    }
}
