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
)

object KnownApps {
    val all: List<KnownApp> = listOf(
        KnownApp("TikTok", listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill"), "https://www.tiktok.com"),
        KnownApp("Instagram", listOf("com.instagram.android"), "https://www.instagram.com"),
        KnownApp("YouTube", listOf("com.google.android.youtube"), "https://www.youtube.com"),
        KnownApp("Spotify", listOf("com.spotify.music"), "https://open.spotify.com"),
        KnownApp("Discord", listOf("com.discord"), "https://discord.com/app"),
        KnownApp("WhatsApp", listOf("com.whatsapp"), "https://web.whatsapp.com"),
        KnownApp("Viber", listOf("com.viber.voip"), null),
        KnownApp("Telegram", listOf("org.telegram.messenger"), "https://web.telegram.org"),
        KnownApp("Messenger", listOf("com.facebook.orca"), "https://www.messenger.com"),
        KnownApp("Facebook", listOf("com.facebook.katana"), "https://www.facebook.com"),
        KnownApp("Reddit", listOf("com.reddit.frontpage"), "https://www.reddit.com"),
        KnownApp("X", listOf("com.twitter.android"), "https://x.com"),
        KnownApp("Snapchat", listOf("com.snapchat.android"), "https://web.snapchat.com"),
        KnownApp("Chrome", listOf("com.android.chrome"), null),
        KnownApp("Gmail", listOf("com.google.android.gm"), "https://mail.google.com"),
        KnownApp("Google Maps", listOf("com.google.android.apps.maps"), "https://maps.google.com"),
        KnownApp("Netflix", listOf("com.netflix.mediaclient"), "https://www.netflix.com"),
        KnownApp("Twitch", listOf("tv.twitch.android.app"), "https://www.twitch.tv"),
        KnownApp("Pinterest", listOf("com.pinterest"), "https://www.pinterest.com"),
        KnownApp("LinkedIn", listOf("com.linkedin.android"), "https://www.linkedin.com"),
    )

    fun forPackage(packageName: String): KnownApp? = all.firstOrNull { packageName in it.packageNames }
}
