package com.aura.dashboard.data.news

import com.aura.dashboard.data.news.api.NewsDataService
import com.aura.dashboard.domain.model.NewsCategory
import com.aura.dashboard.domain.model.NewsError
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NewsDataProviderTest {

    private val server = MockWebServer()
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; explicitNulls = false; isLenient = true }
    private lateinit var provider: NewsDataProvider

    @Before
    fun setUp() {
        server.start()
        val service = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(NewsDataService::class.java)
        provider = NewsDataProvider(service, NewsApiConfig(baseUrl = server.url("/").toString(), apiKey = "test-key"), json)
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun article(id: Int, duplicate: Boolean = false) = """
        {"article_id":"$id","title":"Headline $id","link":"https://news.example.bg/$id",
         "description":"Summary <b>$id</b>","pubDate":"2026-10-01 19:20:00","pubDateTZ":"UTC",
         "image_url":"https://img.example.bg/$id.jpg","source_id":"example","source_name":"Example News",
         "source_url":"https://www.example.bg","duplicate":$duplicate}
    """.trimIndent()

    @Test
    fun `maps articles, skips duplicates and follows pagination`() = runTest {
        server.enqueue(MockResponse.Builder().body("""{"status":"success","totalResults":3,"results":[${article(1)},${article(2, duplicate = true)}],"nextPage":"p2"}""").build())
        server.enqueue(MockResponse.Builder().body("""{"status":"success","totalResults":3,"results":[${article(3)}],"nextPage":null}""").build())

        val articles = provider.fetch("BG", NewsCategory.LOCAL, limit = 5)

        assertEquals(listOf("Headline 1", "Headline 3"), articles.map { it.title })
        val first = articles.first()
        assertEquals("Summary 1", first.summary)
        assertEquals("Example News", first.sourceName)
        assertEquals("example.bg", first.sourceDomain)
        assertEquals(Instant.parse("2026-10-01T19:20:00Z"), first.publishedAt)

        val request = server.takeRequest()
        val url = request.url
        assertEquals("/api/1/latest", url.encodedPath)
        assertEquals("bg", url.queryParameter("country"))
        assertEquals("domestic", url.queryParameter("category"))
        assertEquals("test-key", url.queryParameter("apikey"))
        assertEquals("p2", server.takeRequest().url.queryParameter("page"))
    }

    @Test
    fun `unauthorized maps to a friendly error`() = runTest {
        server.enqueue(MockResponse.Builder().code(401).body("""{"status":"error","results":{"message":"Invalid key","code":"Unauthorized"}}""").build())
        val error = assertFailsWith<NewsException> { provider.fetch("BG", NewsCategory.TOP, 10) }
        assertEquals(NewsError.UNAUTHORIZED, error.error)
    }

    @Test
    fun `rate limit maps to rate limited`() = runTest {
        server.enqueue(MockResponse.Builder().code(429).body("{}").build())
        val error = assertFailsWith<NewsException> { provider.fetch("BG", NewsCategory.TOP, 10) }
        assertEquals(NewsError.RATE_LIMITED, error.error)
    }

    @Test
    fun `error status in a 200 response is an invalid response`() = runTest {
        server.enqueue(MockResponse.Builder().body("""{"status":"error","results":{"message":"Bad"}}""").build())
        val error = assertFailsWith<NewsException> { provider.fetch("BG", NewsCategory.TOP, 10) }
        assertEquals(NewsError.INVALID_RESPONSE, error.error)
    }

    @Test
    fun `malformed json never crashes`() = runTest {
        server.enqueue(MockResponse.Builder().body("<html>oops</html>").build())
        val error = assertFailsWith<NewsException> { provider.fetch("BG", NewsCategory.TOP, 10) }
        assertTrue(error.error == NewsError.INVALID_RESPONSE || error.error == NewsError.UNKNOWN)
    }

    @Test
    fun `config is only considered set up with a key or a custom endpoint`() {
        assertEquals(false, NewsApiConfig("", "").isConfigured)
        assertEquals(true, NewsApiConfig("", "abc").isConfigured)
        assertEquals(true, NewsApiConfig("https://proxy.example.workers.dev", "").isConfigured)
        assertEquals("https://proxy.example.workers.dev/", NewsApiConfig("https://proxy.example.workers.dev", "").effectiveBaseUrl)
    }
}
