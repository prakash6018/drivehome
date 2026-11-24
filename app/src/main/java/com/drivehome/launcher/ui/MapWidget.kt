package com.drivehome.launcher.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drivehome.launcher.R
import com.drivehome.launcher.data.Place
import com.drivehome.launcher.ui.theme.LocalPalette
import kotlin.random.Random

// Where the "you are here" puck sits, as a fraction of the widget; the route leaves from here.
private const val PUCK_X = 0.46f
private const val PUCK_Y = 0.40f

/**
 * Navigation card. There's no map SDK on the head unit, so the map is a stylised night-mode
 * street plan; the useful parts are the area name and one-tap Home / Work routes.
 */
@Composable
fun MapWidget(
    modifier: Modifier,
    place: Place?,
    hasLocation: Boolean,
    onOpen: () -> Unit,
    onHome: () -> Unit,
    onWork: () -> Unit,
) {
    val p = LocalPalette.current
    Panel(modifier, onClick = onOpen) {
        StreetPlan(Modifier.fillMaxSize())
        LocationPuck(Modifier.fillMaxSize())

        Row(
            Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(p.background.copy(alpha = 0.85f), Color.Transparent)))
                .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WidgetLabel("Navigate", R.drawable.ic_navigation, p.accent)
        }

        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, p.background.copy(alpha = 0.92f))))
                .padding(start = 18.dp, end = 18.dp, bottom = 18.dp, top = 36.dp),
        ) {
            WidgetLabel("You are in", R.drawable.ic_place)
            Text(
                place?.area ?: if (hasLocation) "Locating…" else "Location off",
                color = p.text,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                place?.city ?: "Tap the map to open navigation",
                color = p.textDim,
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PillButton("Home", icon = R.drawable.ic_home, modifier = Modifier.weight(1f), onClick = onHome)
                PillButton("Work", icon = R.drawable.ic_work, modifier = Modifier.weight(1f), onClick = onWork)
            }
        }
    }
}

/** Static layer: never reads animated state, so it is drawn once per size change. */
@Composable
private fun StreetPlan(modifier: Modifier) {
    val p = LocalPalette.current
    val land = lerp(p.background, Color(0xFF1B2433), 0.55f)
    val water = Color(0xFF123049)
    val park = Color(0xFF12291D)
    val minor = Color.White.copy(alpha = 0.06f)
    val major = Color.White.copy(alpha = 0.16f)

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val rnd = Random(7)
        drawRect(land)

        val river = Path().apply {
            moveTo(-40f, h * 0.22f)
            cubicTo(w * 0.3f, h * 0.05f, w * 0.55f, h * 0.62f, w + 40f, h * 0.3f)
        }
        drawPath(river, water, style = Stroke(w * 0.07f, cap = StrokeCap.Round))

        repeat(4) {
            drawRoundRect(
                park,
                topLeft = Offset(rnd.nextFloat() * w * 0.85f, rnd.nextFloat() * h * 0.9f),
                size = Size(w * (0.1f + rnd.nextFloat() * 0.12f), h * (0.06f + rnd.nextFloat() * 0.08f)),
                cornerRadius = CornerRadius(14f),
            )
        }

        // Slightly skewed street grid, like an old city centre
        for (i in -2..12) {
            val x = i * w / 9f + rnd.nextFloat() * 20f
            drawLine(minor, Offset(x, 0f), Offset(x + w * 0.12f, h), strokeWidth = 3.dp.toPx())
        }
        for (j in 0..12) {
            val y = j * h / 11f + rnd.nextFloat() * 20f
            drawLine(minor, Offset(0f, y), Offset(w, y - h * 0.06f), strokeWidth = 3.dp.toPx())
        }

        val puck = Offset(w * PUCK_X, h * PUCK_Y)
        drawLine(major, Offset(-20f, h * 0.52f), Offset(w + 20f, h * 0.30f), strokeWidth = 9.dp.toPx(), cap = StrokeCap.Round)
        drawLine(major, Offset(w * 0.34f, h + 20f), Offset(w * 0.6f, -20f), strokeWidth = 7.dp.toPx(), cap = StrokeCap.Round)

        // Suggested route, with a soft glow under it
        val route = Path().apply {
            moveTo(puck.x, puck.y)
            lineTo(w * 0.52f, h * 0.27f)
            lineTo(w * 0.80f, h * 0.20f)
            lineTo(w * 0.86f, -20f)
        }
        drawPath(route, p.accent.copy(alpha = 0.25f), style = Stroke(16.dp.toPx(), cap = StrokeCap.Round))
        drawPath(route, p.accent, style = Stroke(6.dp.toPx(), cap = StrokeCap.Round))
    }
}

/** Animated layer: only a few circles redraw each frame, not the whole street plan. */
@Composable
private fun LocationPuck(modifier: Modifier) {
    val p = LocalPalette.current
    val pulse by rememberInfiniteTransition(label = "puck").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "pulse",
    )
    Canvas(modifier) {
        val c = Offset(size.width * PUCK_X, size.height * PUCK_Y)
        drawCircle(p.accent.copy(alpha = 0.4f * (1f - pulse)), radius = 14.dp.toPx() + 34.dp.toPx() * pulse, center = c)
        drawCircle(Color.White, radius = 13.dp.toPx(), center = c)
        drawCircle(p.accent, radius = 9.dp.toPx(), center = c)
    }
}
