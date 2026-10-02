package com.aura.dashboard.ui.weather

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aura.dashboard.domain.model.WeatherCondition
import com.aura.dashboard.ui.theme.AuraTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val SunCore = Color(0xFFFFC94D)
private val SunEdge = Color(0xFFFF9F43)
private val RainBlue = Color(0xFF5A9BFF)
private val BoltYellow = Color(0xFFFFD54A)

/** Animated, hand-drawn weather icon (sun rays turn, rain falls, snow drifts). */
@Composable
fun WeatherIcon(
    condition: WeatherCondition,
    isDay: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    animate: Boolean = true,
    contentDescription: String? = null,
) {
    val colors = AuraTheme.colors
    val transition = rememberInfiniteTransition(label = "weatherIcon")
    val spin by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(24_000, easing = LinearEasing)), label = "spin")
    val fall by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1_100, easing = LinearEasing)), label = "fall")
    val bob by transition.animateFloat(-1f, 1f, infiniteRepeatable(tween(2_600), RepeatMode.Reverse), label = "bob")
    val cloudPath = remember { Path() }
    val moonPaths = remember { Triple(Path(), Path(), Path()) }
    val cloudColor = if (colors.isDark) Color(0xFFD5DAEE) else Color(0xFFF2F4FC)
    val cloudShade = if (colors.isDark) Color(0xFF8E95B5) else Color(0xFFA9B4DA)
    val moonColor = if (colors.isDark) Color(0xFFF6EBC8) else Color(0xFF8C88D6)

    Canvas(
        modifier
            .size(size)
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier),
    ) {
        val s = this.size.minDimension
        val rotation = if (animate) spin else 0f
        val drop = if (animate) fall else 0.5f
        val float = if (animate) bob else 0f
        when (condition) {
            WeatherCondition.CLEAR ->
                if (isDay) sun(Offset(s * 0.5f, s * 0.5f), s * 0.22f, rotation) else moon(Offset(s * 0.5f, s * 0.5f), s * 0.28f, moonColor, moonPaths)
            WeatherCondition.PARTLY_CLOUDY -> {
                if (isDay) sun(Offset(s * 0.36f, s * 0.36f), s * 0.17f, rotation) else moon(Offset(s * 0.38f, s * 0.36f), s * 0.2f, moonColor, moonPaths)
                cloud(cloudPath, s, Offset(s * 0.08f, s * 0.36f + float * s * 0.015f), s * 0.84f, cloudColor, cloudShade)
            }
            WeatherCondition.CLOUDY -> {
                cloud(cloudPath, s, Offset(s * 0.26f, s * 0.16f), s * 0.62f, cloudShade.copy(alpha = 0.9f), cloudShade.copy(alpha = 0.5f))
                cloud(cloudPath, s, Offset(s * 0.06f, s * 0.34f + float * s * 0.015f), s * 0.86f, cloudColor, cloudShade)
            }
            WeatherCondition.FOG -> {
                cloud(cloudPath, s, Offset(s * 0.08f, s * 0.16f), s * 0.84f, cloudColor, cloudShade)
                for (i in 0..2) {
                    val y = s * (0.70f + i * 0.1f)
                    val shift = float * s * 0.03f * (if (i % 2 == 0) 1 else -1)
                    drawLine(cloudShade, Offset(s * 0.18f + shift, y), Offset(s * 0.82f + shift, y), strokeWidth = s * 0.05f, cap = StrokeCap.Round)
                }
            }
            WeatherCondition.DRIZZLE, WeatherCondition.RAIN, WeatherCondition.FREEZING_RAIN -> {
                cloud(cloudPath, s, Offset(s * 0.08f, s * 0.12f), s * 0.84f, cloudColor, cloudShade)
                val heavy = condition == WeatherCondition.RAIN
                val color = if (condition == WeatherCondition.FREEZING_RAIN) Color(0xFF8FD3FF) else RainBlue
                for (i in 0..2) {
                    val x = s * (0.32f + i * 0.18f)
                    val progress = (drop + i * 0.33f) % 1f
                    val y = s * (0.62f + progress * 0.26f)
                    val length = s * (if (heavy) 0.12f else 0.06f)
                    drawLine(
                        color.copy(alpha = 1f - progress * 0.6f),
                        Offset(x, y),
                        Offset(x - length * 0.35f, y + length),
                        strokeWidth = s * 0.045f,
                        cap = StrokeCap.Round,
                    )
                }
            }
            WeatherCondition.SNOW -> {
                cloud(cloudPath, s, Offset(s * 0.08f, s * 0.12f), s * 0.84f, cloudColor, cloudShade)
                for (i in 0..2) {
                    val progress = (drop * 0.6f + i * 0.33f) % 1f
                    val center = Offset(s * (0.32f + i * 0.18f) + sin(progress * 6.28f) * s * 0.03f, s * (0.64f + progress * 0.26f))
                    flake(center, s * 0.045f, if (colors.isDark) Color.White else Color(0xFF8FB6F0), 1f - progress * 0.5f)
                }
            }
            WeatherCondition.THUNDERSTORM -> {
                cloud(cloudPath, s, Offset(s * 0.08f, s * 0.1f), s * 0.84f, cloudShade, cloudShade.copy(alpha = 0.6f))
                val flicker = if (animate) (0.65f + 0.35f * sin(drop * 6.28f * 2)) else 1f
                val bolt = Path().apply {
                    moveTo(s * 0.54f, s * 0.56f)
                    lineTo(s * 0.40f, s * 0.76f)
                    lineTo(s * 0.51f, s * 0.76f)
                    lineTo(s * 0.44f, s * 0.95f)
                    lineTo(s * 0.64f, s * 0.70f)
                    lineTo(s * 0.53f, s * 0.70f)
                    lineTo(s * 0.60f, s * 0.56f)
                    close()
                }
                drawPath(bolt, BoltYellow.copy(alpha = flicker))
            }
        }
    }
}

