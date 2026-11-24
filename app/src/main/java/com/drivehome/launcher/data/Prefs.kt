package com.drivehome.launcher.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Which side of the car the driver sits on. India (RHD) = RIGHT, so controls go on the right. */
enum class DriverSide { LEFT, RIGHT }

enum class ThemeMode { AUTO, DAY, NIGHT }

data class LauncherSettings(
    val driverSide: DriverSide = DriverSide.RIGHT,
    val themeMode: ThemeMode = ThemeMode.AUTO,
    val accentIndex: Int = 0,
    val use24h: Boolean = false,
    val carName: String = "SWIFT ZDi",
    /** Free-text addresses for the one-tap Home / Work buttons on the map widget. */
    val homeAddress: String = "",
    val workAddress: String = "",
    /** Package chosen for each tile slot; empty = pick automatically. */
    val tiles: List<String> = List(TileRole.entries.size) { "" },
)

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("drivehome", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<LauncherSettings> = _settings

    var askedLocation: Boolean
        get() = sp.getBoolean("askedLocation", false)
        set(value) = sp.edit().putBoolean("askedLocation", value).apply()

    fun update(transform: (LauncherSettings) -> LauncherSettings) {
        val s = transform(_settings.value)
        sp.edit().apply {
            putString("driverSide", s.driverSide.name)
            putString("themeMode", s.themeMode.name)
            putInt("accentIndex", s.accentIndex)
            putBoolean("use24h", s.use24h)
            putString("carName", s.carName)
            putString("homeAddress", s.homeAddress)
            putString("workAddress", s.workAddress)
            s.tiles.forEachIndexed { i, pkg -> putString("tile$i", pkg) }
        }.apply()
        _settings.value = s
    }

    private fun load(): LauncherSettings {
        val d = LauncherSettings()
        return LauncherSettings(
            driverSide = enumOr(sp.getString("driverSide", null), d.driverSide),
            themeMode = enumOr(sp.getString("themeMode", null), d.themeMode),
            accentIndex = sp.getInt("accentIndex", d.accentIndex),
            use24h = sp.getBoolean("use24h", d.use24h),
            carName = sp.getString("carName", null) ?: d.carName,
            homeAddress = sp.getString("homeAddress", null) ?: d.homeAddress,
            workAddress = sp.getString("workAddress", null) ?: d.workAddress,
            tiles = List(TileRole.entries.size) { sp.getString("tile$it", "") ?: "" },
        )
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        name?.let { runCatching { enumValueOf<E>(it) }.getOrNull() } ?: default
}
