package com.aura.dashboard.data.apps

/**
 * Popular apps that get first-run suggestions and a web fallback when they aren't installed.
 * This list is only used for suggestions; any installed app can be added to Quick Apps.
 */
data class KnownApp(
    val name: String,
    /** Package names, most common first (some apps ship under regional package names). */
    val packageNames: List<String>,
    val webUrl: String?,
    /** Brand-inspired hue (0–360) used to tint themed icons in the Pastel style. */
    val hue: Float = 250f,
)

object KnownApps {
    const val CLAUDE_PACKAGE = "com.anthropic.claude"

    val all: List<KnownApp> = listOf(
        KnownApp("Claude", listOf(CLAUDE_PACKAGE), "https://claude.ai", hue = 18f),
        KnownApp("TikTok", listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill"), "https://www.tiktok.com", hue = 340f),
        KnownApp("Instagram", listOf("com.instagram.android"), "https://www.instagram.com", hue = 320f),
        KnownApp("YouTube", listOf("com.google.android.youtube"), "https://www.youtube.com", hue = 0f),
        KnownApp("Spotify", listOf("com.spotify.music"), "https://open.spotify.com", hue = 141f),
        KnownApp("Discord", listOf("com.discord"), "https://discord.com/app", hue = 235f),
        KnownApp("WhatsApp", listOf("com.whatsapp"), "https://web.whatsapp.com", hue = 142f),
        KnownApp("Viber", listOf("com.viber.voip"), null, hue = 270f),
        KnownApp("Telegram", listOf("org.telegram.messenger"), "https://web.telegram.org", hue = 200f),
        KnownApp("Messenger", listOf("com.facebook.orca"), "https://www.messenger.com", hue = 225f),
        KnownApp("Facebook", listOf("com.facebook.katana"), "https://www.facebook.com", hue = 221f),
        KnownApp("Reddit", listOf("com.reddit.frontpage"), "https://www.reddit.com", hue = 16f),
        KnownApp("X", listOf("com.twitter.android"), "https://x.com", hue = 220f),
        KnownApp("Snapchat", listOf("com.snapchat.android"), "https://web.snapchat.com", hue = 55f),
        KnownApp("Chrome", listOf("com.android.chrome"), null, hue = 45f),
        KnownApp("Gmail", listOf("com.google.android.gm"), "https://mail.google.com", hue = 4f),
        KnownApp("Google Maps", listOf("com.google.android.apps.maps"), "https://maps.google.com", hue = 130f),
        KnownApp("Netflix", listOf("com.netflix.mediaclient"), "https://www.netflix.com", hue = 357f),
        KnownApp("Twitch", listOf("tv.twitch.android.app"), "https://www.twitch.tv", hue = 264f),
        KnownApp("Pinterest", listOf("com.pinterest"), "https://www.pinterest.com", hue = 351f),
        KnownApp("LinkedIn", listOf("com.linkedin.android"), "https://www.linkedin.com", hue = 210f),
    )

    fun forPackage(packageName: String): KnownApp? = all.firstOrNull { packageName in it.packageNames }
}
