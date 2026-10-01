package com.aura.dashboard.ui.news

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.aura.dashboard.R
import com.aura.dashboard.domain.model.Article
import com.aura.dashboard.ui.components.GlassCard
import com.aura.dashboard.ui.components.SkeletonBlock
import com.aura.dashboard.ui.theme.AuraTheme
import com.aura.dashboard.ui.util.Formatting
import java.time.Instant

/** Large card with image on top; used for the featured story and the Home carousel. */
@Composable
fun FeaturedArticleCard(
    article: Article,
    now: Instant,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    imageHeight: Dp = 180.dp,
    summaryLines: Int = 2,
) {
    GlassCard(
        modifier = modifier,
        onClick = onClick,
        onClickLabel = stringResource(R.string.news_open_article_label, article.sourceName),
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(26.dp),
    ) {
        ArticleImage(
            url = article.imageUrl,
            modifier = Modifier
                .fillMaxWidth()
                .height(imageHeight),
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        )
        Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 16.dp)) {
            SourceLine(article, now)
            Spacer(Modifier.height(6.dp))
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium,
                color = AuraTheme.colors.textPrimary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (article.summary.isNotBlank() && summaryLines > 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = article.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AuraTheme.colors.textSecondary,
                    maxLines = summaryLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Compact row card with a thumbnail on the right. */
@Composable
fun CompactArticleCard(article: Article, now: Instant, onClick: () -> Unit, modifier: Modifier = Modifier) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        onClickLabel = stringResource(R.string.news_open_article_label, article.sourceName),
        contentPadding = PaddingValues(14.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = 10.dp,
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                SourceLine(article, now)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = AuraTheme.colors.textPrimary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (article.summary.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = article.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = AuraTheme.colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            ArticleImage(
                url = article.imageUrl,
                modifier = Modifier
                    .width(96.dp)
                    .aspectRatio(1f),
                shape = RoundedCornerShape(18.dp),
            )
        }
    }
}

/** "Source · 20 min ago" — the publisher is always visible. */
@Composable
fun SourceLine(article: Article, now: Instant) {
    val colors = AuraTheme.colors
    val time = article.publishedAt?.let { Formatting.relativeTime(it, now) }
    Text(
        text = if (time != null) "${article.sourceName} · $time" else article.sourceName,
        style = MaterialTheme.typography.labelMedium,
        color = colors.accent.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun ArticleImage(url: String?, modifier: Modifier, shape: RoundedCornerShape) {
    val colors = AuraTheme.colors
    Box(
        modifier = modifier
            .clip(shape)
            .background(Brush.linearGradient(listOf(colors.accent.container, colors.glowSecondary))),
        contentAlignment = Alignment.Center,
    ) {
        // The placeholder stays underneath; the photo simply covers it once loaded.
        Icon(
            Icons.Rounded.Newspaper,
            contentDescription = null,
            tint = colors.accent.primary.copy(alpha = 0.6f),
            modifier = Modifier.size(32.dp),
        )
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun FeaturedArticleSkeleton(modifier: Modifier = Modifier, imageHeight: Dp = 180.dp) {
    GlassCard(modifier = modifier, contentPadding = PaddingValues(0.dp), shape = RoundedCornerShape(26.dp)) {
        SkeletonBlock(
            Modifier
                .fillMaxWidth()
                .height(imageHeight),
            RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        )
        Column(Modifier.padding(18.dp)) {
            SkeletonBlock(Modifier.width(110.dp).height(12.dp))
            Spacer(Modifier.height(10.dp))
            SkeletonBlock(Modifier.fillMaxWidth().height(16.dp))
            Spacer(Modifier.height(8.dp))
            SkeletonBlock(Modifier.fillMaxWidth(0.7f).height(16.dp))
        }
    }
}

@Composable
fun CompactArticleSkeleton(modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp), shape = RoundedCornerShape(24.dp), elevation = 10.dp) {
        Row {
            Column(Modifier.weight(1f)) {
                SkeletonBlock(Modifier.width(90.dp).height(12.dp))
                Spacer(Modifier.height(10.dp))
                SkeletonBlock(Modifier.fillMaxWidth().height(14.dp))
                Spacer(Modifier.height(8.dp))
                SkeletonBlock(Modifier.fillMaxWidth(0.8f).height(14.dp))
                Spacer(Modifier.height(8.dp))
                SkeletonBlock(Modifier.fillMaxWidth(0.5f).height(12.dp))
            }
            Spacer(Modifier.width(12.dp))
            SkeletonBlock(Modifier.size(96.dp), RoundedCornerShape(18.dp))
        }
    }
}
