package com.aura.dashboard.domain.model

import com.aura.dashboard.domain.model.NewsCategory.BUSINESS
import com.aura.dashboard.domain.model.NewsCategory.LOCAL
import com.aura.dashboard.domain.model.NewsCategory.SPORTS
import com.aura.dashboard.domain.model.NewsCategory.TECHNOLOGY
import com.aura.dashboard.domain.model.NewsCategory.TOP
import com.aura.dashboard.domain.model.NewsCategory.WORLD

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class AccentColor { LAVENDER, OCEAN, ROSE, MINT, PEACH }

enum class TaskSort { TIME, PRIORITY, CREATED }

enum class NewsRefreshInterval(val hours: Long) {
    MANUAL(0), HOURLY(1), EVERY_3_HOURS(3), EVERY_6_HOURS(6), EVERY_12_HOURS(12), DAILY(24)
}

data class UserPreferences(
    val onboardingCompleted: Boolean = false,
    val userName: String = "",
    val avatarPath: String? = null,
    val countryCode: String = DEFAULT_COUNTRY,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColor: AccentColor = AccentColor.LAVENDER,
    val remindersEnabled: Boolean = true,
    val notificationSound: Boolean = true,
    val notificationVibration: Boolean = true,
    val newsCategories: Set<NewsCategory> = DEFAULT_NEWS_CATEGORIES,
    val newsArticleCount: Int = DEFAULT_ARTICLE_COUNT,
    val newsRefreshInterval: NewsRefreshInterval = NewsRefreshInterval.EVERY_3_HOURS,
    val showCompletedTasks: Boolean = true,
    val defaultPriority: Priority = Priority.NORMAL,
    val taskSort: TaskSort = TaskSort.TIME,
    val quickAppsSeeded: Boolean = false,
) {
    companion object {
        const val DEFAULT_COUNTRY = "BG"
        const val DEFAULT_ARTICLE_COUNT = 20
        val ARTICLE_COUNT_OPTIONS = listOf(10, 20, 30, 50)
        val DEFAULT_NEWS_CATEGORIES = setOf(TOP, LOCAL, WORLD, BUSINESS, TECHNOLOGY, SPORTS)
    }
}
