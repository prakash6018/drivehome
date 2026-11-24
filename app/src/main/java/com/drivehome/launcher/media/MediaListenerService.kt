package com.drivehome.launcher.media

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.drivehome.launcher.DriveHomeApp

/**
 * Being an enabled notification listener unlocks getActiveSessions(). It also lets us see
 * notifications from vendor players (radio, Bluetooth music) that have no media session.
 */
class MediaListenerService : NotificationListenerService() {
    private val media get() = (application as DriveHomeApp).media

    override fun onListenerConnected() {
        runCatching { activeNotifications }.getOrNull()?.forEach(media::onNotificationPosted)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) = media.onNotificationPosted(sbn)

    override fun onNotificationRemoved(sbn: StatusBarNotification) = media.onNotificationRemoved(sbn)
}
