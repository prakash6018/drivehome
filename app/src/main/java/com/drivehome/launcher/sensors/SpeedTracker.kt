package com.drivehome.launcher.sensors

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToInt

data class SpeedReading(val kmh: Int, val atMillis: Long)

/** Trip since the head unit powered on (it boots with the ignition). */
data class TripStats(
    val distanceMeters: Float = 0f,
    val movingMillis: Long = 0L,
    val maxKmh: Int = 0,
) {
    val avgKmh: Int
        get() = if (movingMillis < 60_000L) 0 else (distanceMeters / 1000f / (movingMillis / 3_600_000f)).roundToInt()
}

// Below this the car is treated as parked, so GPS jitter doesn't add phantom kilometres.
private const val MOVING_KMH = 3f
private const val MAX_ACCURACY_M = 40f

/** GPS speed and trip computer from the head unit's own GPS antenna. One per process. */
class SpeedTracker(private val context: Context) {
    private val locationManager = context.getSystemService(LocationManager::class.java)
    private val _reading = MutableStateFlow<SpeedReading?>(null)
    val reading: StateFlow<SpeedReading?> = _reading
    private val _trip = MutableStateFlow(TripStats())
    val trip: StateFlow<TripStats> = _trip
    private val _location = MutableStateFlow<Location?>(null)
    val location: StateFlow<Location?> = _location

    private var running = false
    private var lastFix: Location? = null

    // Full object (not a lambda): on Android 9 and older these methods are abstract.
    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) = onFix(location)
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        override fun onProviderEnabled(provider: String) = Unit
        override fun onProviderDisabled(provider: String) { _reading.value = null }
    }

    fun hasPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun start() {
        if (running || !hasPermission()) return
        if (_location.value == null) {
            _location.value = runCatching {
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }.getOrNull()
        }
        running = runCatching {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 1000L, 0f, listener, Looper.getMainLooper()
            )
        }.isSuccess
    }

    fun stop() {
        if (!running) return
        locationManager.removeUpdates(listener)
        running = false
    }

    private fun onFix(fix: Location) {
        _location.value = fix
        if (!fix.hasSpeed()) return
        val kmh = fix.speed * 3.6f
        _reading.value = SpeedReading(kmh.roundToInt(), System.currentTimeMillis())

        val prev = lastFix
        lastFix = fix
        if (prev == null || kmh < MOVING_KMH || (fix.hasAccuracy() && fix.accuracy > MAX_ACCURACY_M)) return
        val dt = (fix.time - prev.time).coerceIn(0L, 5_000L)
        val t = _trip.value
        _trip.value = t.copy(
            distanceMeters = t.distanceMeters + prev.distanceTo(fix),
            movingMillis = t.movingMillis + dt,
            maxKmh = maxOf(t.maxKmh, kmh.roundToInt()),
        )
    }
}
