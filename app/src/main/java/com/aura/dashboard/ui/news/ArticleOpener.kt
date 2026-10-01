package com.aura.dashboard.ui.news

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.net.toUri

/**
 * Opens an article on the publisher's own website in a Custom Tab (the browser UI shows the
 * real address), falling back to any browser. Returns false if nothing can open it.
 */
fun openArticle(context: Context, url: String, toolbarColor: Color, dark: Boolean): Boolean {
    val uri = url.toUri()
    val params = CustomTabColorSchemeParams.Builder().setToolbarColor(toolbarColor.toArgb()).build()
    val customTab = CustomTabsIntent.Builder()
        .setShowTitle(true)
        .setUrlBarHidingEnabled(false)
        .setShareState(CustomTabsIntent.SHARE_STATE_ON)
        .setColorScheme(if (dark) CustomTabsIntent.COLOR_SCHEME_DARK else CustomTabsIntent.COLOR_SCHEME_LIGHT)
        .setDefaultColorSchemeParams(params)
        .build()
    return try {
        customTab.launchUrl(context, uri)
        true
    } catch (notFound: ActivityNotFoundException) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (stillNotFound: ActivityNotFoundException) {
            false
        }
    }
}
