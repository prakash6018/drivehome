package com.drivehome.launcher

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.drivehome.launcher.data.AppEntry
import com.drivehome.launcher.data.AppRepository
import com.drivehome.launcher.data.Prefs
import com.drivehome.launcher.data.TileRole
import com.drivehome.launcher.media.MediaListenerService
import com.drivehome.launcher.sensors.TripService
import com.drivehome.launcher.ui.AppDrawer
import com.drivehome.launcher.ui.DiagnosticsScreen
import com.drivehome.launcher.ui.HomeActions
import com.drivehome.launcher.ui.HomeScreen
import com.drivehome.launcher.ui.HomeState
import com.drivehome.launcher.ui.SettingsActions
import com.drivehome.launcher.ui.SettingsScreen
import com.drivehome.launcher.ui.theme.Accents
import com.drivehome.launcher.ui.theme.DriveHomeTheme
import com.drivehome.launcher.ui.theme.dayPalette
import com.drivehome.launcher.ui.theme.isNight
import com.drivehome.launcher.ui.theme.nightPalette
import com.drivehome.launcher.util.CrashLog
import com.drivehome.launcher.util.DiagnosticsReport
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime

private sealed interface Screen {
    data object Home : Screen
    data class Drawer(val pickSlot: Int? = null) : Screen
    data object Settings : Screen
    data object Diagnostics : Screen
}

private const val MAPS_PACKAGE = "com.google.android.apps.maps"

class MainActivity : ComponentActivity() {
    private val app get() = application as DriveHomeApp
    private lateinit var prefs: Prefs
    private lateinit var appRepo: AppRepository
    private val media get() = app.media

    private var screen by mutableStateOf<Screen>(Screen.Home)
    private var hasLocation by mutableStateOf(false)
    private var usageAccess by mutableStateOf(false)
    private var diagnosticsVersion by mutableIntStateOf(0)

    // The head unit boots with the ignition, so a fresh activity = "ignition on":
    // play the dashboard reveal and gauge sweep once.
    private var introPending by mutableStateOf(true)

    private val locationRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasLocation = granted
            if (granted) startTripService()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        appRepo = AppRepository(this)

        hasLocation = app.speedTracker.hasPermission()
        if (!hasLocation && !prefs.askedLocation) {
            prefs.askedLocation = true
            locationRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        setContent { Root() }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { appRepo.refresh() }
        media.start()
        usageAccess = media.usageAccess
        hasLocation = app.speedTracker.hasPermission()
        if (hasLocation) startTripService()
    }

    override fun onPause() {
        super.onPause()
        media.stop()
    }

