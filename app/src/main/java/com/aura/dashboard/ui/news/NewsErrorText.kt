package com.aura.dashboard.ui.news

import androidx.annotation.StringRes
import com.aura.dashboard.R
import com.aura.dashboard.domain.model.NewsError

@StringRes
fun NewsError.titleRes(): Int = when (this) {
    NewsError.OFFLINE -> R.string.news_error_offline_title
    NewsError.UNSUPPORTED_COUNTRY -> R.string.news_error_unsupported_title
    NewsError.UNAUTHORIZED -> R.string.news_error_unauthorized_title
    NewsError.RATE_LIMITED -> R.string.news_error_rate_limited_title
    NewsError.SERVER, NewsError.INVALID_RESPONSE, NewsError.UNKNOWN -> R.string.news_error_generic_title
}

@StringRes
fun NewsError.messageRes(): Int = when (this) {
    NewsError.OFFLINE -> R.string.news_error_offline_message
    NewsError.UNSUPPORTED_COUNTRY -> R.string.news_error_unsupported_message
    NewsError.UNAUTHORIZED -> R.string.news_error_unauthorized_message
    NewsError.RATE_LIMITED -> R.string.news_error_rate_limited_message
    NewsError.SERVER, NewsError.INVALID_RESPONSE, NewsError.UNKNOWN -> R.string.news_error_generic_message
}
