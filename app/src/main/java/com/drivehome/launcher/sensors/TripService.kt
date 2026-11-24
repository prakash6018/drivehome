package com.drivehome.launcher.sensors

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.drivehome.launcher.DriveHomeApp
import com.drivehome.launcher.R

/**
 * Keeps GPS running while another app (e.g. Maps) is in front. Without a foreground service
 * Android throttles background location to a few fixes an hour and the trip would stop counting.
 */
class TripService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val tracker = (application as DriveHomeApp).speedTracker
        if (!tracker.hasPermission()) {
            stopSelf()
            return START_NOT_STICKY
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Trip computer", NotificationManager.IMPORTANCE_MIN)
        )
        val notification = Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_navigation)
            .setContentTitle("DriveHome trip computer")
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(ID, notification)
        }
        tracker.start()
        return START_STICKY
    }

    override fun onDestroy() {
        (application as DriveHomeApp).speedTracker.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private companion object {
        const val CHANNEL = "trip"
        const val ID = 1
    }
}
