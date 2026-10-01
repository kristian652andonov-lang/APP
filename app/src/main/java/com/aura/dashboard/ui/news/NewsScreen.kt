package com.aura.dashboard.ui.news

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.dashboard.R
import com.aura.dashboard.domain.model.NewsError
import com.aura.dashboard.ui.components.AuraChip
import com.aura.dashboard.ui.components.EmptyState
import com.aura.dashboard.ui.components.GlassCard
import com.aura.dashboard.ui.components.GlassIconButton
import com.aura.dashboard.ui.components.InfoPill
import com.aura.dashboard.ui.components.OfflinePill
import com.aura.dashboard.ui.components.ScreenHeader
import com.aura.dashboard.ui.navigation.LocalBottomBarPadding
import com.aura.dashboard.ui.navigation.LocalSnackbarHostState
import com.aura.dashboard.ui.theme.AuraTheme
import com.aura.dashboard.ui.util.Formatting
import kotlinx.coroutines.launch

@Composable
fun NewsScreen(viewModel: NewsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = AuraTheme.colors
    val context = LocalContext.current
    val resources = LocalResources.current
    val snackbar = LocalSnackbarHostState.current
    val bottomPadding = LocalBottomBarPadding.current
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val pullState = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()
    val hasArticles = state.articles.isNotEmpty()

    // Errors while cached news is visible are reported unobtrusively.
    LaunchedEffect(state.error, hasArticles) {
        val error = state.error
        if (error != null && hasArticles && error != NewsError.OFFLINE) {
            snackbar.showSnackbar(resources.getString(R.string.news_refresh_failed))
        }
    }

    val subtitle = buildString {
        append(state.country.flag).append(' ').append(state.country.displayName)
        state.lastUpdated?.let {
            append(" · ")
            append(resources.getString(R.string.news_updated, Formatting.relativeTime(it, state.now)))
        }
    }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing && hasArticles,
        onRefresh = viewModel::refresh,
        state = pullState,
        modifier = Modifier.fillMaxSize(),
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullState,
                isRefreshing = state.isRefreshing && hasArticles,
                color = colors.accent.primary,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = topPadding),
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = topPadding + 16.dp, bottom = bottomPadding + 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                ScreenHeader(
                    title = stringResource(R.string.news_title),
                    subtitle = subtitle,
                    modifier = Modifier.padding(horizontal = 20.dp),
                ) {
                    GlassIconButton(
                        icon = Icons.Rounded.Refresh,
                        contentDescription = stringResource(R.string.news_refresh),
                        onClick = viewModel::refresh,
                    )
                }
            }
            item(key = "status") {
                AnimatedVisibility(visible = !state.isOnline) {
                    Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                        OfflinePill()
                        if (hasArticles) {
                            Spacer(Modifier.width(8.dp))
                            InfoPill(text = stringResource(R.string.news_showing_saved), color = colors.textSecondary)
                        }
                    }
                }
            }
            if (state.categories.size > 1) {
                item(key = "categories") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.categories, key = { it.name }) { category ->
                            AuraChip(
                                label = Formatting.categoryLabel(context, category),
                                selected = category == state.selected,
                                onClick = { viewModel.selectCategory(category) },
                            )
                        }
                    }
                }
            }

            when {
                hasArticles -> {
                    itemsIndexed(state.articles, key = { _, article -> article.url }) { index, article ->
                        val open: () -> Unit = {
                            if (!openArticle(context, article.url, colors.background, colors.isDark)) {
                                scope.launch { snackbar.showSnackbar(resources.getString(R.string.error_no_browser)) }
                            }
                        }
                        if (index == 0) {
                            FeaturedArticleCard(
                                article = article,
                                now = state.now,
                                onClick = open,
                                summaryLines = 3,
                                modifier = Modifier
                                    .padding(horizontal = 20.dp)
                                    .fillMaxWidth()
                                    .animateItem(),
                            )
                        } else {
                            CompactArticleCard(
                                article = article,
                                now = state.now,
                                onClick = open,
                                modifier = Modifier
                                    .padding(horizontal = 20.dp)
                                    .animateItem(),
                            )
                        }
                    }
                    item(key = "attribution") {
                        Attribution(state.sourceInfo.publishers, state.sourceInfo.providerName)
                    }
                }
                !state.isLoaded || state.isRefreshing -> {
                    item(key = "skeleton-featured") {
                        FeaturedArticleSkeleton(Modifier.padding(horizontal = 20.dp).fillMaxWidth())
                    }
                    items(4, key = { "skeleton-$it" }) {
                        CompactArticleSkeleton(Modifier.padding(horizontal = 20.dp))
                    }
                }
                state.error != null -> item(key = "error") {
                    val error = state.error ?: NewsError.UNKNOWN
                    GlassCard(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
                        EmptyState(
                            icon = if (error == NewsError.OFFLINE) Icons.Rounded.WifiOff else Icons.Rounded.Language,
                            title = stringResource(error.titleRes()),
                            message = stringResource(error.messageRes()),
                            actionLabel = if (error != NewsError.UNSUPPORTED_COUNTRY) stringResource(R.string.action_retry) else null,
                            onAction = viewModel::refresh,
                        )
                    }
                }
                else -> item(key = "empty") {
                    GlassCard(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
                        EmptyState(
                            icon = Icons.Rounded.Newspaper,
                            title = stringResource(R.string.news_empty_title),
                            message = stringResource(R.string.news_empty_message),
                            actionLabel = stringResource(R.string.action_retry),
                            onAction = viewModel::refresh,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Attribution(publishers: List<String>, provider: String) {
    val colors = AuraTheme.colors
    val text = if (publishers.isNotEmpty()) {
        stringResource(R.string.news_attribution_publishers, publishers.joinToString(", "))
    } else {
        stringResource(R.string.news_attribution_provider, provider)
    }
    Box(Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
    Spacer(Modifier.height(4.dp))
}
