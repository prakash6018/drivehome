package com.drivehome.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.min
import com.drivehome.launcher.R
import com.drivehome.launcher.ui.theme.LocalPalette

// car_photo.png is the user's own Swift, cut out of its studio background (616 x 272 px).
private const val PHOTO_ASPECT = 616f / 272f
// Where the near-side headlamp sits in the photo, as fractions of width/height.
private val HEADLAMP = Offset(0.27f, 0.47f)
private const val REFLECTION = 0.22f

/**
 * The car on a showroom floor: accent light pool, a faded mirror reflection underneath,
 * and at night a glow on the headlamp so it looks switched on.
 */
@Composable
fun CarPhoto(modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        // Fit car + reflection strip inside the box without distorting the photo.
        val carW = min(maxWidth, maxHeight / (1f + REFLECTION) * PHOTO_ASPECT)
        val carH = carW / PHOTO_ASPECT
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.width(carW).height(carH)) {
                Canvas(Modifier.fillMaxSize()) {
                    drawOval(
                        Brush.radialGradient(
                            listOf(p.accent.copy(alpha = 0.45f), Color.Transparent),
                            center = Offset(size.width / 2, size.height * 0.95f),
                            radius = size.width * 0.55f,
                        ),
                        topLeft = Offset(-size.width * 0.05f, size.height * 0.78f),
                        size = Size(size.width * 1.1f, size.height * 0.34f),
                    )
                }
                Image(
                    painterResource(R.drawable.car_photo),
                    contentDescription = "Your car",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    alpha = if (p.isNight) 0.9f else 1f,
                )
                if (p.isNight) {
                    Canvas(Modifier.fillMaxSize()) {
                        val c = Offset(size.width * HEADLAMP.x, size.height * HEADLAMP.y)
                        drawCircle(
                            Brush.radialGradient(listOf(Color.White.copy(alpha = 0.55f), Color.Transparent), center = c, radius = size.width * 0.09f),
                            radius = size.width * 0.09f,
                            center = c,
                        )
                    }
                }
            }
            // Mirror image fading out: flip vertically, then mask with a gradient.
            Image(
                painterResource(R.drawable.car_photo),
                contentDescription = null,
                modifier = Modifier
                    .width(carW)
                    .height(carH * REFLECTION)
                    .graphicsLayer {
                        scaleY = -1f
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f))),
                            blendMode = BlendMode.DstIn,
                        )
                    },
                contentScale = ContentScale.Crop,
                alignment = Alignment.BottomCenter,
            )
        }
    }
}
