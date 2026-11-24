package com.drivehome.launcher.data

import android.content.Context
import android.location.Geocoder
import android.location.Location
import androidx.annotation.DrawableRes
import com.drivehome.launcher.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.roundToInt

data class Weather(
    val tempC: Int,
    val highC: Int,
    val lowC: Int,
    val condition: String,
    @param:DrawableRes val icon: Int,
    val fetchedAt: Long,
)

/** Area name shown on the map widget, e.g. "Koramangala" / "Bengaluru". */
data class Place(val area: String, val city: String)

private const val REFRESH_MS = 30 * 60_000L

/**
 * Weather from Open-Meteo (free, no API key) and the area name from Android's Geocoder.
 * Both need internet; on failure the widgets simply keep showing their placeholders.
 */
class WeatherRepository(private val context: Context) {
    private val _weather = MutableStateFlow<Weather?>(null)
    val weather: StateFlow<Weather?> = _weather
    private val _place = MutableStateFlow<Place?>(null)
    val place: StateFlow<Place?> = _place

    suspend fun refreshIfStale(location: Location?) {
        if (location == null) return
        val w = _weather.value
        if (w == null || System.currentTimeMillis() - w.fetchedAt > REFRESH_MS) {
            runCatching { fetchWeather(location) }.onSuccess { _weather.value = it }
        }
        if (_place.value == null) {
            runCatching { resolvePlace(location) }.onSuccess { if (it != null) _place.value = it }
        }
    }

    private suspend fun fetchWeather(loc: Location): Weather = withContext(Dispatchers.IO) {
        val url = URL(
            "https://api.open-meteo.com/v1/forecast?latitude=%.3f&longitude=%.3f".format(Locale.US, loc.latitude, loc.longitude) +
                "&current=temperature_2m,weather_code,is_day&daily=temperature_2m_max,temperature_2m_min" +
                "&timezone=auto&forecast_days=1"
        )
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
        }
        val json = try {
            JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
        } finally {
            conn.disconnect()
        }
        val current = json.getJSONObject("current")
        val daily = json.getJSONObject("daily")
        val code = current.getInt("weather_code")
        val (condition, icon) = describe(code, isDay = current.optInt("is_day", 1) == 1)
        Weather(
            tempC = current.getDouble("temperature_2m").roundToInt(),
            highC = daily.getJSONArray("temperature_2m_max").getDouble(0).roundToInt(),
            lowC = daily.getJSONArray("temperature_2m_min").getDouble(0).roundToInt(),
            condition = condition,
            icon = icon,
            fetchedAt = System.currentTimeMillis(),
        )
    }

    @Suppress("DEPRECATION") // The async variant is API 33+; this runs on IO anyway.
    private suspend fun resolvePlace(loc: Location): Place? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null
        val a = Geocoder(context, Locale.getDefault()).getFromLocation(loc.latitude, loc.longitude, 1)
            ?.firstOrNull() ?: return@withContext null
        val area = a.subLocality ?: a.thoroughfare ?: a.locality ?: return@withContext null
        Place(area, a.locality ?: a.subAdminArea ?: a.adminArea.orEmpty())
    }

    /** WMO weather codes → label + icon. */
    private fun describe(code: Int, isDay: Boolean): Pair<String, Int> = when (code) {
        0 -> (if (isDay) "Clear" else "Clear night") to (if (isDay) R.drawable.ic_sun else R.drawable.ic_moon)
        1, 2 -> "Partly cloudy" to R.drawable.ic_cloud
        3 -> "Overcast" to R.drawable.ic_cloud
        45, 48 -> "Fog" to R.drawable.ic_fog
        in 51..67, in 80..82 -> "Rain" to R.drawable.ic_rain
        in 71..77, 85, 86 -> "Snow" to R.drawable.ic_cloud
        in 95..99 -> "Thunderstorm" to R.drawable.ic_storm
        else -> "—" to R.drawable.ic_cloud
    }
}
