package com.drivehome.launcher.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drivehome.launcher.R
import com.drivehome.launcher.data.TileModel
import com.drivehome.launcher.ui.theme.LocalPalette
import com.drivehome.launcher.ui.theme.Orbitron
import com.drivehome.launcher.ui.theme.labelStyle
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val DockShape = RoundedCornerShape(30.dp)
private val IconShape = RoundedCornerShape(16.dp)

/** CarPlay-style side dock: clock on top, the four app slots, then Apps and Settings. */
@Composable
fun Dock(
    modifier: Modifier,
    now: LocalDateTime,
    use24h: Boolean,
    tiles: List<TileModel>,
    onTile: (Int) -> Unit,
    onTileLongPress: (Int) -> Unit,
    onApps: () -> Unit,
    onSettings: () -> Unit,
) {
    val p = LocalPalette.current
    Column(
        modifier
            .clip(DockShape)
            .background(p.surface.copy(alpha = 0.78f))
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.03f))), DockShape)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            now.format(DateTimeFormatter.ofPattern(if (use24h) "HH:mm" else "h:mm")),
            color = p.text,
            style = TextStyle(fontFamily = Orbitron, fontWeight = FontWeight.Black, fontSize = 24.sp, fontFeatureSettings = "tnum"),
        )
        Text(
            if (use24h) now.format(DateTimeFormatter.ofPattern("EEE")).uppercase() else if (now.hour < 12) "AM" else "PM",
            color = p.textDim,
            style = labelStyle(10.sp),
        )
        DockDivider()
        tiles.forEachIndexed { i, tile ->
            DockItem(
                Modifier.weight(1f),
                icon = tile.app?.icon,
                glyph = tile.role.icon,
                label = tile.app?.label ?: tile.role.label,
                dim = tile.app == null,
                onClick = { onTile(i) },
                onLongClick = { onTileLongPress(i) },
            )
        }
        DockDivider()
        DockItem(Modifier.weight(1f), null, R.drawable.ic_apps, "Apps", onClick = onApps)
        DockItem(Modifier.weight(1f), null, R.drawable.ic_settings, "Settings", onClick = onSettings)
    }
}

@Composable
private fun DockDivider() {
    Box(
        Modifier
            .padding(vertical = 8.dp)
            .width(44.dp)
            .height(1.dp)
            .background(LocalPalette.current.outline)
    )
}

@Composable
private fun DockItem(
    modifier: Modifier,
    icon: ImageBitmap?,
    @DrawableRes glyph: Int,
    label: String,
    dim: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val p = LocalPalette.current
    val haptics = LocalHapticFeedback.current
    // The whole slot is the touch target, so it stays large even when the icon shrinks to fit.
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = p.accent),
                onClick = onClick,
                onLongClick = onLongClick?.let { action ->
                    { haptics.performHapticFeedback(HapticFeedbackType.LongPress); action() }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        val iconSize = (maxHeight - 26.dp).coerceAtMost(56.dp)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(
                Modifier
                    .size(iconSize)
                    .clip(IconShape)
                    .then(if (icon == null) Modifier.background(Brush.linearGradient(listOf(p.surfaceHigh, p.background))) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                if (icon != null) {
                    Image(icon, null, Modifier.fillMaxSize())
                } else {
                    Icon(painterResource(glyph), null, Modifier.size(iconSize * 0.5f), tint = if (dim) p.textDim else p.accent)
                }
            }
            Text(
                label,
                Modifier.padding(top = 4.dp, start = 6.dp, end = 6.dp),
                color = if (dim) p.textDim else p.text,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
