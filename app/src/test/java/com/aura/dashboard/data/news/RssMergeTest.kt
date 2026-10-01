package com.aura.dashboard.data.news

import com.aura.dashboard.data.news.rss.RssNewsProvider
import com.aura.dashboard.domain.model.Article
import org.junit.Test
import java.time.Instant
import kotlin.test.assertEquals

class RssMergeTest {

    private fun article(url: String, title: String, at: String?) = Article(
        url = url,
        title = title,
        summary = "",
        imageUrl = null,
        sourceName = "S",
        sourceDomain = null,
        publishedAt = at?.let(Instant::parse),
    )

    @Test
    fun `merges feeds newest first without duplicates`() {
        val merged = RssNewsProvider.merge(
            listOf(
                listOf(article("https://a/1", "One", "2026-10-01T10:00:00Z"), article("https://a/2", "No date", null)),
                listOf(article("https://b/1", "Two", "2026-10-01T12:00:00Z"), article("https://a/1", "One again", "2026-10-01T10:00:00Z")),
                listOf(article("https://c/1", "ONE!", "2026-10-01T09:00:00Z")),
            ),
            limit = 10,
        )
        assertEquals(listOf("https://b/1", "https://a/1", "https://a/2"), merged.map { it.url })
    }

    @Test
    fun `respects the limit`() {
        val feed = (1..20).map { article("https://a/$it", "Title $it", "2026-10-01T10:${it.toString().padStart(2, '0')}:00Z") }
        assertEquals(5, RssNewsProvider.merge(listOf(feed), limit = 5).size)
    }

    @Test
    fun `future dated items rank after dated news`() {
        val now = Instant.parse("2026-10-01T21:00:00Z")
        val merged = RssNewsProvider.merge(
            listOf(
                listOf(article("https://a/future", "Scheduled", "2026-10-02T01:00:00Z")),
                listOf(article("https://b/fresh", "Fresh", "2026-10-01T20:00:00Z")),
                listOf(article("https://c/skew", "Clock skew", "2026-10-01T21:05:00Z")),
            ),
            limit = 10,
            now = now,
        )
        assertEquals(listOf("https://c/skew", "https://b/fresh", "https://a/future"), merged.map { it.url })
    }
}
