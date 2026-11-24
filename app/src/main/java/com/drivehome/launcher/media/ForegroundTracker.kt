package com.drivehome.launcher.media

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process

/**
 * Finds which app was last brought to the front, e.g. the radio opened with the steering
 * MODE button. Needs the optional "Usage access" permission; without it, only apps opened
 * from DriveHome itself are known.
 */
class ForegroundTracker(private val context: Context) {
    private val usage = context.getSystemService(UsageStatsManager::class.java)
    private var checkedUntil = System.currentTimeMillis() - LOOKBACK_MS

    // Both variants are deprecated in the newest SDK but are the supported check on Android 8–13.
    @Suppress("DEPRECATION")
    fun hasAccess(): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= 29) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Most recent app (matching [filter]) that came to the front since the last call. */
    fun latestSince(filter: (String) -> Boolean): Pair<String, Long>? {
        if (!hasAccess()) return null
        val end = System.currentTimeMillis()
        val events = runCatching { usage.queryEvents(checkedUntil, end) }.getOrNull() ?: return null
        checkedUntil = end
        val e = UsageEvents.Event()
        var latest: Pair<String, Long>? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            if (e.eventType == RESUMED && filter(e.packageName)) latest = e.packageName to e.timeStamp
        }
        return latest
    }

    private companion object {
        const val LOOKBACK_MS = 6 * 60 * 60_000L
        // ACTIVITY_RESUMED (API 29) has the same value as the older MOVE_TO_FOREGROUND.
        @Suppress("DEPRECATION")
        val RESUMED = if (Build.VERSION.SDK_INT >= 29) UsageEvents.Event.ACTIVITY_RESUMED else UsageEvents.Event.MOVE_TO_FOREGROUND
    }
}
