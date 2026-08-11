package com.personalagenda.app.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.personalagenda.app.ui.theme.AgendaTheme
import kotlinx.coroutines.delay
import java.time.LocalTime
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val SecondHandRed = Color(0xFFD23B36)

/**
 * An elegant, real-time analog clock drawn with Canvas. Self-contained: it keeps
 * its own per-second tick so the second hand sweeps smoothly, independent of the
 * dashboard's minute-level clock.
 */
@Composable
fun AnalogClock(modifier: Modifier = Modifier, diameter: Dp = 128.dp) {
    val colors = AgendaTheme.colors
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedTicker { now = LocalTime.now() }

    val minorTick = colors.textFaint
    val majorTick = colors.textSecondary
    val handColor = colors.textPrimary

    Canvas(modifier.size(diameter)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = min(cx, cy)

        // Tick marks: 60 minute ticks, with the 12 hour ticks longer/heavier.
        for (i in 0 until 60) {
            val major = i % 5 == 0
            val angle = Math.toRadians(i * 6.0)
            val sinA = sin(angle).toFloat()
            val cosA = cos(angle).toFloat()
            val outer = radius - 1f
            val inner = radius - (if (major) radius * 0.10f else radius * 0.05f)
            drawLine(
                color = if (major) majorTick else minorTick,
                start = Offset(cx + inner * sinA, cy - inner * cosA),
                end = Offset(cx + outer * sinA, cy - outer * cosA),
                strokeWidth = if (major) 1.6f else 0.9f,
                cap = StrokeCap.Round,
            )
        }

        fun hand(fractionOfTurn: Float, length: Float, color: Color, stroke: Float) {
            val angle = Math.toRadians(fractionOfTurn * 360.0)
            val sinA = sin(angle).toFloat()
            val cosA = cos(angle).toFloat()
            drawLine(
                color = color,
                start = Offset(cx, cy),
                end = Offset(cx + length * sinA, cy - length * cosA),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }

        val hour = now.hour % 12
        val minute = now.minute
        val second = now.second

        hand((hour + minute / 60f) / 12f, radius * 0.50f, handColor, 3f)   // hour
        hand((minute + second / 60f) / 60f, radius * 0.72f, handColor, 2f) // minute
        hand(second / 60f, radius * 0.82f, SecondHandRed, 1.3f)            // second

        // Center hub: filled surface with a ring, like the reference dial.
        drawCircle(color = colors.surface, radius = radius * 0.055f, center = Offset(cx, cy))
        drawCircle(
            color = handColor,
            radius = radius * 0.055f,
            center = Offset(cx, cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.6f),
        )
    }
}

/** Runs [onTick] once per second for as long as the composable is present. */
@Composable
private fun LaunchedTicker(onTick: () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            onTick()
            delay(1_000)
        }
    }
}
