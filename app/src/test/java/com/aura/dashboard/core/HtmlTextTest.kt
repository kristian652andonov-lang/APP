package com.aura.dashboard.core

import com.aura.dashboard.core.util.HtmlText
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HtmlTextTest {

    @Test
    fun `decodes named and numeric entities`() {
        assertEquals("Tom & Jerry „Сега“ — 'ok' é", HtmlText.decodeEntities("Tom &amp; Jerry &bdquo;Сега&ldquo; &mdash; &#39;ok&#x27; &#233;"))
    }

    @Test
    fun `strips tags and collapses whitespace`() {
        assertEquals(
            "Hello world. Second line",
            HtmlText.toPlainText("<p>Hello <b>world</b>.</p>\n<br/>  Second   line<script>alert(1)</script>"),
        )
    }

    @Test
    fun `handles double escaped markup`() {
        assertEquals("Text here", HtmlText.toPlainText("&lt;img src=&quot;x.jpg&quot; /&gt;&lt;br /&gt;Text here"))
    }

    @Test
    fun `finds the first image even when escaped`() {
        assertEquals("https://img.example.com/a.jpg", HtmlText.firstImageSrc("<p><img class='x' src=\"https://img.example.com/a.jpg\"/></p>"))
        assertEquals("https://m5.netinfo.bg/a.jpg", HtmlText.firstImageSrc("img src=&quot;https://m5.netinfo.bg/a.jpg&quot; &lt;img src=&quot;https://m5.netinfo.bg/a.jpg&quot;&gt;"))
        assertNull(HtmlText.firstImageSrc("no images"))
    }

    @Test
    fun `truncates at a word boundary with an ellipsis`() {
        val result = HtmlText.truncate("The quick brown fox jumps over the lazy dog", 20)
        assertTrue(result.endsWith("…"))
        assertTrue(result.length <= 21)
        assertEquals("The quick brown fox…", result)
    }
}
