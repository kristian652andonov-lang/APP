package com.aura.dashboard.data.news

/**
 * News API configuration injected at build time from local.properties / environment variables
 * (see app/build.gradle.kts). For production, point [baseUrl] at a proxy that holds the key
 * server-side and leave [apiKey] empty, so no secret ships inside the APK.
 */
data class NewsApiConfig(
    private val baseUrl: String,
    val apiKey: String,
) {
    val effectiveBaseUrl: String = baseUrl.trim().ifEmpty { DEFAULT_BASE_URL }.let { if (it.endsWith('/')) it else "$it/" }

    /** True when either a key or a custom (proxy) endpoint is configured. */
    val isConfigured: Boolean = apiKey.isNotBlank() || effectiveBaseUrl != DEFAULT_BASE_URL

    companion object {
        const val DEFAULT_BASE_URL = "https://newsdata.io/"
    }
}
