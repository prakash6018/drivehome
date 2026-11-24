package com.drivehome.launcher.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drivehome.launcher.R
import com.drivehome.launcher.ui.theme.LocalPalette
import com.drivehome.launcher.ui.theme.Orbitron
import com.drivehome.launcher.ui.theme.labelStyle

/** Driving-UX minimum touch target (Android for Cars guideline). */
val MinTouch = 76.dp

val PanelShape = RoundedCornerShape(26.dp)
val ControlShape = RoundedCornerShape(18.dp)

/** Elevation scale: how opaque each glass layer is over the ambient backdrop. */
object Glass {
    const val WIDGET = 0.66f
    const val CONTROL = 0.85f
}

/**
 * Frosted glass widget (elevation level 1). The backdrop behind is already blurred, so a
 * translucent fill plus a light top edge reads as glass without a per-widget blur pass.
 */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val p = LocalPalette.current
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "press")

    var m = modifier
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clip(PanelShape)
        .background(p.surface.copy(alpha = Glass.WIDGET))
        .background(Brush.verticalGradient(0f to Color.White.copy(alpha = 0.06f), 0.35f to Color.Transparent))
        .border(
            1.dp,
            Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.03f))),
            PanelShape,
        )
    if (onClick != null || onLongClick != null) {
        m = m.combinedClickable(
            interactionSource = interaction,
            indication = ripple(color = p.accent),
            onClick = { onClick?.invoke() },
            onLongClick = onLongClick?.let { action ->
                { haptics.performHapticFeedback(HapticFeedbackType.LongPress); action() }
            },
        )
    }
    Box(m, content = content)
}

/** Small caps widget title with an optional leading glyph, e.g. "▶ NOW PLAYING". */
@Composable
fun WidgetLabel(text: String, @DrawableRes icon: Int? = null, color: Color = LocalPalette.current.textDim) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(painterResource(icon), null, Modifier.size(16.dp), tint = color)
            Box(Modifier.size(6.dp))
        }
        Text(text.uppercase(), color = color, style = labelStyle(11.sp))
    }
}

/**
 * Entrance for the signature load sequence: fade + rise, staggered by [index].
 * Plays only when [play] is true, so returning home doesn't replay it.
 */
@Composable
fun Reveal(index: Int, play: Boolean, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val progress = remember { Animatable(if (play) 0f else 1f) }
    val rise = with(LocalDensity.current) { 28.dp.toPx() }
    LaunchedEffect(Unit) {
        if (play) progress.animateTo(1f, tween(420, delayMillis = 60 + index * 55, easing = FastOutSlowInEasing))
    }
    Box(
        modifier.graphicsLayer {
            alpha = progress.value
            translationY = (1f - progress.value) * rise
        },
        content = content,
    )
}

/** Two slanted racing stripes: a small livery mark for car-related widgets and headers. */
@Composable
fun LiveryStripes(modifier: Modifier = Modifier, color: Color = LocalPalette.current.accent) {
    Canvas(modifier.size(30.dp, 12.dp)) {
        val w = size.width
        val h = size.height
        val bar = w * 0.28f
        val slant = h * 0.8f
        for ((i, alpha) in listOf(1f, 0.5f).withIndex()) {
            val x = i * (bar + w * 0.14f)
            val path = Path().apply {
                moveTo(x + slant, 0f)
                lineTo(x + slant + bar, 0f)
                lineTo(x + bar, h)
                lineTo(x, h)
                close()
            }
            drawPath(path, color.copy(alpha = alpha))
        }
    }
}

@Composable
fun RoundButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    size: Dp,
    background: Color,
    tint: Color,
    glow: Color? = null,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(size)
            .drawBehind {
                // Soft halo behind the hero button, drawn outside the clip.
                if (glow != null) {
                    drawCircle(
                        Brush.radialGradient(listOf(glow.copy(alpha = 0.45f), Color.Transparent), radius = this.size.minDimension * 0.75f),
                        radius = this.size.minDimension * 0.75f,
                    )
                }
            }
            .clip(CircleShape)
            .background(background)
            .border(1.dp, Color.White.copy(alpha = if (glow != null) 0.25f else 0.08f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription, Modifier.size(size * 0.44f), tint = tint)
    }
}

@Composable
fun PillButton(
    label: String,
    primary: Boolean = false,
    @DrawableRes icon: Int? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val p = LocalPalette.current
    val fg = if (primary) p.onAccent else p.text
    Row(
        modifier
            .heightIn(min = 64.dp)
            .widthIn(min = 120.dp)
            .clip(ControlShape)
            .background(if (primary) p.accent else p.surfaceHigh.copy(alpha = Glass.CONTROL))
            .border(1.dp, Color.White.copy(alpha = if (primary) 0f else 0.08f), ControlShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(painterResource(icon), null, Modifier.size(22.dp), tint = if (primary) fg else p.accent)
            Box(Modifier.size(10.dp))
        }
        Text(
            label.uppercase(),
            color = fg,
            fontFamily = Orbitron,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            letterSpacing = 1.5.sp,
            maxLines = 1,
        )
    }
}

@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier
            .clip(ControlShape)
            .background(p.surfaceHigh.copy(alpha = Glass.CONTROL))
            .border(1.dp, Color.White.copy(alpha = 0.08f), ControlShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier
                    .heightIn(min = 68.dp)
                    .widthIn(min = 120.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (on) p.accent else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label.uppercase(),
                    color = if (on) p.onAccent else p.text,
                    fontFamily = Orbitron,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.5.sp,
                )
            }
        }
    }
}

/** Screen header used by the full-screen pages: back button, title, livery stripes. */
@Composable
fun ScreenHeader(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    val p = LocalPalette.current
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        RoundButton(R.drawable.ic_back, "Back", MinTouch, p.surfaceHigh.copy(alpha = Glass.CONTROL), p.text, onClick = onBack)
        Box(Modifier.padding(start = 20.dp, end = 12.dp)) {
            Text(
                title.uppercase(),
                color = p.text,
                fontFamily = Orbitron,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                letterSpacing = 3.sp,
            )
        }
        LiveryStripes()
        Box(Modifier.weight(1f))
        trailing()
    }
}
