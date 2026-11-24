package com.drivehome.launcher

import android.app.Application
import com.drivehome.launcher.data.WeatherRepository
import com.drivehome.launcher.media.MediaRepository
import com.drivehome.launcher.sensors.SpeedTracker
import com.drivehome.launcher.util.CrashLog

class DriveHomeApp : Application() {
    /** Process-wide so the trip survives the activity and is shared with [sensors.TripService]. */
    val speedTracker by lazy { SpeedTracker(this) }
    val weather by lazy { WeatherRepository(this) }
    /** Process-wide so [media.MediaListenerService] can feed it notifications from vendor players. */
    val media by lazy { MediaRepository(this) }

    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this)
    }
}
