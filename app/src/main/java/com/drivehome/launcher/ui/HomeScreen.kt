package com.drivehome.launcher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.drivehome.launcher.data.DriverSide
import com.drivehome.launcher.data.LauncherSettings
import com.drivehome.launcher.data.Place
import com.drivehome.launcher.data.TileModel
import com.drivehome.launcher.data.Weather
import com.drivehome.launcher.media.NowPlaying
import com.drivehome.launcher.sensors.SpeedReading
import com.drivehome.launcher.sensors.TripStats
import java.time.LocalDateTime

private val Gap = 14.dp

class HomeState(
    val now: LocalDateTime,
    val elapsedNow: Long,
    val settings: LauncherSettings,
    val tiles: List<TileModel>,
    val nowPlaying: NowPlaying?,
    val mediaAccess: Boolean,
    val speed: SpeedReading?,
    val trip: TripStats,
    val weather: Weather?,
    val place: Place?,
    val hasLocation: Boolean,
    /** True on the first home screen after boot: plays the reveal and the gauge sweep. */
    val playIntro: Boolean,
)

class HomeActions(
    val onTile: (Int) -> Unit,
    val onTileLongPress: (Int) -> Unit,
    val onOpenApps: () -> Unit,
    val onOpenSettings: () -> Unit,
    val onOpenDiagnostics: () -> Unit,
    val onRequestLocation: () -> Unit,
    val onRequestMediaAccess: () -> Unit,
    val onPlayPause: () -> Unit,
    val onNext: () -> Unit,
    val onPrevious: () -> Unit,
    val onOpenPlayer: () -> Unit,
    val onOpenMap: () -> Unit,
    val onNavigateHome: () -> Unit,
    val onNavigateWork: () -> Unit,
    val onIntroDone: () -> Unit,
)

/**
 * CarPlay-style dashboard: glass widgets over an ambient backdrop, with the dock on the
 * driver's side. Columns are ordered so the most glanced-at info (speed, trip) sits nearest
 * the driver and the map, which is read longest, sits furthest away.
 */
@Composable
fun HomeScreen(state: HomeState, actions: HomeActions) {
    val s = state
    val driverRight = s.settings.driverSide == DriverSide.RIGHT
    var order = 0
    fun next() = order++

    val mapColumn: @Composable RowScope.() -> Unit = {
        Reveal(next(), s.playIntro, Modifier.weight(1.3f).fillMaxHeight()) {
            MapWidget(
                Modifier.fillMaxSize(), s.place, s.hasLocation,
                onOpen = actions.onOpenMap, onHome = actions.onNavigateHome, onWork = actions.onNavigateWork,
            )
        }
    }
    val mediaColumn: @Composable RowScope.() -> Unit = {
        Column(Modifier.weight(1.1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(Gap)) {
            Reveal(next(), s.playIntro, Modifier.weight(1.45f).fillMaxWidth()) {
                MediaWidget(
                    Modifier.fillMaxSize(), s.nowPlaying, s.mediaAccess, s.elapsedNow,
                    onOpen = actions.onOpenPlayer,
                    onRequestAccess = actions.onRequestMediaAccess,
                    onPlayPause = actions.onPlayPause,
                    onNext = actions.onNext,
                    onPrevious = actions.onPrevious,
                )
            }
            Row(Modifier.weight(0.75f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Gap)) {
                Reveal(next(), s.playIntro, Modifier.weight(1f).fillMaxHeight()) {
                    WeatherWidget(Modifier.fillMaxSize(), s.weather, s.hasLocation)
                }
                Reveal(next(), s.playIntro, Modifier.weight(1f).fillMaxHeight()) {
                    DayWidget(Modifier.fillMaxSize(), s.now)
                }
            }
        }
    }
    val driveColumn: @Composable RowScope.() -> Unit = {
        Column(Modifier.weight(0.95f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(Gap)) {
            Reveal(next(), s.playIntro, Modifier.weight(1.15f).fillMaxWidth()) {
                SpeedWidget(
                    Modifier.fillMaxSize(), s.speed, s.hasLocation,
                    selfTest = s.playIntro,
                    onSelfTestDone = actions.onIntroDone,
                    onRequestLocation = actions.onRequestLocation,
                )
            }
            Reveal(next(), s.playIntro, Modifier.weight(0.85f).fillMaxWidth()) {
                TripWidget(Modifier.fillMaxSize(), s.settings.carName, s.trip, onLongPress = actions.onOpenDiagnostics)
            }
        }
    }
    val dock: @Composable RowScope.() -> Unit = {
        Reveal(next(), s.playIntro, Modifier.width(112.dp).fillMaxHeight()) {
            Dock(
                Modifier.fillMaxSize(), s.now, s.settings.use24h, s.tiles,
                onTile = actions.onTile,
                onTileLongPress = actions.onTileLongPress,
                onApps = actions.onOpenApps,
                onSettings = actions.onOpenSettings,
            )
        }
    }

    val columns = listOf(mapColumn, mediaColumn, driveColumn, dock)
    AmbientBackdrop(s.nowPlaying?.art, glowFromRight = driverRight, modifier = Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(Gap)) {
            (if (driverRight) columns else columns.reversed()).forEach { it(this) }
        }
    }
}