private fun DrawScope.sun(center: Offset, radius: Float, rotation: Float) {
    drawCircle(Brush.radialGradient(listOf(SunCore.copy(alpha = 0.35f), Color.Transparent), center, radius * 2.2f), radius * 2.2f, center)
    rotate(rotation, center) {
        for (i in 0 until 8) {
            val angle = (i * 45f) * (PI / 180f).toFloat()
            val start = Offset(center.x + cos(angle) * radius * 1.35f, center.y + sin(angle) * radius * 1.35f)
            val end = Offset(center.x + cos(angle) * radius * 1.8f, center.y + sin(angle) * radius * 1.8f)
            drawLine(SunEdge, start, end, strokeWidth = radius * 0.28f, cap = StrokeCap.Round)
        }
    }
    drawCircle(Brush.linearGradient(listOf(SunCore, SunEdge), center - Offset(radius, radius), center + Offset(radius, radius)), radius, center)
}

private fun DrawScope.moon(center: Offset, radius: Float, color: Color, paths: Triple<Path, Path, Path>) {
    drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.3f), Color.Transparent), center, radius * 2f), radius * 2f, center)
    val (full, cut, result) = paths
    full.reset()
    full.addOval(Rect(center, radius))
    cut.reset()
    cut.addOval(Rect(Offset(center.x + radius * 0.6f, center.y - radius * 0.35f), radius * 0.9f))
    result.reset()
    result.op(full, cut, PathOperation.Difference)
    drawPath(result, color)
}

private fun DrawScope.cloud(path: Path, s: Float, topLeft: Offset, width: Float, color: Color, shade: Color) {
    val h = width * 0.55f
    path.reset()
    path.addRoundRect(androidx.compose.ui.geometry.RoundRect(Rect(topLeft.x, topLeft.y + h * 0.45f, topLeft.x + width, topLeft.y + h), h * 0.3f, h * 0.3f))
    path.addOval(Rect(Offset(topLeft.x + width * 0.32f, topLeft.y + h * 0.42f), width * 0.2f))
    path.addOval(Rect(Offset(topLeft.x + width * 0.58f, topLeft.y + h * 0.36f), width * 0.26f))
    drawPath(path, shade, alpha = 0.8f, style = Stroke(width = s * 0.03f))
    drawPath(path, color)
}

private fun DrawScope.flake(center: Offset, radius: Float, color: Color, alpha: Float) {
    for (i in 0 until 3) {
        val angle = (i * 60f) * (PI / 180f).toFloat()
        val dx = cos(angle) * radius
        val dy = sin(angle) * radius
        drawLine(color.copy(alpha = alpha), center - Offset(dx, dy), center + Offset(dx, dy), strokeWidth = radius * 0.45f, cap = StrokeCap.Round)
    }
}
