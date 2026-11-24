package com.drivehome.launcher.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drivehome.launcher.R
import com.drivehome.launcher.media.NowPlaying
import com.drivehome.launcher.ui.theme.LocalPalette
import com.drivehome.launcher.ui.theme.Orbitron

private val ArtShape = RoundedCornerShape(18.dp)

@Composable
fun MediaWidget(
    modifier: Modifier,
    nowPlaying: NowPlaying?,
    mediaAccess: Boolean,
    elapsedNow: Long,
    onOpen: () -> Unit,
    onRequestAccess: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    val p = LocalPalette.current
    val playing = nowPlaying?.isPlaying == true
    val art = nowPlaying?.art
    val title = nowPlaying?.title?.takeIf { it.isNotBlank() } ?: "Nothing playing"
    val subtitle = when {
        nowPlaying != null -> nowPlaying.artist
        mediaAccess -> "Tap to open your music app"
        else -> "Tap to allow song details"
    }

    // Without access the card's tap goes to the permission screen; the buttons still work.
    Panel(modifier, onClick = if (mediaAccess) onOpen else onRequestAccess) {
        if (art != null) {
            Image(
                art, null,
                Modifier.fillMaxSize().blur(40.dp, BlurredEdgeTreatment.Rectangle),
                contentScale = ContentScale.Crop,
                alpha = if (p.isNight) 0.22f else 0.32f,
            )
        }
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WidgetLabel(if (playing) "Now playing" else "Media", R.drawable.ic_music, p.accent)
                if (playing) EqualizerBars(p.accent)
                Spacer(Modifier.weight(1f))
                Text(
                    nowPlaying?.appLabel.orEmpty(),
                    color = p.textDim,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // Capped so a tall screen grows the art only until the title would get squeezed.
                AlbumArt(Modifier.sizeIn(maxWidth = 168.dp, maxHeight = 168.dp).fillMaxHeight().aspectRatio(1f, matchHeightConstraintsFirst = true), art, nowPlaying?.appIcon)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        color = p.text,
                        fontSize = 26.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        subtitle,
                        color = p.textDim,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (nowPlaying != null && nowPlaying.durationMs > 0) {
                Spacer(Modifier.height(12.dp))
                ProgressRow(nowPlaying.positionNow(elapsedNow), nowPlaying.durationMs)
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundButton(R.drawable.ic_prev, "Previous", 76.dp, p.surfaceHigh.copy(alpha = Glass.CONTROL), p.text, onClick = onPrevious)
                RoundButton(
                    if (playing) R.drawable.ic_pause else R.drawable.ic_play,
                    if (playing) "Pause" else "Play",
                    88.dp, p.accent, p.onAccent, glow = p.accent, onClick = onPlayPause,
                )
                RoundButton(R.drawable.ic_next, "Next", 76.dp, p.surfaceHigh.copy(alpha = Glass.CONTROL), p.text, onClick = onNext)
            }
        }
    }
}

@Composable
private fun AlbumArt(modifier: Modifier, art: ImageBitmap?, appIcon: ImageBitmap?) {
    val p = LocalPalette.current
    Box(
        modifier
            .clip(ArtShape)
            .background(Brush.linearGradient(listOf(p.accent.copy(alpha = 0.35f), p.surfaceHigh)))
            .border(1.dp, Color.White.copy(alpha = 0.12f), ArtShape),
        contentAlignment = Alignment.Center,
    ) {
        if (art != null) {
            Image(art, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = if (p.isNight) 0.85f else 1f)
        } else if (appIcon != null) {
            // Radio / Bluetooth / USB sources have no artwork: show the source app's own icon.
            Image(appIcon, null, Modifier.fillMaxSize(0.55f))
        } else {
            Icon(painterResource(R.drawable.ic_music), null, Modifier.fillMaxSize(0.4f), tint = p.text.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun ProgressRow(positionMs: Long, durationMs: Long) {
    val p = LocalPalette.current
    val fraction = (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    val timeStyle = TextStyle(fontFamily = Orbitron, fontWeight = FontWeight.Medium, fontSize = 11.sp, fontFeatureSettings = "tnum")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(formatTime(positionMs), color = p.textDim, style = timeStyle)
        Box(
            Modifier
                .weight(1f)
                .padding(horizontal = 10.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.12f))
        ) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(fraction).background(p.accent))
        }
        Text("-" + formatTime(durationMs - positionMs), color = p.textDim, style = timeStyle)
    }
}

private fun formatTime(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

@Composable
private fun EqualizerBars(color: Color) {
    val transition = rememberInfiniteTransition(label = "eq")
    Row(
        Modifier.padding(start = 10.dp).height(14.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        listOf(0, 180, 90, 270).forEachIndexed { i, offset ->
            val h by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(420 + i * 70, easing = LinearEasing),
                    RepeatMode.Reverse,
                    initialStartOffset = StartOffset(offset),
                ),
                label = "bar$i",
            )
            Box(Modifier.width(3.dp).fillMaxHeight(h).background(color))
        }
    }
}
