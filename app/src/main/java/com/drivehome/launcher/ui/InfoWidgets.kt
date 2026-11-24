package com.drivehome.launcher.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drivehome.launcher.R
import com.drivehome.launcher.data.Weather
import com.drivehome.launcher.sensors.SpeedReading
import com.drivehome.launcher.sensors.TripStats
import com.drivehome.launcher.ui.theme.LocalPalette
import com.drivehome.launcher.ui.theme.Orbitron
import com.drivehome.launcher.ui.theme.labelStyle
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val SPEED_STALE_MS = 4_000L

private fun bigNumber(size: TextUnit) =
    TextStyle(fontFamily = Orbitron, fontWeight = FontWeight.Black, fontSize = size, fontFeatureSettings = "tnum")

@Composable
fun WeatherWidget(modifier: Modifier, weather: Weather?, hasLocation: Boolean) {
    val p = LocalPalette.current
    Panel(modifier) {
        Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.SpaceBetween) {
            WidgetLabel("Weather", R.drawable.ic_cloud)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painterResource(weather?.icon ?: R.drawable.ic_cloud), null,
                    Modifier
                        .size(42.dp)
                        .drawBehind {
                            drawCircle(Brush.radialGradient(listOf(p.accent.copy(alpha = 0.35f), Color.Transparent)), radius = size.minDimension)
                        },
                    tint = if (weather != null) p.accent else p.textDim,
                )
                Spacer(Modifier.width(12.dp))
                Text(weather?.let { "${it.tempC}°" } ?: "--°", color = p.text, style = bigNumber(36.sp))
            }
            Column {
                Text(
                    weather?.condition ?: if (hasLocation) "Needs internet" else "Needs location",
                    color = p.text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (weather != null) {
                    Text("H ${weather.highC}°  ·  L ${weather.lowC}°", color = p.textDim, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun DayWidget(modifier: Modifier, now: LocalDateTime) {
    val p = LocalPalette.current
    val greeting = when (now.hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
    Panel(modifier) {
        Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.SpaceBetween) {
            WidgetLabel(now.format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault())), color = p.accent)
            Text(now.dayOfMonth.toString(), color = p.text, style = bigNumber(46.sp))
            Column {
                Text(
                    now.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                    color = p.text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(greeting, color = p.textDim, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun SpeedWidget(
    modifier: Modifier,
    speed: SpeedReading?,
    hasLocation: Boolean,
    selfTest: Boolean,
    onSelfTestDone: () -> Unit,
    onRequestLocation: () -> Unit,
) {
    val live = speed != null && System.currentTimeMillis() - speed.atMillis < SPEED_STALE_MS
    Panel(modifier, onClick = if (hasLocation) null else onRequestLocation) {
        Column(Modifier.fillMaxSize().padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 6.dp)) {
            WidgetLabel("Speed", R.drawable.ic_speed)
            SpeedGauge(
                kmh = if (live) speed.kmh else null,
                status = when {
                    !hasLocation -> "Tap to enable GPS"
                    live -> "GPS"
                    else -> "No GPS fix"
                },
                selfTest = selfTest,
                onSelfTestDone = onSelfTestDone,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        }
    }
}

/** Trip computer since ignition, under the car. Long-press opens Diagnostics. */
@Composable
fun TripWidget(modifier: Modifier, carName: String, trip: TripStats, onLongPress: () -> Unit) {
    val p = LocalPalette.current
    val minutes = trip.movingMillis / 60_000
    Panel(modifier, onLongClick = onLongPress) {
        Column(Modifier.fillMaxSize().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WidgetLabel("Trip", R.drawable.ic_route)
                Spacer(Modifier.weight(1f))
                LiveryStripes()
                Text(carName.uppercase(), Modifier.padding(start = 8.dp), color = p.text, style = labelStyle(11.sp), maxLines = 1)
            }
            CarPhoto(Modifier.weight(1f).fillMaxWidth().padding(vertical = 4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TripStat(R.drawable.ic_route, "%.1f".format(trip.distanceMeters / 1000f), "km")
                TripStat(R.drawable.ic_timer, "%d:%02d".format(minutes / 60, minutes % 60), "drive")
                TripStat(R.drawable.ic_speed, trip.avgKmh.toString(), "avg km/h")
            }
        }
    }
}

@Composable
private fun TripStat(@DrawableRes icon: Int, value: String, unit: String) {
    val p = LocalPalette.current
    Column {
        Text(value, color = p.text, style = bigNumber(20.sp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(icon), null, Modifier.size(13.dp), tint = p.accent)
            Text(unit, Modifier.padding(start = 4.dp), color = p.textDim, fontSize = 14.sp)
        }
    }
}
