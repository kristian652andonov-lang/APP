package com.aura.dashboard.data.news.rss

import com.aura.dashboard.core.util.HtmlText
import com.aura.dashboard.domain.model.Article
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.net.URI
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.Locale

/**
 * Streaming parser for RSS 2.0 and Atom feeds. Extracts the image from the common places
 * publishers put it (enclosure, media:content, media:thumbnail, <image>, or inline <img>).
 *
 * @param newParser creates a fresh XmlPullParser (android.util.Xml in the app, kxml2 in tests).
 */
class RssParser(private val newParser: () -> XmlPullParser) {

    fun parse(input: InputStream, publisher: String): List<Article> {
        val parser = newParser()
        runCatching { parser.setFeature(RELAXED_FEATURE, true) }
        parser.setInput(input, null)

        val articles = mutableListOf<Article>()
        var item: ItemBuilder? = null
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val name = qualifiedName(parser)
                    val current = item
                    if (name == "item" || name == "entry") {
                        item = ItemBuilder()
                    } else if (current != null) {
                        readField(parser, name, current)
                    }
                }
                XmlPullParser.END_TAG -> {
                    val name = qualifiedName(parser)
                    if ((name == "item" || name == "entry") && item != null) {
                        item.build(publisher)?.let(articles::add)
                        item = null
                    }
                }
            }
            event = parser.next()
        }
        return articles
    }

    private fun readField(parser: XmlPullParser, name: String, item: ItemBuilder) {
        when (name) {
            "title" -> item.title = item.title ?: readText(parser)
            "link" -> {
                val href = parser.getAttributeValue(null, "href")
                if (href != null) {
                    val rel = parser.getAttributeValue(null, "rel")
                    if (rel == null || rel == "alternate") item.link = item.link ?: href
                } else {
                    item.link = item.link ?: readText(parser)
                }
            }
            "guid", "id" -> item.guid = item.guid ?: readText(parser)
            "description", "summary" -> item.description = item.description ?: readText(parser)
            "content:encoded", "content" -> item.content = item.content ?: readText(parser)
            "pubdate", "published", "dc:date" -> item.date = item.date ?: readText(parser)
            "updated" -> item.updated = item.updated ?: readText(parser)
            "enclosure", "media:content" -> {
                val url = parser.getAttributeValue(null, "url")
                val type = parser.getAttributeValue(null, "type").orEmpty()
                val medium = parser.getAttributeValue(null, "medium").orEmpty()
                if (url != null && (type.startsWith("image") || medium == "image" || looksLikeImage(url))) {
                    item.image = item.image ?: url
                }
            }
            "media:thumbnail" -> parser.getAttributeValue(null, "url")?.let { item.thumbnail = item.thumbnail ?: it }
            "image" -> readText(parser).takeIf { it.startsWith("http") || it.startsWith("//") }?.let {
                item.image = item.image ?: it
            }
        }
    }

    /**
     * "media:content"-style names regardless of namespace processing: android.util.Xml enables it
     * (reporting just "content" plus a namespace URI), other parsers keep the raw prefix.
     */
    private fun qualifiedName(parser: XmlPullParser): String {
        val name = parser.name.lowercase(Locale.ROOT)
        if (':' in name) return name
        val prefix = when (parser.namespace) {
            MEDIA_NAMESPACE -> "media:"
            CONTENT_NAMESPACE -> "content:"
            DUBLIN_CORE_NAMESPACE -> "dc:"
            else -> ""
        }
        return prefix + name
    }

    /** Reads all text inside the current element, tolerating nested markup. */
    private fun readText(parser: XmlPullParser): String {
        val text = StringBuilder()
        var depth = 1
        while (depth > 0) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> depth++
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.TEXT -> text.append(parser.text)
                XmlPullParser.END_DOCUMENT -> return text.toString().trim()
            }
        }
        return text.toString().trim()
    }

    private class ItemBuilder {
        var title: String? = null
        var link: String? = null
        var guid: String? = null
        var description: String? = null
        var content: String? = null
        var date: String? = null
        var updated: String? = null
        var image: String? = null
        var thumbnail: String? = null

        fun build(publisher: String): Article? {
            val url = normalizeUrl(link ?: guid?.takeIf { it.startsWith("http") }) ?: return null
            val cleanTitle = title?.let(HtmlText::toPlainText)?.takeIf { it.isNotBlank() } ?: return null
            val html = description?.takeIf { it.isNotBlank() } ?: content.orEmpty()
            val summary = HtmlText.truncate(HtmlText.toPlainText(html), MAX_SUMMARY_LENGTH)
                .takeUnless { it == cleanTitle }
                .orEmpty()
            val imageUrl = image
                ?: thumbnail
                ?: HtmlText.firstImageSrc(description.orEmpty())
                ?: HtmlText.firstImageSrc(content.orEmpty())
            return Article(
                url = url,
                title = cleanTitle,
                summary = summary,
                imageUrl = normalizeImageUrl(imageUrl),
                sourceName = publisher,
                sourceDomain = domainOf(url),
                publishedAt = parseDate(date) ?: parseDate(updated),
            )
        }
    }

    companion object {
        private const val RELAXED_FEATURE = "http://xmlpull.org/v1/doc/features.html#relaxed"
        private const val MEDIA_NAMESPACE = "http://search.yahoo.com/mrss/"
        private const val CONTENT_NAMESPACE = "http://purl.org/rss/1.0/modules/content/"
        private const val DUBLIN_CORE_NAMESPACE = "http://purl.org/dc/elements/1.1/"
        private const val MAX_SUMMARY_LENGTH = 280
        private val IMAGE_EXTENSIONS = listOf(".jpg", ".jpeg", ".png", ".webp", ".gif")

        private val rfc822 = DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .parseLenient()
            .optionalStart().appendPattern("EEE, ").optionalEnd()
            .appendPattern("d MMM ")
            .appendValueReduced(ChronoField.YEAR, 2, 4, 1970)
            .appendPattern(" HH:mm")
            .optionalStart().appendPattern(":ss").optionalEnd()
            .appendPattern(" ")
            .optionalStart().appendPattern("XX").optionalEnd()
            .optionalStart().appendPattern("z").optionalEnd()
            .toFormatter(Locale.ENGLISH)

        internal fun parseDate(raw: String?): Instant? {
            val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            runCatching { return ZonedDateTime.parse(value, rfc822).toInstant() }
            runCatching { return ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant() }
            runCatching { return OffsetDateTime.parse(value).toInstant() }
            runCatching { return Instant.parse(value) }
            runCatching { return LocalDateTime.parse(value).toInstant(ZoneOffset.UTC) }
            return null
        }

        internal fun normalizeUrl(raw: String?): String? {
            val value = raw?.trim()?.let(HtmlText::decodeEntities)?.takeIf { it.isNotEmpty() } ?: return null
            val absolute = if (value.startsWith("//")) "https:$value" else value
            return absolute.takeIf { it.startsWith("https://") || it.startsWith("http://") }
        }

        /** Images are always loaded over HTTPS; cleartext traffic is disabled. */
        internal fun normalizeImageUrl(raw: String?): String? {
            val url = normalizeUrl(raw) ?: return null
            val secure = if (url.startsWith("http://")) "https://" + url.removePrefix("http://") else url
            // BBC feeds reference 240px thumbnails; the same image is published at larger sizes.
            return if (secure.contains("ichef.bbci.co.uk") && secure.contains("/standard/240/")) {
                secure.replace("/standard/240/", "/standard/800/")
            } else {
                secure
            }
        }

        internal fun domainOf(url: String): String? =
            runCatching { URI(url).host?.removePrefix("www.") }.getOrNull()

        private fun looksLikeImage(url: String): Boolean {
            val path = url.substringBefore('?').lowercase(Locale.ROOT)
            return IMAGE_EXTENSIONS.any(path::endsWith)
        }
    }
}
