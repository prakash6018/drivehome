package com.drivehome.launcher.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.drivehome.launcher.ui.theme.LocalPalette
import com.drivehome.launcher.ui.theme.Orbitron
import com.drivehome.launcher.ui.theme.Redline
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private const val MAX_KMH = 180f
private const val REDLINE_KMH = 140f
private const val START_ANGLE = 135f
private const val SWEEP = 270f

/**
 * Cluster-style speedometer. [selfTest] sweeps the needle to the top and back, the way a
 * real cluster does when the ignition comes on (the head unit boots with the ignition).
 */
@Composable
fun SpeedGauge(
    kmh: Int?,
    status: String,
    selfTest: Boolean,
    onSelfTestDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = LocalPalette.current
    val measurer = rememberTextMeasurer()
    val shown = remember { Animatable(0f) }
    var testing by remember { mutableStateOf(selfTest) }

    LaunchedEffect(Unit) {
        if (testing) {
            shown.animateTo(MAX_KMH, tween(900, easing = FastOutSlowInEasing))
            shown.animateTo(0f, tween(700, easing = FastOutSlowInEasing))
            onSelfTestDone()
            testing = false
        }
    }
    LaunchedEffect(kmh, testing) {
        if (!testing) shown.animateTo((kmh ?: 0).toFloat().coerceIn(0f, MAX_KMH), tween(500))
    }

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val d = min(maxWidth.value, maxHeight.value).dp
        val density = LocalDensity.current
        Canvas(Modifier.size(d)) {
            val stroke = size.minDimension * 0.055f
            val radius = size.minDimension / 2f - stroke * 1.4f
            val c = Offset(size.width / 2f, size.height / 2f)
            val arcTopLeft = Offset(c.x - radius, c.y - radius)
            val arcSize = Size(radius * 2, radius * 2)
            val value = shown.value
            val valueSweep = SWEEP * value / MAX_KMH
            val redStart = SWEEP * REDLINE_KMH / MAX_KMH

            // Track, then a dim redline zone
            drawArc(p.surfaceHigh, START_ANGLE, SWEEP, false, arcTopLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(Redline.copy(alpha = 0.35f), START_ANGLE + redStart, SWEEP - redStart, false, arcTopLeft, arcSize, style = Stroke(stroke * 0.35f))

            // Value arc: wide translucent pass for glow, then the solid arc
            if (valueSweep > 0.5f) {
                val arcColor = if (value >= REDLINE_KMH) Redline else p.accent
                drawArc(arcColor.copy(alpha = 0.22f), START_ANGLE, valueSweep, false, arcTopLeft, arcSize, style = Stroke(stroke * 2.6f, cap = StrokeCap.Round))
                drawArc(
                    Brush.sweepGradient(listOf(arcColor.copy(alpha = 0.5f), arcColor), center = c),
                    START_ANGLE, valueSweep, false, arcTopLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round),
                )
                // Bright "comet head" at the tip
                val tipRad = Math.toRadians((START_ANGLE + valueSweep).toDouble())
                val tip = Offset(c.x + radius * cos(tipRad).toFloat(), c.y + radius * sin(tipRad).toFloat())
                drawCircle(Color.White, radius = stroke * 0.42f, center = tip)
            }

            // Ticks every 10 km/h, numbers every 20
            val labelStyle = TextStyle(
                fontFamily = Orbitron, fontWeight = FontWeight.Bold,
                fontSize = with(density) { (size.minDimension * 0.045f).toSp() },
                color = p.textDim,
            )
            for (k in 0..MAX_KMH.toInt() step 10) {
                val major = k % 20 == 0
                val a = Math.toRadians((START_ANGLE + SWEEP * k / MAX_KMH).toDouble())
                val dir = Offset(cos(a).toFloat(), sin(a).toFloat())
                val outer = radius - stroke * 1.2f
                val inner = outer - if (major) stroke * 0.9f else stroke * 0.45f
                val tickColor = when {
                    k >= REDLINE_KMH -> Redline
                    k <= value -> p.text
                    else -> p.textDim.copy(alpha = 0.6f)
                }
                drawLine(tickColor, c + dir * inner, c + dir * outer, strokeWidth = if (major) stroke * 0.18f else stroke * 0.1f)
                if (major) {
                    val text = measurer.measure(k.toString(), labelStyle)
                    val at = c + dir * (inner - stroke * 1.1f)
                    drawText(text, topLeft = Offset(at.x - text.size.width / 2f, at.y - text.size.height / 2f))
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            val numberSize = with(density) { (d * 0.2f).toSp() }
            Text(
                if (testing || kmh != null) shown.value.roundToInt().toString() else "--",
                color = p.text,
                style = TextStyle(fontFamily = Orbitron, fontWeight = FontWeight.Black, fontSize = numberSize, fontFeatureSettings = "tnum"),
            )
            Text("KM/H", color = p.textDim, style = labelStyleFor(d.value * 0.05f))
        }
        // Status sits in the open gap at the bottom of the dial, like a cluster's warning area.
        Text(
            status.uppercase(),
            Modifier.align(Alignment.BottomCenter).padding(bottom = (maxHeight - d) / 2 + d * 0.08f),
            color = if (kmh != null) p.accent else p.textDim,
            style = labelStyleFor(d.value * 0.034f),
        )
    }
}

@Composable
private fun labelStyleFor(dpSize: Float): TextStyle {
    val sp = with(LocalDensity.current) { dpSize.dp.toSp() }
    return TextStyle(fontFamily = Orbitron, fontWeight = FontWeight.Bold, fontSize = sp, letterSpacing = sp * 0.15f)
}
