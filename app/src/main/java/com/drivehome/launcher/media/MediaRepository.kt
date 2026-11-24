package com.drivehome.launcher.media

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.service.notification.StatusBarNotification
import android.view.KeyEvent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class NowPlaying(
    val title: String,
    val artist: String,
    val appLabel: String,
    val packageName: String,
    val art: ImageBitmap?,
    /** Shown in place of album art for sources without artwork, e.g. the vendor radio. */
    val appIcon: ImageBitmap?,
    val isPlaying: Boolean,
    /** False for apps without a media session: we can show and open them, not control them. */
    val hasSession: Boolean,
    val durationMs: Long,
    /** Position at [positionAtElapsed] (SystemClock.elapsedRealtime); extrapolate with [speed]. */
    val positionMs: Long,
    val positionAtElapsed: Long,
    val speed: Float,
) {
    fun positionNow(elapsedNow: Long): Long {
        if (!isPlaying) return positionMs
        return (positionMs + ((elapsedNow - positionAtElapsed) * speed).toLong()).coerceIn(0L, durationMs.coerceAtLeast(0L))
    }
}

/** Latest notification text from a media app, used for players that have no session. */
private data class SourceNote(val key: String, val title: String, val text: String, val icon: Bitmap?)

// Label/package fragments that mark an app as an audio source (vendor apps are named loosely).
private val MEDIA_HINTS = listOf(
    "radio", "fm", "tuner", "dab", "music", "player", "audio", "media", "bluetooth", "btmusic",
    "bt music", "a2dp", "aux", "usb", "video", "podcast", "spotify", "saavn", "gaana", "wynk",
)

/**
 * Decides what the media widget shows. Three signals, most recent wins:
 *  1. media sessions (Spotify, YT Music…) — full metadata and controls;
 *  2. the app the driver last switched to (from DriveHome, or Usage access for the MODE key);
 *  3. notifications from audio apps — the only text vendor radios usually expose.
 * One instance per process, shared with [MediaListenerService].
 */
