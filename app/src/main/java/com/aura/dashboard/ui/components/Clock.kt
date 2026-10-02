package com.aura.dashboard.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.dashboard.ui.theme.AuraTheme
import com.aura.dashboard.ui.util.Formatting
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** The local time, updated at the start of every second. */
@Composable
fun rememberCurrentTime(): LocalTime {
    val time by produceState(LocalTime.now()) {
        while (true) {
            val now = LocalTime.now()
            value = now
            delay(1_000L - now.nano / 1_000_000 + 5)
        }
    }
    return time
}

/** Large digital clock with small seconds, following the system 12/24-hour setting. */
@Composable
fun DigitalClock(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val colors = AuraTheme.colors
    val time = rememberCurrentTime()
    val locale = LocalConfiguration.current.locales[0]
    val is24 = DateFormat.is24HourFormat(context)
    val main = time.format(DateTimeFormatter.ofPattern(if (is24) "HH:mm" else "h:mm", locale))
    val seconds = time.format(DateTimeFormatter.ofPattern("ss"))
    val spoken = Formatting.time(context, time)
    Row(modifier.clearAndSetSemantics { contentDescription = spoken }, verticalAlignment = Alignment.Bottom) {
        Text(
            text = main,
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 46.sp, lineHeight = 50.sp),
            color = colors.textPrimary,
        )
        // Seconds (and AM/PM in 12-hour mode) stack beside the time so "36 AM" never reads as one value.
        Column(Modifier.padding(start = 6.dp, bottom = 8.dp)) {
            if (!is24) {
                Text(
                    text = time.format(DateTimeFormatter.ofPattern("a", locale)),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = colors.accent.primary,
                )
            }
            Text(
                text = ":$seconds",
                style = MaterialTheme.typography.titleMedium,
                color = if (is24) colors.accent.primary else colors.textSecondary,
            )
        }
    }
}

/** Analog clock face drawn in the app's style. */
@Composable
fun AnalogClock(modifier: Modifier = Modifier, size: Dp = 112.dp) {
    val context = LocalContext.current
    val colors = AuraTheme.colors
    val time = rememberCurrentTime()
    val spoken = Formatting.time(context, time)
    Canvas(modifier.size(size).semantics { contentDescription = spoken }) {
        val radius = this.size.minDimension / 2f
        val center = Offset(radius, radius)
        drawCircle(
            Brush.linearGradient(listOf(colors.glassStrong, colors.accent.container.copy(alpha = 0.6f))),
            radius,
            center,
        )
        drawCircle(colors.accent.primary.copy(alpha = 0.25f), radius - 1.dp.toPx(), center, style = Stroke(1.2.dp.toPx()))
        for (i in 0 until 60) {
            val angle = (i * 6f - 90f) * (PI / 180f).toFloat()
            val major = i % 5 == 0
            val outer = radius * 0.9f
            val inner = radius * (if (major) 0.76f else 0.84f)
            drawLine(
                colors.textSecondary.copy(alpha = if (major) 0.8f else 0.3f),
                Offset(center.x + cos(angle) * inner, center.y + sin(angle) * inner),
                Offset(center.x + cos(angle) * outer, center.y + sin(angle) * outer),
                strokeWidth = (if (major) 2.2f else 1f) * density,
                cap = StrokeCap.Round,
            )
        }
        fun hand(degrees: Float, length: Float, width: Float, color: androidx.compose.ui.graphics.Color) {
            val angle = (degrees - 90f) * (PI / 180f).toFloat()
            drawLine(
                color,
                center - Offset(cos(angle), sin(angle)) * (radius * 0.1f),
                Offset(center.x + cos(angle) * length, center.y + sin(angle) * length),
                strokeWidth = width,
                cap = StrokeCap.Round,
            )
        }
        val hours = (time.hour % 12) + time.minute / 60f
        val minutes = time.minute + time.second / 60f
        hand(hours * 30f, radius * 0.48f, 4.5f * density, colors.textPrimary)
        hand(minutes * 6f, radius * 0.7f, 3f * density, colors.textPrimary)
        hand(time.second * 6f, radius * 0.78f, 1.5f * density, colors.accent.primary)
        drawCircle(colors.accent.primary, 4f * density, center)
    }
}
