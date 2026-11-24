package com.drivehome.launcher.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.drivehome.launcher.ui.theme.LocalPalette
import kotlin.random.Random

// A cool teal partner for the accent, so the mesh has two hues without going rainbow.
private val MeshPartner = Color(0xFF00C2A8)

/** 64px tile of random grey specks; repeated at very low alpha it removes the "flat digital" look. */
private fun grainTile(): ImageBitmap {
    val size = 64
    val rnd = Random(42)
    val pixels = IntArray(size * size) {
        val v = rnd.nextInt(256)
        (rnd.nextInt(40) shl 24) or (v shl 16) or (v shl 8) or v
    }
    return android.graphics.Bitmap.createBitmap(pixels, size, size, android.graphics.Bitmap.Config.ARGB_8888).asImageBitmap()
}

/**
 * Level-0 atmosphere behind every screen: a heavily blurred copy of the current album art
 * (so the whole cabin screen takes on the song's colours, like CarPlay), an accent/teal mesh,
 * film grain and a vignette.
 */
@Composable
fun AmbientBackdrop(
    art: ImageBitmap?,
    glowFromRight: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val p = LocalPalette.current
    val grain = remember { ShaderBrush(ImageShader(grainTile(), TileMode.Repeated, TileMode.Repeated)) }
    val partner = lerp(p.accent, MeshPartner, 0.6f)

    Box(modifier.drawBehind { drawRect(p.background) }) {
        Crossfade(art, animationSpec = tween(900), label = "ambient") { bitmap ->
            if (bitmap != null) {
                Image(
                    bitmap, null,
                    Modifier
                        .fillMaxSize()
                        .blur(90.dp, BlurredEdgeTreatment.Rectangle),
                    contentScale = ContentScale.Crop,
                    alpha = if (p.isNight) 0.28f else 0.38f,
                )
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .drawWithContent {
                    val driverX = if (glowFromRight) size.width * 0.9f else size.width * 0.1f
                    drawRect(
                        Brush.radialGradient(
                            listOf(p.accent.copy(alpha = if (p.isNight) 0.16f else 0.22f), Color.Transparent),
                            center = Offset(driverX, size.height * 0.05f),
                            radius = size.maxDimension * 0.55f,
                        )
                    )
                    drawRect(
                        Brush.radialGradient(
                            listOf(partner.copy(alpha = if (p.isNight) 0.08f else 0.12f), Color.Transparent),
                            center = Offset(size.width - driverX, size.height),
                            radius = size.maxDimension * 0.6f,
                        )
                    )
                    drawRect(grain, alpha = 0.5f)
                    drawRect(
                        Brush.radialGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                            center = Offset(size.width / 2, size.height / 2),
                            radius = size.maxDimension * 0.75f,
                        )
                    )
                    drawContent()
                },
            content = content,
        )
    }
}
