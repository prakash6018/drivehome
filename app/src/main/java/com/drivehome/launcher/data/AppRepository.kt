package com.drivehome.launcher.data

import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.drivehome.launcher.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

data class AppEntry(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: ImageBitmap,
)

/**
 * The four configurable home tiles. When the user hasn't picked an app we guess one:
 * known package names first, then label keywords (vendor apps like "BT Phone" or "FM Radio").
 */
enum class TileRole(
    val label: String,
    @param:DrawableRes val icon: Int,
    val packages: List<String>,
    val keywords: List<String>,
) {
    NAVIGATE(
        "Navigate", R.drawable.ic_navigation,
        listOf("com.google.android.apps.maps", "com.waze"),
        listOf("maps", "navi", "gps"),
    ),
    MUSIC(
        "Music", R.drawable.ic_music,
        listOf(
            "com.spotify.music", "com.google.android.apps.youtube.music", "com.jio.media.jiobeats",
            "com.gaana", "com.apple.android.music", "com.amazon.mp3",
        ),
        listOf("music", "player", "audio"),
    ),
    PHONE(
        "Phone", R.drawable.ic_phone,
        listOf("com.google.android.dialer", "com.android.dialer"),
        listOf("bluetooth", "bt phone", "phone", "dialer"),
    ),
    RADIO(
        "Radio", R.drawable.ic_radio,
        emptyList(),
        listOf("radio", "fm", "tuner"),
    ),
}

data class TileModel(val role: TileRole, val app: AppEntry?, val customized: Boolean)

class AppRepository(private val context: Context) {
    private val pm = context.packageManager
    private val iconCache = HashMap<String, ImageBitmap>()
    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps

    suspend fun refresh() = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        _apps.value = pm.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != context.packageName }
            .map { ri ->
                val info = ri.activityInfo
                val key = "${info.packageName}/${info.name}"
                val icon = iconCache.getOrPut(key) {
                    ri.loadIcon(pm).toBitmap(width = 144, height = 144).asImageBitmap()
                }
                AppEntry(ri.loadLabel(pm).toString(), info.packageName, info.name, icon)
            }
            .distinctBy { "${it.packageName}/${it.activityName}" }
            .sortedBy { it.label.lowercase() }
    }

    fun buildTiles(apps: List<AppEntry>, chosen: List<String>): List<TileModel> =
        TileRole.entries.mapIndexed { i, role ->
            val pkg = chosen.getOrElse(i) { "" }
            val picked = if (pkg.isNotEmpty()) apps.firstOrNull { it.packageName == pkg } else null
            TileModel(role, picked ?: guess(apps, role), customized = picked != null)
        }

    private fun guess(apps: List<AppEntry>, role: TileRole): AppEntry? {
        for (pkg in role.packages) apps.firstOrNull { it.packageName == pkg }?.let { return it }
        for (k in role.keywords) apps.firstOrNull { it.label.contains(k, ignoreCase = true) }?.let { return it }
        return null
    }
}
