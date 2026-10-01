package com.aura.dashboard.data.news.rss

import com.aura.dashboard.domain.model.NewsCategory
import com.aura.dashboard.domain.model.NewsCategory.BUSINESS
import com.aura.dashboard.domain.model.NewsCategory.ENTERTAINMENT
import com.aura.dashboard.domain.model.NewsCategory.HEALTH
import com.aura.dashboard.domain.model.NewsCategory.LOCAL
import com.aura.dashboard.domain.model.NewsCategory.SCIENCE
import com.aura.dashboard.domain.model.NewsCategory.SPORTS
import com.aura.dashboard.domain.model.NewsCategory.TECHNOLOGY
import com.aura.dashboard.domain.model.NewsCategory.TOP
import com.aura.dashboard.domain.model.NewsCategory.WORLD

data class FeedSource(val publisher: String, val url: String)

/**
 * Official RSS feeds that publishers offer for syndication. These need no API key. Add a
 * country by adding an entry here; any other country uses the NewsData.io provider.
 */
object RssFeeds {

    private val bntNews = FeedSource("BNT News", "https://bntnews.bg/bg/rss/news.xml")
    // Dnevnik's catch-all feed also carries cinema listings, so its news sections are used instead.
    private val dnevnikBulgaria = FeedSource("Dnevnik", "https://www.dnevnik.bg/rss/?rubrid=1657")
    private val dnevnikWorld = FeedSource("Dnevnik", "https://www.dnevnik.bg/rss/?rubrid=1658")
    private val sega = FeedSource("Sega", "https://www.segabg.com/rss")
    private val chasa24 = FeedSource("24 Chasa", "https://www.24chasa.bg/rss")

    private fun bbc(path: String) = FeedSource("BBC News", "https://feeds.bbci.co.uk/$path")
    private fun tagesschau(path: String) = FeedSource("tagesschau", "https://www.tagesschau.de/$path")

    private val feeds: Map<String, Map<NewsCategory, List<FeedSource>>> = mapOf(
        "BG" to mapOf(
            TOP to listOf(bntNews, dnevnikBulgaria, sega, chasa24),
            LOCAL to listOf(dnevnikBulgaria),
            WORLD to listOf(dnevnikWorld, FeedSource("Actualno", "https://www.actualno.com/rss/world")),
            BUSINESS to listOf(
                FeedSource("Capital", "https://www.capital.bg/rss/"),
                FeedSource("Dnevnik", "https://www.dnevnik.bg/rss/?rubrid=1661"),
            ),
            TECHNOLOGY to listOf(FeedSource("Kaldata", "https://www.kaldata.com/feed/")),
            SPORTS to listOf(FeedSource("Gong.bg", "https://gong.bg/rss")),
        ),
        "GB" to mapOf(
            TOP to listOf(bbc("news/rss.xml")),
            LOCAL to listOf(bbc("news/uk/rss.xml")),
            WORLD to listOf(bbc("news/world/rss.xml")),
            BUSINESS to listOf(bbc("news/business/rss.xml")),
            TECHNOLOGY to listOf(bbc("news/technology/rss.xml")),
            SCIENCE to listOf(bbc("news/science_and_environment/rss.xml")),
            SPORTS to listOf(FeedSource("BBC Sport", "https://feeds.bbci.co.uk/sport/rss.xml")),
            ENTERTAINMENT to listOf(bbc("news/entertainment_and_arts/rss.xml")),
            HEALTH to listOf(bbc("news/health/rss.xml")),
        ),
        "DE" to mapOf(
            TOP to listOf(tagesschau("index~rss2.xml")),
            LOCAL to listOf(tagesschau("inland/index~rss2.xml")),
            WORLD to listOf(tagesschau("ausland/index~rss2.xml")),
            BUSINESS to listOf(tagesschau("wirtschaft/index~rss2.xml")),
            TECHNOLOGY to listOf(tagesschau("wissen/technologie/index~rss2.xml")),
            SCIENCE to listOf(tagesschau("wissen/index~rss2.xml")),
            SPORTS to listOf(FeedSource("sportschau", "https://www.sportschau.de/index~rss2.xml")),
        ),
    )

    fun forCountry(countryCode: String): Map<NewsCategory, List<FeedSource>> =
        feeds[countryCode.uppercase()].orEmpty()
}
