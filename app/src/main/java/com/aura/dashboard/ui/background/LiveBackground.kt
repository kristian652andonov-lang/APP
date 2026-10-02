package com.aura.dashboard.ui.background

import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import com.aura.dashboard.domain.model.BackgroundEffect
import com.aura.dashboard.ui.theme.AuraColors
import com.aura.dashboard.ui.theme.AuraTheme
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import kotlinx.coroutines.isActive
import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/**
 * Animated scene drawn behind the app. Every particle position is a pure function of time and
 * a per-particle random seed, so nothing is allocated or simulated per frame; only the draw
 * phase is invalidated. Animation pauses with the app and is disabled when the user turned off
 * system animations ("Remove animations"), in which case a still frame is shown.
 */
@Composable
fun LiveBackground(effect: BackgroundEffect, modifier: Modifier = Modifier, animate: Boolean = true) {
    if (effect == BackgroundEffect.NONE || effect == BackgroundEffect.AUTO) return
    val context = LocalContext.current
    val reduceMotion = remember {
        runCatching { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
            .getOrDefault(false)
    }
    val time = rememberSceneTime(animate && !reduceMotion)
    val colors = AuraTheme.colors
    val particles = remember(effect) { particlesFor(effect) }
    val paths = remember { ScenePaths() }

    Canvas(modifier.fillMaxSize().clipToBounds()) {
        // A still frame looks best a few seconds in, once everything has spread out.
        val t = if (animate && !reduceMotion) time.value else STILL_FRAME_TIME
        drawScene(effect, t, particles, colors, paths, allowFlashes = !reduceMotion)
    }
}

private const val STILL_FRAME_TIME = 7.3f

/** Seconds since the scene started, advanced on each frame while visible. */
@Composable
private fun rememberSceneTime(enabled: Boolean): State<Float> {
    val time = remember { mutableFloatStateOf(0f) }
    if (enabled) {
        LaunchedEffect(Unit) {
            var last = -1L
            while (isActive) {
                // Infinite-animation frames pause in tests and while the app is in the background.
                withInfiniteAnimationFrameNanos { now ->
                    if (last >= 0) {
                        // Clamp so returning from the background continues smoothly instead of jumping.
                        val delta = (now - last).coerceIn(0L, MAX_FRAME_NANOS)
                        time.floatValue = (time.floatValue + delta / 1_000_000_000f) % SCENE_WRAP_SECONDS
                    }
                    last = now
                }
            }
        }
    }
    return time
}

private const val MAX_FRAME_NANOS = 100_000_000L

/** Keeps float precision high; a wrap once an hour is imperceptible. */
private const val SCENE_WRAP_SECONDS = 3600f

/** Per-particle random values in [0, 1). */
private class Particles(count: Int, seed: Long) {
    val values: Array<FloatArray> = Random(seed).let { random -> Array(count) { FloatArray(8) { random.nextFloat() } } }
}

private fun particlesFor(effect: BackgroundEffect): Particles = when (effect) {
    BackgroundEffect.SNOW -> Particles(80, 11)
    BackgroundEffect.RAIN -> Particles(95, 12)
    BackgroundEffect.THUNDER -> Particles(120, 13)
    BackgroundEffect.SUNNY -> Particles(22, 14)
    BackgroundEffect.CLOUDS -> Particles(7, 15)
    BackgroundEffect.NIGHT_SKY -> Particles(90, 16)
    BackgroundEffect.SHOOTING_STARS -> Particles(80, 17)
    BackgroundEffect.FLOWING_LINES -> Particles(9, 18)
    BackgroundEffect.AURORA -> Particles(40, 19)
    BackgroundEffect.BUBBLES -> Particles(30, 20)
    BackgroundEffect.FIREFLIES -> Particles(28, 21)
    BackgroundEffect.PETALS -> Particles(30, 22)
    BackgroundEffect.NONE, BackgroundEffect.AUTO -> Particles(0, 0)
}

/** Reusable paths so drawing doesn't allocate every frame. */
private class ScenePaths {
    val a = Path()
    val b = Path()
    val c = Path()
}

private fun DrawScope.drawScene(
    effect: BackgroundEffect,
    t: Float,
    particles: Particles,
    colors: AuraColors,
    paths: ScenePaths,
    allowFlashes: Boolean,
) {
    val p = particles.values
    when (effect) {
        BackgroundEffect.SNOW -> snow(t, p, colors)
        BackgroundEffect.RAIN -> {
            dim(colors, light = 0.05f, dark = 0.12f)
            rain(t, p, colors, heavy = false)
        }
        BackgroundEffect.THUNDER -> {
            dim(colors, light = 0.10f, dark = 0.22f)
            clouds(t, p, colors, paths, stormy = true, count = 6)
            rain(t, p, colors, heavy = true)
            lightning(t, colors, paths, allowFlashes)
        }
        BackgroundEffect.SUNNY -> sunny(t, p, colors, paths)
        BackgroundEffect.CLOUDS -> clouds(t, p, colors, paths, stormy = false)
        BackgroundEffect.NIGHT_SKY -> {
            nightTint(colors)
            stars(t, p, colors)
            moon(colors, paths)
        }
        BackgroundEffect.SHOOTING_STARS -> {
            nightTint(colors)
            stars(t, p, colors)
            shootingStars(t, colors)
        }
        BackgroundEffect.FLOWING_LINES -> flowingLines(t, p, colors, paths)
        BackgroundEffect.AURORA -> {
            nightTint(colors)
            stars(t, p, colors, fraction = 0.5f)
            aurora(t, colors, paths)
        }
        BackgroundEffect.BUBBLES -> bubbles(t, p, colors)
        BackgroundEffect.FIREFLIES -> fireflies(t, p, colors)
        BackgroundEffect.PETALS -> petals(t, p, colors, paths)
        BackgroundEffect.NONE, BackgroundEffect.AUTO -> Unit
    }
}

// --- Shared helpers ---------------------------------------------------------------------------

private fun DrawScope.dim(colors: AuraColors, light: Float, dark: Float) {
    drawRect(if (colors.isDark) Color.Black.copy(alpha = dark) else Color(0xFF3C4466).copy(alpha = light))
}

private fun DrawScope.nightTint(colors: AuraColors) {
    val top = if (colors.isDark) Color(0xFF1B1A3D).copy(alpha = 0.55f) else Color(0xFF7C74C9).copy(alpha = 0.16f)
    drawRect(Brush.verticalGradient(listOf(top, Color.Transparent), endY = size.height * 0.7f))
}

// --- Weather ---------------------------------------------------------------------------------

private fun DrawScope.snow(t: Float, p: Array<FloatArray>, colors: AuraColors) {
    val w = size.width
    val h = size.height
    // On light backgrounds white flakes get a soft cool shadow so they stay visible.
    val halo = if (colors.isDark) Color.White else lerp(colors.accent.gradientEnd, Color(0xFF8E9AC4), 0.55f)
    for (r in p) {
        val depth = 0.35f + r[2] * 0.65f
        val radius = (1.3f + 3.4f * depth) * density
        val speed = (12f + 42f * depth) * density
        val travel = h + radius * 6
        val y = (r[1] * travel + speed * t) % travel - radius * 3
        val sway = (6f + 18f * r[3]) * density
        val x = r[0] * w + sin(t * (0.35f + r[4] * 0.6f) + r[5] * 6.28f) * sway
        val center = Offset(x, y)
        if (colors.isDark) {
            val alpha = 0.85f * (0.4f + 0.6f * depth)
            drawCircle(Color.White.copy(alpha = alpha), radius, center)
            if (depth > 0.85f) drawCircle(halo.copy(alpha = alpha * 0.18f), radius * 2.6f, center)
        } else {
            drawCircle(halo.copy(alpha = 0.16f * depth), radius * 1.6f, center + Offset(0f, radius * 0.25f))
            drawCircle(halo.copy(alpha = 0.30f * depth), radius * 1.12f, center + Offset(0f, radius * 0.3f))
            drawCircle(Color.White.copy(alpha = 0.96f), radius, center)
        }
    }
}

private fun DrawScope.rain(t: Float, p: Array<FloatArray>, colors: AuraColors, heavy: Boolean) {
    val w = size.width
    val h = size.height
    val drop = if (colors.isDark) Color(0xFFB9C6FF) else Color(0xFF5E6E9E)
    val slant = tan(Math.toRadians(if (heavy) 16.0 else 10.0)).toFloat()
    for (r in p) {
        val depth = 0.4f + r[2] * 0.6f
        val length = (12f + 20f * depth) * density * (if (heavy) 1.3f else 1f)
        val speed = (520f + 520f * depth) * density
        val travel = h + length * 2
        val progress = (r[1] * travel + speed * t) % travel
        val y = progress - length
        val x = ((r[0] * (w + h * slant)) - progress * slant) % (w + h * slant)
        val xx = if (x < -h * slant) x + w + h * slant else x
        drawLine(
            color = drop.copy(alpha = (if (colors.isDark) 0.38f else 0.32f) * depth),
            start = Offset(xx, y),
            end = Offset(xx - length * slant, y + length),
            strokeWidth = (0.8f + 1.1f * depth) * density,
            cap = StrokeCap.Round,
        )
    }
}

/** Gentle, infrequent lightning (at most two soft flashes per strike, well under 3 per second). */
private fun DrawScope.lightning(t: Float, colors: AuraColors, paths: ScenePaths, allowFlashes: Boolean) {
    val period = 7.5f
    val cycle = floor(t / period).toInt()
    val local = t - cycle * period
    val random = Random(cycle * 7919L + 17)
    val offset = random.nextFloat() * 3f
    val phase = local - offset
    if (phase !in 0f..0.55f) return
    val flash = when {
        phase < 0.07f -> phase / 0.07f
        phase < 0.15f -> 1f - (phase - 0.07f) / 0.08f
        phase < 0.22f -> 0f
        phase < 0.27f -> 0.7f * (phase - 0.22f) / 0.05f
        else -> 0.7f * (1f - ((phase - 0.27f) / 0.28f).coerceIn(0f, 1f))
    }.coerceIn(0f, 1f)
    if (allowFlashes) drawRect(Color.White.copy(alpha = flash * (if (colors.isDark) 0.22f else 0.30f)))

    // The bolt itself.
    val path = paths.c
    path.reset()
    var x = size.width * (0.15f + random.nextFloat() * 0.7f)
    var y = 0f
    path.moveTo(x, y)
    val end = size.height * (0.35f + random.nextFloat() * 0.25f)
    while (y < end) {
        y += size.height * (0.03f + random.nextFloat() * 0.05f)
        x += (random.nextFloat() - 0.5f) * size.width * 0.12f
        path.lineTo(x, y)
    }
    val boltAlpha = (if (allowFlashes) flash else 0.6f).coerceAtLeast(0f)
    drawPath(path, Color(0xFFFFF2B3).copy(alpha = boltAlpha * 0.35f), style = Stroke(width = 9f * density, cap = StrokeCap.Round))
    drawPath(path, Color.White.copy(alpha = boltAlpha), style = Stroke(width = 2.2f * density, cap = StrokeCap.Round))
}

private fun DrawScope.sunny(t: Float, p: Array<FloatArray>, colors: AuraColors, paths: ScenePaths) {
    val center = Offset(size.width * 0.84f, size.height * 0.08f)
    val warm = Color(0xFFFFD27A)
    val glowAlpha = (if (colors.isDark) 0.28f else 0.55f) * (0.92f + 0.08f * sin(t * 0.8f))
    val glowRadius = size.width * 0.75f
    drawCircle(
        Brush.radialGradient(listOf(warm.copy(alpha = glowAlpha), Color(0xFFFFB46B).copy(alpha = glowAlpha * 0.35f), Color.Transparent), center, glowRadius),
        glowRadius,
        center,
    )
    // Slowly turning rays.
    rotate(t * 3f, center) {
        val rays = 12
        val rayLength = size.width * 1.1f
        val spread = 0.06f
        val path = paths.a
        path.reset()
        for (i in 0 until rays) {
            val angle = (i * 360f / rays) * (PI / 180f).toFloat()
            path.moveTo(center.x, center.y)
            path.lineTo(center.x + cos(angle - spread) * rayLength, center.y + sin(angle - spread) * rayLength)
            path.lineTo(center.x + cos(angle + spread) * rayLength, center.y + sin(angle + spread) * rayLength)
            path.close()
        }
        drawPath(
            path,
            Brush.radialGradient(listOf(warm.copy(alpha = if (colors.isDark) 0.07f else 0.13f), Color.Transparent), center, rayLength),
        )
    }
    drawCircle(warm.copy(alpha = if (colors.isDark) 0.55f else 0.85f), 30f * density, center)
    // Floating light motes.
    for (r in p) {
        val travel = size.height * 1.1f
        val y = size.height - ((r[1] * travel + (6f + 10f * r[2]) * density * t) % travel)
        val x = r[0] * size.width + sin(t * 0.3f + r[3] * 6f) * 14f * density
        val twinkle = 0.5f + 0.5f * sin(t * (0.8f + r[4]) + r[5] * 6f)
        drawCircle(warm.copy(alpha = 0.35f * twinkle), (1.5f + 2.5f * r[2]) * density, Offset(x, y))
    }
}

private fun DrawScope.clouds(
    t: Float,
    p: Array<FloatArray>,
    colors: AuraColors,
    paths: ScenePaths,
    stormy: Boolean,
    count: Int = p.size,
) {
    val base = when {
        stormy && colors.isDark -> Color(0xFF2A2840)
        stormy -> Color(0xFF9BA3BF)
        colors.isDark -> Color(0xFF34314D)
        else -> Color.White
    }
    val shade = when {
        colors.isDark -> Color.Black.copy(alpha = 0.18f)
        stormy -> Color(0xFF5E6684).copy(alpha = 0.25f)
        else -> Color(0xFFC9D2EE).copy(alpha = 0.55f)
    }
    for (index in 0 until minOf(count, p.size)) {
        val r = p[index]
        val scale = (0.7f + r[2] * 0.9f) * size.width / 400f
        val cloudWidth = 160f * density * scale
        val speed = (5f + 9f * r[3]) * density * (if (stormy) 1.8f else 1f)
        val travel = size.width + cloudWidth * 2
        val x = (r[0] * travel + speed * t) % travel - cloudWidth
        val y = size.height * (if (stormy) 0.02f + r[1] * 0.18f else 0.04f + r[1] * 0.42f) + index % 2 * 8f * density
        val alpha = if (colors.isDark) 0.55f else if (stormy) 0.7f else 0.92f
        cloudShape(paths.a, x, y, cloudWidth)
        val shadowOffset = 6f * density
        withTransform({ translate(0f, shadowOffset) }) { drawPath(paths.a, shade) }
        drawPath(paths.a, base.copy(alpha = alpha))
    }
}

private fun cloudShape(path: Path, x: Float, y: Float, width: Float) {
    path.reset()
    val h = width * 0.42f
    path.addOval(androidx.compose.ui.geometry.Rect(x, y + h * 0.35f, x + width, y + h))
    path.addOval(androidx.compose.ui.geometry.Rect(x + width * 0.12f, y + h * 0.1f, x + width * 0.5f, y + h * 0.85f))
    path.addOval(androidx.compose.ui.geometry.Rect(x + width * 0.35f, y - h * 0.15f, x + width * 0.8f, y + h * 0.75f))
    path.addOval(androidx.compose.ui.geometry.Rect(x + width * 0.6f, y + h * 0.2f, x + width * 0.95f, y + h * 0.9f))
}

// --- Night ----------------------------------------------------------------------------------

private fun DrawScope.stars(t: Float, p: Array<FloatArray>, colors: AuraColors, fraction: Float = 1f) {
    val starColor = if (colors.isDark) Color(0xFFFFF8E7) else colors.accent.primary
    val count = (p.size * fraction).toInt()
    for (i in 0 until count) {
        val r = p[i]
        val x = r[0] * size.width
        val y = r[1] * size.height * 0.75f
        val twinkle = 0.35f + 0.65f * (0.5f + 0.5f * sin(t * (0.6f + r[2] * 2.2f) + r[3] * 6.28f))
        val radius = (0.7f + 1.6f * r[4]) * density
        val alpha = (if (colors.isDark) 0.9f else 0.45f) * twinkle
        drawCircle(starColor.copy(alpha = alpha), radius, Offset(x, y))
        if (r[4] > 0.85f) {
            // A few bright stars get a soft cross sparkle.
            val arm = radius * 4f
            drawLine(starColor.copy(alpha = alpha * 0.5f), Offset(x - arm, y), Offset(x + arm, y), strokeWidth = density * 0.8f)
            drawLine(starColor.copy(alpha = alpha * 0.5f), Offset(x, y - arm), Offset(x, y + arm), strokeWidth = density * 0.8f)
        }
    }
}

private fun DrawScope.moon(colors: AuraColors, paths: ScenePaths) {
    val center = Offset(size.width * 0.8f, size.height * 0.11f)
    val radius = 34f * density
    val halo = if (colors.isDark) Color(0xFFFFF1C9) else colors.accent.gradientStart
    drawCircle(
        Brush.radialGradient(listOf(halo.copy(alpha = if (colors.isDark) 0.22f else 0.30f), Color.Transparent), center, radius * 5f),
        radius * 5f,
        center,
    )
    val full = paths.a.apply {
        reset()
        addOval(androidx.compose.ui.geometry.Rect(center, radius))
    }
    val cut = paths.b.apply {
        reset()
        addOval(androidx.compose.ui.geometry.Rect(Offset(center.x + radius * 0.55f, center.y - radius * 0.3f), radius * 0.95f))
    }
    paths.c.reset()
    paths.c.op(full, cut, PathOperation.Difference)
    drawPath(paths.c, if (colors.isDark) Color(0xFFFFF4D6) else Color.White)
}

private fun DrawScope.shootingStars(t: Float, colors: AuraColors) {
    val streakColor = if (colors.isDark) Color.White else colors.accent.primary
    for (slot in 0 until 3) {
        val period = 5f + slot * 1.7f
        val cycle = floor((t + slot * 2.3f) / period).toInt()
        val local = (t + slot * 2.3f) - cycle * period
        val duration = 1.1f
        if (local > duration) continue
        val random = Random(cycle * 31L + slot * 977L)
        val start = Offset(size.width * (0.1f + random.nextFloat() * 0.6f), size.height * random.nextFloat() * 0.35f)
        val angle = (25f + random.nextFloat() * 20f) * (PI / 180f).toFloat()
        val travel = size.width * (0.45f + random.nextFloat() * 0.3f)
        val progress = local / duration
        val head = Offset(start.x + cos(angle) * travel * progress, start.y + sin(angle) * travel * progress)
        val tailLength = travel * 0.35f * (1f - abs(progress - 0.5f))
        val tail = Offset(head.x - cos(angle) * tailLength, head.y - sin(angle) * tailLength)
        val fade = sin(progress * PI.toFloat())
        drawLine(
            Brush.linearGradient(listOf(Color.Transparent, streakColor.copy(alpha = 0.9f * fade)), tail, head),
            tail,
            head,
            strokeWidth = 2f * density,
            cap = StrokeCap.Round,
        )
        drawCircle(streakColor.copy(alpha = fade), 2.2f * density, head)
        drawCircle(streakColor.copy(alpha = 0.25f * fade), 7f * density, head)
    }
}

private fun DrawScope.aurora(t: Float, colors: AuraColors, paths: ScenePaths) {
    val palette = listOf(Color(0xFF5BE3A8), Color(0xFF4CC9F0), Color(0xFF9D7BFF))
    val strength = if (colors.isDark) 0.30f else 0.18f
    val feather = 6f * density
    for ((i, color) in palette.withIndex()) {
        // Each band is a curtain: brightest along its wavy lower hem and fading out upwards.
        val baseY = size.height * (0.20f + i * 0.075f)
        val amplitude = size.height * 0.035f
        val height = size.height * (0.15f + i * 0.02f)
        val frequency = 6.28f * (0.9f + i * 0.25f)
        val speed = 0.22f + i * 0.07f
        val shimmer = 0.75f + 0.25f * sin(t * 0.7f + i * 1.7f)
        val brush = Brush.verticalGradient(
            listOf(Color.Transparent, color.copy(alpha = strength * shimmer)),
            startY = baseY + amplitude - height,
            endY = baseY - amplitude,
        )
        // Stacked, slightly longer copies feather the hem instead of ending in a hard edge.
        for (layer in 0 until AURORA_LAYERS) {
            val path = paths.a
            path.reset()
            val steps = 40
            for (s in 0..steps) {
                val x = size.width * s / steps
                val y = baseY + sin(x / size.width * frequency + t * speed) * amplitude - height
                if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            for (s in steps downTo 0) {
                val x = size.width * s / steps
                val wave = sin(x / size.width * frequency + t * speed) * amplitude
                // A slower second wave makes the hem ripple independently of the top.
                val ripple = sin(x / size.width * frequency * 2.3f - t * speed * 1.6f) * amplitude * 0.35f
                path.lineTo(x, baseY + wave + ripple + layer * feather)
            }
            path.close()
            drawPath(path, brush, alpha = AURORA_LAYER_ALPHA)
        }
    }
}

private const val AURORA_LAYERS = 6

/** Six overlapping layers at this alpha add up to ~90% in the body and taper towards the hem. */
private const val AURORA_LAYER_ALPHA = 0.33f

// --- Playful ---------------------------------------------------------------------------------

private fun DrawScope.flowingLines(t: Float, p: Array<FloatArray>, colors: AuraColors, paths: ScenePaths) {
    val steps = 48
    for ((i, r) in p.withIndex()) {
        val path = paths.a
        path.reset()
        val baseY = size.height * (0.18f + i * 0.085f)
        val amplitude = (18f + 34f * r[0]) * density
        val waves = 0.8f + r[1] * 1.1f
        val speed = 0.25f + r[2] * 0.35f
        for (s in 0..steps) {
            val x = size.width * s / steps
            val y = baseY +
                sin(x / size.width * 6.28f * waves + t * speed + r[3] * 6.28f) * amplitude +
                sin(x / size.width * 6.28f * waves * 0.5f - t * speed * 0.6f) * amplitude * 0.35f
            if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        val alpha = (if (colors.isDark) 0.30f else 0.34f) * (0.55f + 0.45f * r[4])
        drawPath(
            path,
            Brush.horizontalGradient(
                listOf(colors.accent.gradientStart.copy(alpha = 0f), colors.accent.gradientStart.copy(alpha = alpha), colors.accent.gradientEnd.copy(alpha = alpha), colors.accent.gradientEnd.copy(alpha = 0f)),
            ),
            style = Stroke(width = (1.2f + 1.6f * r[5]) * density, cap = StrokeCap.Round),
        )
    }
}

private fun DrawScope.bubbles(t: Float, p: Array<FloatArray>, colors: AuraColors) {
    val rim = if (colors.isDark) Color.White else colors.accent.primary
    for (r in p) {
        val radius = (6f + 22f * r[2]) * density
        val speed = (14f + 30f * (1f - r[2])) * density
        val travel = size.height + radius * 4
        val y = size.height + radius * 2 - ((r[1] * travel + speed * t) % travel)
        val x = r[0] * size.width + sin(t * (0.6f + r[3]) + r[4] * 6.28f) * 12f * density
        val center = Offset(x, y)
        drawCircle(rim.copy(alpha = 0.05f), radius, center)
        drawCircle(rim.copy(alpha = if (colors.isDark) 0.28f else 0.22f), radius, center, style = Stroke(width = 1.1f * density))
        drawCircle(Color.White.copy(alpha = if (colors.isDark) 0.45f else 0.8f), radius * 0.22f, Offset(x - radius * 0.38f, y - radius * 0.38f))
    }
}

private fun DrawScope.fireflies(t: Float, p: Array<FloatArray>, colors: AuraColors) {
    val glow = if (colors.isDark) Color(0xFFE8FF8A) else Color(0xFFF2B33D)
    for (r in p) {
        val cx = r[0] * size.width
        val cy = size.height * (0.15f + r[1] * 0.8f)
        val x = cx + sin(t * (0.15f + r[2] * 0.25f) + r[3] * 6.28f) * 40f * density
        val y = cy + cos(t * (0.12f + r[4] * 0.2f) + r[5] * 6.28f) * 30f * density
        val pulse = (0.5f + 0.5f * sin(t * (1f + r[6] * 1.5f) + r[7] * 6.28f)).let { it * it }
        val radius = (10f + 8f * r[2]) * density
        val center = Offset(x, y)
        drawCircle(
            Brush.radialGradient(listOf(glow.copy(alpha = (if (colors.isDark) 0.55f else 0.4f) * pulse), Color.Transparent), center, radius),
            radius,
            center,
        )
        drawCircle(glow.copy(alpha = 0.9f * pulse), 1.8f * density, center)
    }
}

private fun DrawScope.petals(t: Float, p: Array<FloatArray>, colors: AuraColors, paths: ScenePaths) {
    val tones = listOf(Color(0xFFFFB7C9), Color(0xFFFF9BB5), Color(0xFFFFD1DC))
    for (r in p) {
        val size2 = (6f + 7f * r[2]) * density
        val speed = (22f + 30f * r[3]) * density
        val travel = size.height + size2 * 6
        val progress = (r[1] * travel + speed * t) % travel
        val y = progress - size2 * 3
        val x = (r[0] * size.width + progress * 0.25f + sin(t * 0.7f + r[4] * 6.28f) * 20f * density) % (size.width + size2 * 2)
        val angle = t * (40f + 80f * r[5]) + r[6] * 360f
        val petal = paths.a
        petal.reset()
        petal.moveTo(x, y - size2)
        petal.cubicTo(x + size2, y - size2 * 0.6f, x + size2 * 0.7f, y + size2 * 0.7f, x, y + size2)
        petal.cubicTo(x - size2 * 0.7f, y + size2 * 0.7f, x - size2, y - size2 * 0.6f, x, y - size2)
        petal.close()
        val tone = tones[(r[7] * tones.size).toInt().coerceIn(0, tones.lastIndex)]
        rotate(angle, Offset(x, y)) {
            drawPath(petal, tone.copy(alpha = if (colors.isDark) 0.6f else 0.85f))
        }
    }
}
