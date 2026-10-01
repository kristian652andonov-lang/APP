package com.aura.dashboard.core.util

/**
 * Minimal, dependency-free HTML helpers for feed content. Pure Kotlin so it works in JVM
 * unit tests (android.text.Html is unavailable there).
 */
object HtmlText {

    private val blockTags = Regex(
        "(?i)<\\s*/?\\s*(br|p|div|li|ul|ol|h[1-6]|tr|td|th|blockquote|figure|figcaption|section|article)\\b[^>]*>",
    )
    private val scriptOrStyle = Regex("(?is)<(script|style)\\b.*?</\\1\\s*>")
    private val tags = Regex("<[^>]*>")
    private val whitespace = Regex("\\s+")
    private val numericEntity = Regex("&#(x?)([0-9a-fA-F]+);")
    private val namedEntity = Regex("&([a-zA-Z]+);")
    private val imgSrc = Regex("(?i)<img\\b[^>]*?\\bsrc\\s*=\\s*[\"']([^\"']+)[\"']")

    private val named = mapOf(
        "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to " ",
        "ndash" to "–", "mdash" to "—", "hellip" to "…", "laquo" to "«", "raquo" to "»",
        "bdquo" to "„", "ldquo" to "“", "rdquo" to "”", "lsquo" to "‘", "rsquo" to "’",
        "copy" to "©", "reg" to "®", "trade" to "™", "euro" to "€", "deg" to "°",
        "middot" to "·", "bull" to "•", "times" to "×", "shy" to "",
    )

    fun decodeEntities(input: String): String {
        if ('&' !in input) return input
        val numeric = numericEntity.replace(input) { match ->
            val isHex = match.groupValues[1].isNotEmpty()
            val code = match.groupValues[2].toIntOrNull(if (isHex) 16 else 10)
            if (code != null && Character.isValidCodePoint(code)) String(Character.toChars(code)) else match.value
        }
        return namedEntity.replace(numeric) { match -> named[match.groupValues[1].lowercase()] ?: match.value }
    }

    /** Converts an HTML fragment to a single line of readable plain text. */
    fun toPlainText(html: String): String {
        // Feeds sometimes double-escape markup, so decode once before stripping tags.
        val decoded = if (html.contains("&lt;")) decodeEntities(html) else html
        val withoutScripts = scriptOrStyle.replace(decoded, " ")
        val withBreaks = blockTags.replace(withoutScripts, " ")
        val stripped = tags.replace(withBreaks, "")
        return whitespace.replace(decodeEntities(stripped), " ").trim()
    }

    fun firstImageSrc(html: String): String? {
        val decoded = if (html.contains("&lt;") || html.contains("&quot;")) decodeEntities(html) else html
        return imgSrc.find(decoded)?.groupValues?.get(1)?.let(::decodeEntities)
    }

    /** Shortens [text] to at most [maxLength] characters, preferring a word boundary. */
    fun truncate(text: String, maxLength: Int): String {
        if (text.length <= maxLength) return text
        val cut = text.substring(0, maxLength)
        val boundary = cut.lastIndexOf(' ').takeIf { it > maxLength * 2 / 3 } ?: cut.length
        return cut.substring(0, boundary).trimEnd(' ', ',', ';', ':', '-', '–') + "…"
    }
}
