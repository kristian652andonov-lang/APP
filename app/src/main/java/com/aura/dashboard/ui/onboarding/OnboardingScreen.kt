package com.aura.dashboard.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.dashboard.R
import com.aura.dashboard.ui.components.GradientButton
import com.aura.dashboard.ui.components.glassBorderBrush
import com.aura.dashboard.ui.components.rememberNotificationPermissionState
import com.aura.dashboard.ui.settings.CountryList
import com.aura.dashboard.ui.tasks.EditorTextField
import com.aura.dashboard.ui.theme.AuraTheme
import kotlinx.coroutines.launch

private const val PAGE_COUNT = 6
private const val PAGE_NAME = 3
private const val PAGE_COUNTRY = 4
private const val PAGE_NOTIFICATIONS = 5

@Composable
fun OnboardingScreen(onFinished: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    val name by viewModel.name.collectAsStateWithLifecycle()
    val country by viewModel.country.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState { PAGE_COUNT }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val colors = AuraTheme.colors
    val notifications = rememberNotificationPermissionState { viewModel.finish(onFinished) }
    val page = pagerState.currentPage

    fun goTo(target: Int) {
        focusManager.clearFocus()
        scope.launch { pagerState.animateScrollToPage(target) }
    }

    BackHandler(enabled = page > 0) { goTo(page - 1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium,
                color = colors.accent.primary,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f),
            )
            if (page < PAGE_NAME) {
                TextButton(onClick = { goTo(PAGE_NAME) }) {
                    Text(stringResource(R.string.onboarding_skip), color = colors.textSecondary)
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            userScrollEnabled = page < PAGE_NAME,
            modifier = Modifier.weight(1f),
            beyondViewportPageCount = 1,
        ) { index ->
            when (index) {
                0 -> IntroPage(
                    title = stringResource(R.string.onboarding_title_1),
                    body = stringResource(R.string.onboarding_body_1),
                    icons = listOf(Icons.Rounded.WbSunny, Icons.Rounded.CalendarMonth, Icons.Rounded.CheckCircle),
                )
                1 -> IntroPage(
                    title = stringResource(R.string.onboarding_title_2),
                    body = stringResource(R.string.onboarding_body_2),
                    icons = listOf(Icons.Rounded.CheckCircle, Icons.Rounded.NotificationsActive, Icons.Rounded.CalendarMonth),
                )
                2 -> IntroPage(
                    title = stringResource(R.string.onboarding_title_3),
                    body = stringResource(R.string.onboarding_body_3),
                    icons = listOf(Icons.Rounded.Apps, Icons.Rounded.Newspaper, Icons.Rounded.WbSunny),
                )
                PAGE_NAME -> QuestionPage(title = stringResource(R.string.onboarding_name_title), body = stringResource(R.string.onboarding_name_body)) {
                    EditorTextField(
                        value = name,
                        onValueChange = viewModel::setName,
                        placeholder = stringResource(R.string.onboarding_name_hint),
                        large = true,
                    )
                }
                PAGE_COUNTRY -> QuestionPage(
                    title = stringResource(R.string.onboarding_country_title),
                    body = stringResource(R.string.onboarding_country_body),
                    fill = true,
                ) {
                    CountryList(selectedCode = country, onSelect = { viewModel.setCountry(it.code) })
                }
                else -> QuestionPage(
                    title = stringResource(R.string.onboarding_notifications_title),
                    body = stringResource(R.string.onboarding_notifications_body),
                ) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Orb(size = 180.dp, icons = listOf(Icons.Rounded.NotificationsActive))
                    }
                }
            }
        }

        // Page indicator: the current page is a wider pill, not just a different color.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .semantics { contentDescription = "${page + 1} / $PAGE_COUNT" },
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(PAGE_COUNT) { index ->
                val width by animateDpAsState(if (index == page) 22.dp else 8.dp, label = "dot")
                Box(
                    Modifier
                        .padding(horizontal = 3.dp)
                        .size(width = width, height = 8.dp)
                        .clip(CircleShape)
                        .background(if (index == page) colors.accent.primary else colors.accent.primary.copy(alpha = 0.2f)),
                )
            }
        }

        Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            when (page) {
                PAGE_NOTIFICATIONS -> {
                    GradientButton(
                        text = stringResource(R.string.onboarding_allow_notifications),
                        onClick = {
                            if (notifications.granted) viewModel.finish(onFinished) else notifications.request()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(onClick = { viewModel.finish(onFinished) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.onboarding_not_now), color = colors.textSecondary)
                    }
                }
                else -> GradientButton(
                    text = stringResource(if (page < PAGE_NAME) R.string.onboarding_next else R.string.onboarding_continue),
                    onClick = {
                        if (page == PAGE_COUNTRY && notifications.granted) viewModel.finish(onFinished) else goTo(page + 1)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun IntroPage(title: String, body: String, icons: List<ImageVector>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Orb(size = 230.dp, icons = icons)
        Spacer(Modifier.height(40.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            color = AuraTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = AuraTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun QuestionPage(title: String, body: String, fill: Boolean = false, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = if (fill) Arrangement.Top else Arrangement.Center,
    ) {
        Spacer(Modifier.height(if (fill) 8.dp else 0.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = AuraTheme.colors.textPrimary,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(8.dp))
        Text(text = body, style = MaterialTheme.typography.bodyLarge, color = AuraTheme.colors.textSecondary)
        Spacer(Modifier.height(24.dp))
        content()
    }
}

/** Decorative glowing orb with floating glass icon bubbles. */
@Composable
private fun Orb(size: Dp, icons: List<ImageVector>) {
    val colors = AuraTheme.colors
    val transition = rememberInfiniteTransition(label = "orb")
    val float by transition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(2600), RepeatMode.Reverse),
        label = "float",
    )
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            colors.accent.gradientStart.copy(alpha = 0.55f),
                            colors.accent.gradientEnd.copy(alpha = 0.25f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Box(
            Modifier
                .size(size * 0.5f)
                .shadow(24.dp, CircleShape, ambientColor = colors.accent.primary, spotColor = colors.accent.primary)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(colors.accent.gradientStart, colors.accent.gradientEnd))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icons.first(), contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.2f))
        }
        val offsets = listOf(Pair(-0.36f, -0.30f), Pair(0.36f, 0.28f))
        icons.drop(1).take(2).forEachIndexed { index, icon ->
            val (x, y) = offsets[index]
            Box(
                Modifier
                    .offset(x = size * x, y = size * y)
                    .graphicsLayer { translationY = if (index == 0) float * density else -float * density }
                    .size(size * 0.24f)
                    .clip(RoundedCornerShape(30))
                    .background(colors.glassStrong)
                    .border(1.dp, glassBorderBrush(), RoundedCornerShape(30)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = colors.accent.primary, modifier = Modifier.size(size * 0.11f))
            }
        }
    }
}
