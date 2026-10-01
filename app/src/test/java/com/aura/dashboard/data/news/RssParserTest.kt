package com.aura.dashboard.data.news

import com.aura.dashboard.data.news.rss.RssParser
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.kxml2.io.KXmlParser
import org.xmlpull.v1.XmlPullParser
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(Parameterized::class)
class RssParserTest(private val processNamespaces: Boolean) {

    companion object {
        /** android.util.Xml enables namespace processing; verify both modes behave the same. */
        @JvmStatic
        @Parameterized.Parameters(name = "processNamespaces={0}")
        fun modes() = listOf(true, false)
    }

    private val parser = RssParser {
        KXmlParser().apply { setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, processNamespaces) }
    }
    private fun parse(xml: String, publisher: String = "Test") = parser.parse(xml.byteInputStream(), publisher)

    @Test
    fun `parses rss items with escaped html description and inline image`() {
        val articles = parse(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0"><channel><title>dnevnik.bg</title>
              <item>
                <title>Здравната комисия одобри промени</title>
                <link>https://www.dnevnik.bg/bulgaria/2026/10/01/4961715_x/?ref=rss</link>
                <pubDate>Thu, 01 Oct 2026 22:20:00 +0300</pubDate>
                <description>&lt;img src="https://img.dnevnik.bg/shimg/zx350c_4903100.jpg" /&gt;&lt;br /&gt;
            Парламентарната здравна комисия прие на първо четене промени.</description>
              </item>
            </channel></rss>
            """.trimIndent(),
            publisher = "Dnevnik",
        )
        assertEquals(1, articles.size)
        val article = articles.single()
        assertEquals("Здравната комисия одобри промени", article.title)
        assertEquals("Парламентарната здравна комисия прие на първо четене промени.", article.summary)
        assertEquals("https://img.dnevnik.bg/shimg/zx350c_4903100.jpg", article.imageUrl)
        assertEquals("Dnevnik", article.sourceName)
        assertEquals("dnevnik.bg", article.sourceDomain)
        assertEquals(Instant.parse("2026-10-01T19:20:00Z"), article.publishedAt)
    }

    @Test
    fun `prefers enclosure and media images and upgrades http to https`() {
        val articles = parse(
            """
            <rss version="2.0" xmlns:media="http://search.yahoo.com/mrss/"><channel>
              <item>
                <title>With enclosure</title>
                <link>https://example.com/a</link>
                <enclosure url="http://cdn.example.com/a.jpg" type="image/jpeg" length="0"/>
                <pubDate>Thu, 01 Oct 2026 16:24:14 GMT</pubDate>
              </item>
              <item>
                <title>With media content</title>
                <link>https://example.com/b</link>
                <media:content url="https://cdn.example.com/b.png" medium="image"><media:title>x</media:title></media:content>
              </item>
              <item>
                <title>BBC thumbnail</title>
                <link>https://www.bbc.co.uk/news/c</link>
                <media:thumbnail width="240" height="134" url="https://ichef.bbci.co.uk/ace/standard/240/cpsprodpb/x.jpg"/>
              </item>
            </channel></rss>
            """.trimIndent(),
        )
        assertEquals("https://cdn.example.com/a.jpg", articles[0].imageUrl)
        assertEquals(Instant.parse("2026-10-01T16:24:14Z"), articles[0].publishedAt)
        assertEquals("https://cdn.example.com/b.png", articles[1].imageUrl)
        assertNull(articles[1].publishedAt)
        assertEquals("https://ichef.bbci.co.uk/ace/standard/800/cpsprodpb/x.jpg", articles[2].imageUrl)
    }

    @Test
    fun `parses atom entries`() {
        val articles = parse(
            """
            <feed xmlns="http://www.w3.org/2005/Atom">
              <title>Atom feed</title>
              <entry>
                <title type="html">Atom &amp;amp; friends</title>
                <link rel="alternate" href="https://example.org/atom-1"/>
                <link rel="enclosure" href="https://example.org/file.mp3"/>
                <published>2026-10-01T08:15:00+02:00</published>
                <updated>2026-10-01T09:00:00Z</updated>
                <summary>Short &lt;em&gt;summary&lt;/em&gt;</summary>
              </entry>
            </feed>
            """.trimIndent(),
        )
        val article = articles.single()
        assertEquals("Atom & friends", article.title)
        assertEquals("https://example.org/atom-1", article.url)
        assertEquals("Short summary", article.summary)
        assertEquals(Instant.parse("2026-10-01T06:15:00Z"), article.publishedAt)
    }

    @Test
    fun `skips items without a usable link or title`() {
        val articles = parse(
            """
            <rss><channel>
              <item><title>No link</title></item>
              <item><title></title><link>https://example.com/x</link></item>
              <item><title>Ftp link</title><link>ftp://example.com/x</link></item>
              <item><title>Protocol relative</title><link>//example.com/ok</link></item>
            </channel></rss>
            """.trimIndent(),
        )
        assertEquals(listOf("https://example.com/ok"), articles.map { it.url })
    }

    @Test
    fun `parses common date formats`() {
        assertEquals(Instant.parse("2026-10-01T19:20:00Z"), RssParser.parseDate("Thu, 01 Oct 2026 22:20:00 +0300"))
        assertEquals(Instant.parse("2026-10-01T20:20:00Z"), RssParser.parseDate("1 Oct 2026 20:20:00 GMT"))
        assertEquals(Instant.parse("2026-10-01T20:20:00Z"), RssParser.parseDate("2026-10-01T20:20:00Z"))
        assertEquals(Instant.parse("2026-10-01T18:20:00Z"), RssParser.parseDate("2026-10-01T20:20:00+02:00"))
        assertNull(RssParser.parseDate("not a date"))
        assertNull(RssParser.parseDate(null))
    }

    @Test
    fun `tolerates html entities that are not defined in xml`() {
        val articles = parse(
            """
            <rss><channel><item>
              <title>Quote&nbsp;test</title>
              <link>https://example.com/q</link>
            </item></channel></rss>
            """.trimIndent(),
        )
        assertTrue(articles.single().title.startsWith("Quote"))
    }

    @Test
    fun `reads content encoded images and dublin core dates`() {
        val articles = parse(
            """
            <rss version="2.0" xmlns:content="http://purl.org/rss/1.0/modules/content/" xmlns:dc="http://purl.org/dc/elements/1.1/"><channel>
              <item>
                <title>Tagesschau style</title>
                <link>https://www.tagesschau.de/a.html</link>
                <description>Kurz</description>
                <content:encoded><![CDATA[<p><img src="https://images.tagesschau.de/x.jpg" alt=""/></p><p>Text</p>]]></content:encoded>
                <dc:date>2026-10-01T20:00:00Z</dc:date>
              </item>
            </channel></rss>
            """.trimIndent(),
        )
        val article = articles.single()
        assertEquals("https://images.tagesschau.de/x.jpg", article.imageUrl)
        assertEquals("Kurz", article.summary)
        assertEquals(Instant.parse("2026-10-01T20:00:00Z"), article.publishedAt)
    }
}