class MediaRepository(private val context: Context) {
    private val pm = context.packageManager
    private val sessionManager = context.getSystemService(MediaSessionManager::class.java)
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val listenerComponent = ComponentName(context, MediaListenerService::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val foreground = ForegroundTracker(context)

    private val _state = MutableStateFlow<NowPlaying?>(null)
    val state: StateFlow<NowPlaying?> = _state
    private val _hasAccess = MutableStateFlow(false)
    val hasAccess: StateFlow<Boolean> = _hasAccess

    private var sessions: List<MediaController> = emptyList()
    private var controller: MediaController? = null
    private var sessionPlayingSince = 0L
    private var lastSwitch: Pair<String, Long>? = null
    private val notes = HashMap<String, SourceNote>()
    private val sessionPackages = HashSet<String>()
    private var knownMediaApps: Set<String> = emptySet()
    private val labels = HashMap<String, String>()
    private val icons = HashMap<String, ImageBitmap?>()
    private var lastArt: Pair<Bitmap, ImageBitmap>? = null
    private var musicActive = false

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            if (state?.state == PlaybackState.STATE_PLAYING && sessionPlayingSince == 0L) {
                sessionPlayingSince = System.currentTimeMillis()
            } else if (state?.state != PlaybackState.STATE_PLAYING) {
                sessionPlayingSince = 0L
            }
            publish()
        }
        override fun onSessionDestroyed() = pick(activeSessions())
    }
    private val sessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { pick(it.orEmpty()) }

    val usageAccess: Boolean get() = foreground.hasAccess()

    fun start() {
        stop()
        _hasAccess.value =
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
        foreground.latestSince(::isMediaApp)?.let { noteSwitch(it.first, it.second) }
        if (!_hasAccess.value) return publish()
        try {
            sessionManager.addOnActiveSessionsChangedListener(sessionsListener, listenerComponent, handler)
            pick(activeSessions())
        } catch (e: SecurityException) {
            _hasAccess.value = false
            publish()
        }
    }

    fun stop() {
        runCatching { sessionManager.removeOnActiveSessionsChangedListener(sessionsListener) }
        controller?.unregisterCallback(callback)
        controller = null
    }

    /** Apps the driver put on the Music / Radio tiles always count as audio sources. */
    fun setKnownMediaApps(packages: Set<String>) {
        knownMediaApps = packages
    }

    /** Called when the driver opens an app from DriveHome. */
    fun noteLaunched(pkg: String) {
        if (isMediaApp(pkg)) noteSwitch(pkg, System.currentTimeMillis())
    }

    /** Called every second by the UI; cheap. Catches session-less players starting or stopping. */
    fun poll() {
        val active = audioManager.isMusicActive
        if (active != musicActive) {
            musicActive = active
            publish()
        }
    }

    fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        if (pkg == context.packageName || !isMediaApp(pkg)) return
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (extras.getCharSequence(Notification.EXTRA_TEXT) ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT))
            ?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return
        val icon = runCatching { sbn.notification.getLargeIcon()?.loadDrawable(context)?.toBitmap(256, 256) }.getOrNull()
        val previous = notes[pkg]
        notes[pkg] = SourceNote(sbn.key, title, text, icon)
        // A new station/track shown by a session-less app means the driver is listening to it.
        if (pkg !in sessionPackages && (previous == null || previous.title != title || previous.text != text)) {
            noteSwitch(pkg, sbn.postTime)
        } else {
            publish()
        }
    }

    fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (notes[sbn.packageName]?.key == sbn.key) {
            notes.remove(sbn.packageName)
            publish()
        }
    }

    fun playPause() = control(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) { c ->
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause()
        else c.transportControls.play()
    }

    fun next() = control(KeyEvent.KEYCODE_MEDIA_NEXT) { it.transportControls.skipToNext() }

    fun previous() = control(KeyEvent.KEYCODE_MEDIA_PREVIOUS) { it.transportControls.skipToPrevious() }

    private fun control(key: Int, withSession: (MediaController) -> Unit) {
        val c = currentController()
        when {
            c != null -> withSession(c)
            // Nobody owns the media keys, so the key event can only reach the vendor player.
            sessions.isEmpty() -> sendKey(key)
            // Another app holds the keys and would start playing over the radio; open the radio instead.
            else -> openPlayer()
        }
    }

    /** Opens whatever the widget is showing. Returns false if nothing is known yet. */
    fun openPlayer(): Boolean {
        val shown = _state.value ?: return false
        currentController()?.sessionActivity?.let { pi -> if (runCatching { pi.send() }.isSuccess) return true }
        val launch = pm.getLaunchIntentForPackage(shown.packageName) ?: return false
        return runCatching { context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
    }

    /** Human-readable state for the Diagnostics report. */
    fun describe(): String = buildString {
        appendLine("Notification access: ${_hasAccess.value}   Usage access: ${foreground.hasAccess()}")
        appendLine("Music active (AudioManager): ${audioManager.isMusicActive}")
        appendLine("Sessions:")
        if (sessions.isEmpty()) appendLine("  (none)")
        sessions.forEach { appendLine("  ${it.packageName}  state=${it.playbackState?.state}") }
        appendLine("Last switch: ${lastSwitch?.let { "${it.first} at ${java.util.Date(it.second)}" } ?: "(none)"}")
        appendLine("Notifications from audio apps:")
        if (notes.isEmpty()) appendLine("  (none)")
        notes.forEach { (pkg, n) -> appendLine("  $pkg: \"${n.title}\" / \"${n.text}\"") }
        appendLine("Showing: ${_state.value?.let { "${it.packageName} (session=${it.hasSession})" } ?: "(nothing)"}")
    }

    private fun noteSwitch(pkg: String, at: Long) {
        if (at < (lastSwitch?.second ?: 0L)) return
        lastSwitch = pkg to at
        pick(sessions)
    }

    private fun isMediaApp(pkg: String): Boolean {
        if (pkg == context.packageName) return false
        if (pkg in sessionPackages || pkg in knownMediaApps) return true
        val haystack = "$pkg ${labelOf(pkg)}".lowercase()
        return MEDIA_HINTS.any { it in haystack }
    }

    private fun activeSessions(): List<MediaController> =
        runCatching { sessionManager.getActiveSessions(listenerComponent) }.getOrDefault(emptyList())

    private fun pick(list: List<MediaController>) {
        sessions = list
        list.forEach { sessionPackages += it.packageName }
        val switched = lastSwitch?.first
        val playing = list.filter { it.playbackState?.state == PlaybackState.STATE_PLAYING }
        val best = playing.firstOrNull { it.packageName == switched }
            ?: playing.firstOrNull()
            ?: list.firstOrNull { it.packageName == switched }
            ?: list.firstOrNull()
        if (best?.sessionToken != controller?.sessionToken) {
            controller?.unregisterCallback(callback)
            controller = best
            best?.registerCallback(callback, handler)
            sessionPlayingSince = if (best?.playbackState?.state == PlaybackState.STATE_PLAYING) System.currentTimeMillis() else 0L
        }
        publish()
    }

    /** The session to control, or null when the widget shows a session-less app. */
    private fun currentController(): MediaController? =
        controller?.takeIf { _state.value?.hasSession == true && it.packageName == _state.value?.packageName }

    private fun publish() {
        val c = controller
        val sw = lastSwitch
        val sessionPlaying = c?.playbackState?.state == PlaybackState.STATE_PLAYING
        // A switch to a different app newer than the session's last "play" wins: the vendor radio
        // can play through the head unit's own audio path while Spotify still reports "playing".
        val showSwitched = sw != null && sw.first != c?.packageName &&
            (!sessionPlaying || sw.second > sessionPlayingSince)
        _state.value = when {
            showSwitched -> fromApp(sw.first)
            c != null -> fromSession(c)
            else -> notes.keys.firstOrNull()?.let(::fromApp)
        }
    }

    private fun fromSession(c: MediaController): NowPlaying {
        val md = c.metadata
        return NowPlaying(
            title = md?.getString(MediaMetadata.METADATA_KEY_TITLE)
                ?: md?.description?.title?.toString().orEmpty(),
            artist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: md?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                ?: md?.description?.subtitle?.toString().orEmpty(),
            appLabel = labelOf(c.packageName),
            packageName = c.packageName,
            art = artOf(md),
            appIcon = iconOf(c.packageName),
            isPlaying = c.playbackState?.state == PlaybackState.STATE_PLAYING,
            hasSession = true,
            durationMs = md?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
            positionMs = c.playbackState?.position ?: 0L,
            positionAtElapsed = c.playbackState?.lastPositionUpdateTime ?: 0L,
            speed = c.playbackState?.playbackSpeed ?: 1f,
        )
    }

    private fun fromApp(pkg: String): NowPlaying {
        val note = notes[pkg]
        val label = labelOf(pkg)
        return NowPlaying(
            title = note?.title?.takeIf { it.isNotBlank() } ?: label,
            artist = note?.text?.takeIf { it.isNotBlank() } ?: if (audioManager.isMusicActive) "Playing" else "Tap to open",
            appLabel = label,
            packageName = pkg,
            art = note?.icon?.asImageBitmap(),
            appIcon = iconOf(pkg),
            isPlaying = audioManager.isMusicActive,
            hasSession = false,
            durationMs = 0L,
            positionMs = 0L,
            positionAtElapsed = 0L,
            speed = 1f,
        )
    }

    private fun labelOf(pkg: String): String = labels.getOrPut(pkg) {
        runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)
    }

    private fun iconOf(pkg: String): ImageBitmap? = icons.getOrPut(pkg) {
        runCatching { pm.getApplicationIcon(pkg).toBitmap(192, 192).asImageBitmap() }.getOrNull()
    }

    // Playback-state ticks arrive often; reuse the converted bitmap when the art hasn't changed.
    private fun artOf(md: MediaMetadata?): ImageBitmap? {
        val bmp = md?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: md?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: md?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
            ?: return null
        lastArt?.let { (src, img) -> if (src === bmp) return img }
        return bmp.asImageBitmap().also { lastArt = bmp to it }
    }

    private fun sendKey(code: Int) {
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
    }
}