    // Pressing Home while already in the launcher always returns to the dashboard.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) screen = Screen.Home
    }

    @Composable
    private fun Root() {
        val settings by prefs.settings.collectAsStateWithLifecycle()
        val apps by appRepo.apps.collectAsStateWithLifecycle()
        val nowPlaying by media.state.collectAsStateWithLifecycle()
        val mediaAccess by media.hasAccess.collectAsStateWithLifecycle()
        val speedReading by app.speedTracker.reading.collectAsStateWithLifecycle()
        val trip by app.speedTracker.trip.collectAsStateWithLifecycle()
        val location by app.speedTracker.location.collectAsStateWithLifecycle()
        val weather by app.weather.weather.collectAsStateWithLifecycle()
        val place by app.weather.place.collectAsStateWithLifecycle()
        val now by rememberNow()

        // Re-checked when the first fix arrives and then hourly; the repository skips fresh data.
        LaunchedEffect(location != null, now.hour) { app.weather.refreshIfStale(location) }

        val accent = Accents.getOrElse(settings.accentIndex) { Accents[0] }.color
        val palette = if (isNight(settings.themeMode, now.toLocalTime())) nightPalette(accent) else dayPalette(accent)
        val tiles = remember(apps, settings.tiles) { appRepo.buildTiles(apps, settings.tiles) }
        LaunchedEffect(tiles) {
            media.setKnownMediaApps(
                listOf(TileRole.MUSIC, TileRole.RADIO).mapNotNull { tiles[it.ordinal].app?.packageName }.toSet()
            )
        }
        LaunchedEffect(now) { media.poll() }

        @Suppress("DEPRECATION")
        LaunchedEffect(palette.background) {
            window.statusBarColor = palette.background.toArgb()
            window.navigationBarColor = palette.background.toArgb()
        }

        DriveHomeTheme(palette) {
            when (val s = screen) {
                Screen.Home -> {
                    BackHandler {} // Back on the home screen does nothing, like any launcher.
                    HomeScreen(
                        HomeState(
                            now = now,
                            elapsedNow = remember(now) { SystemClock.elapsedRealtime() },
                            settings = settings,
                            tiles = tiles,
                            nowPlaying = nowPlaying,
                            mediaAccess = mediaAccess,
                            speed = speedReading,
                            trip = trip,
                            weather = weather,
                            place = place,
                            hasLocation = hasLocation,
                            playIntro = introPending,
                        ),
                        HomeActions(
                            onTile = { i ->
                                val app = tiles[i].app
                                if (app != null) launch(app) else screen = Screen.Drawer(pickSlot = i)
                            },
                            onTileLongPress = { i -> screen = Screen.Drawer(pickSlot = i) },
                            onOpenApps = { screen = Screen.Drawer() },
                            onOpenSettings = { screen = Screen.Settings },
                            onOpenDiagnostics = { screen = Screen.Diagnostics },
                            onRequestLocation = { requestLocation() },
                            onRequestMediaAccess = { openNotificationAccess() },
                            onPlayPause = media::playPause,
                            onNext = media::next,
                            onPrevious = media::previous,
                            onOpenPlayer = {
                                if (!media.openPlayer()) tiles[TileRole.MUSIC.ordinal].app?.let(::launch)
                            },
                            onOpenMap = { tiles[TileRole.NAVIGATE.ordinal].app?.let(::launch) ?: navigateTo("") },
                            onNavigateHome = { navigateTo(settings.homeAddress, "Home") },
                            onNavigateWork = { navigateTo(settings.workAddress, "Work") },
                            onIntroDone = { introPending = false },
                        ),
                    )
                }

                is Screen.Drawer -> {
                    val slot = s.pickSlot
                    AppDrawer(
                        apps = apps,
                        pickingFor = slot?.let { TileRole.entries[it].label },
                        onSelect = { app ->
                            if (slot == null) {
                                launch(app)
                            } else {
                                setTile(slot, app.packageName)
                                screen = Screen.Home
                            }
                        },
                        onReset = slot?.let { { setTile(it, ""); screen = Screen.Home } },
                        onClose = { screen = Screen.Home },
                    )
                }

                Screen.Settings -> SettingsScreen(
                    settings = settings,
                    mediaAccess = mediaAccess,
                    usageAccess = usageAccess,
                    hasLocation = hasLocation,
                    actions = SettingsActions(
                        onUpdate = prefs::update,
                        onMediaAccess = { openNotificationAccess() },
                        onLocation = { requestLocation() },
                        onUsageAccess = { safeStart(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), Intent(Settings.ACTION_SETTINGS)) },
                        onChooseHomeApp = { safeStart(Intent(Settings.ACTION_HOME_SETTINGS), Intent(Settings.ACTION_SETTINGS)) },
                        onOpenOriginalLauncher = { openOriginalLauncher() },
                        onAndroidSettings = { safeStart(Intent(Settings.ACTION_SETTINGS)) },
                        onDiagnostics = { screen = Screen.Diagnostics },
                        onBack = { screen = Screen.Home },
                    ),
                )

                Screen.Diagnostics -> {
                    val report = remember(diagnosticsVersion, apps, mediaAccess, hasLocation) {
                        DiagnosticsReport.build(this, apps, mediaAccess, hasLocation, media.describe())
                    }
                    DiagnosticsScreen(
                        report = report,
                        onExport = {
                            val result = runCatching { DiagnosticsReport.export(this, report) }
                            toast(result.fold({ "Saved to $it" }, { "Could not save: ${it.message}" }))
                        },
                        onClearCrashLog = {
                            CrashLog.clear(this)
                            diagnosticsVersion++
                        },
                        onBack = { screen = Screen.Home },
                    )
                }
            }
        }
    }

    private fun startTripService() {
        runCatching { startForegroundService(Intent(this, TripService::class.java)) }
    }

    private fun setTile(slot: Int, pkg: String) =
        prefs.update { it.copy(tiles = it.tiles.toMutableList().also { list -> list[slot] = pkg }) }

    private fun launch(app: AppEntry) {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setClassName(app.packageName, app.activityName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        if (runCatching { startActivity(intent) }.isFailure) toast("Can't open ${app.label}")
        else media.noteLaunched(app.packageName)
    }

    /** Turn-by-turn in Google Maps when installed, otherwise any app that handles geo: links. */
    private fun navigateTo(address: String, label: String = "") {
        if (address.isBlank() && label.isNotEmpty()) {
            toast("Add your $label address in Settings")
            screen = Screen.Settings
            return
        }
        val query = Uri.encode(address)
        val turnByTurn = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$query")).setPackage(MAPS_PACKAGE)
        val anyMap = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$query"))
        safeStart(if (address.isBlank()) anyMap else turnByTurn, anyMap)
    }

    private fun requestLocation() {
        if (app.speedTracker.hasPermission()) return
        locationRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun openNotificationAccess() {
        val list = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        if (Build.VERSION.SDK_INT >= 30) {
            val detail = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
                Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                ComponentName(this, MediaListenerService::class.java).flattenToString(),
            )
            safeStart(detail, list)
        } else {
            safeStart(list)
        }
    }

    // Escape hatch: always possible to get back to the vendor's home screen.
    private fun openOriginalLauncher() {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val other = packageManager.queryIntentActivities(homeIntent, 0)
            .map { it.activityInfo }
            .firstOrNull { it.packageName != packageName && it.packageName != "com.android.settings" }
            ?: return toast("No other launcher found")
        safeStart(Intent(homeIntent).setClassName(other.packageName, other.name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun safeStart(intent: Intent, fallback: Intent? = null) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (runCatching { startActivity(intent) }.isSuccess) return
        if (fallback != null && runCatching { startActivity(fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess) return
        toast("Not available on this head unit")
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

@Composable
private fun rememberNow(): State<LocalDateTime> = produceState(LocalDateTime.now()) {
    while (true) {
        delay(1000L - System.currentTimeMillis() % 1000L)
        value = LocalDateTime.now()
    }
}
