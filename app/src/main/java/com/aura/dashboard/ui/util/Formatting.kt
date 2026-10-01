package com.aura.dashboard.ui.util

import android.content.Context
import android.text.format.DateFormat
import android.text.format.DateUtils
import com.aura.dashboard.R
import com.aura.dashboard.domain.model.NewsCategory
import com.aura.dashboard.domain.model.Priority
import com.aura.dashboard.domain.model.Repeat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Locale-aware formatting helpers for dates, times and enum labels. */
object Formatting {

    fun time(context: Context, time: LocalTime): String {
        val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
        return time.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
    }

    /** "Thursday, October 1" */
    fun longDate(date: LocalDate): String {
        val pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEEMMMMd")
        return date.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
    }

    /** "Today", "Tomorrow", "Yesterday", or "Mon, Oct 5" (with year if not this year). */
    fun relativeDate(context: Context, date: LocalDate, today: LocalDate): String = when (date) {
        today -> context.getString(R.string.date_today)
        today.plusDays(1) -> context.getString(R.string.date_tomorrow)
        today.minusDays(1) -> context.getString(R.string.date_yesterday)
        else -> {
            val skeleton = if (date.year == today.year) "EEEMMMd" else "EEEMMMdyyyy"
            date.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton), Locale.getDefault()))
        }
    }

    fun mediumDate(date: LocalDate): String =
        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()))

    /**
     * "5 min ago", "3 hr. ago", "Yesterday"… Timestamps in the future (some publishers schedule
     * items ahead) are shown as an absolute date and time rather than "in 3 hours".
     */
    fun relativeTime(instant: Instant, now: Instant): String {
        if (instant.isAfter(now.plusSeconds(60))) {
            val zoned = instant.atZone(java.time.ZoneId.systemDefault())
            val pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "MMMdjmm")
            return zoned.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
        }
        return DateUtils.getRelativeTimeSpanString(
            instant.toEpochMilli(),
            now.toEpochMilli(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE,
        ).toString()
    }

    fun repeatLabel(context: Context, repeat: Repeat): String = context.getString(
        when (repeat) {
            Repeat.NEVER -> R.string.repeat_never
            Repeat.DAILY -> R.string.repeat_daily
            Repeat.WEEKDAYS -> R.string.repeat_weekdays
            Repeat.WEEKENDS -> R.string.repeat_weekends
            Repeat.WEEKLY -> R.string.repeat_weekly
            Repeat.MONTHLY -> R.string.repeat_monthly
        },
    )

    fun priorityLabel(context: Context, priority: Priority): String = context.getString(
        when (priority) {
            Priority.LOW -> R.string.priority_low
            Priority.NORMAL -> R.string.priority_normal
            Priority.HIGH -> R.string.priority_high
        },
    )

    fun categoryLabel(context: Context, category: NewsCategory): String = context.getString(
        when (category) {
            NewsCategory.TOP -> R.string.news_category_top
            NewsCategory.LOCAL -> R.string.news_category_local
            NewsCategory.WORLD -> R.string.news_category_world
            NewsCategory.BUSINESS -> R.string.news_category_business
            NewsCategory.TECHNOLOGY -> R.string.news_category_technology
            NewsCategory.SCIENCE -> R.string.news_category_science
            NewsCategory.SPORTS -> R.string.news_category_sports
            NewsCategory.ENTERTAINMENT -> R.string.news_category_entertainment
            NewsCategory.HEALTH -> R.string.news_category_health
        },
    )
}
