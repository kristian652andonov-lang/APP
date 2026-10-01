package com.aura.dashboard.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.aura.dashboard.data.local.entity.NewsArticleEntity
import com.aura.dashboard.data.local.entity.NewsFetchStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NewsDao {
    @Query("SELECT * FROM news_articles WHERE country_code = :country AND category = :category ORDER BY position")
    fun observeArticles(country: String, category: String): Flow<List<NewsArticleEntity>>

    @Query("SELECT fetched_at FROM news_fetch_state WHERE country_code = :country AND category = :category")
    fun observeFetchedAt(country: String, category: String): Flow<Long?>

    @Query("SELECT fetched_at FROM news_fetch_state WHERE country_code = :country AND category = :category")
    suspend fun getFetchedAt(country: String, category: String): Long?

    @Query("DELETE FROM news_articles WHERE country_code = :country AND category = :category")
    suspend fun deleteArticles(country: String, category: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<NewsArticleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFetchState(state: NewsFetchStateEntity)

    /** Atomically swaps the cached articles for a country/category with a fresh result. */
    @Transaction
    suspend fun replace(country: String, category: String, articles: List<NewsArticleEntity>, fetchedAt: Long) {
        deleteArticles(country, category)
        insertArticles(articles)
        upsertFetchState(NewsFetchStateEntity(country, category, fetchedAt))
    }

    /** Drops cache for countries the user no longer follows. */
    @Query("DELETE FROM news_articles WHERE country_code != :country")
    suspend fun deleteOtherCountries(country: String)

    @Query("DELETE FROM news_fetch_state WHERE country_code != :country")
    suspend fun deleteOtherCountryStates(country: String)
}
