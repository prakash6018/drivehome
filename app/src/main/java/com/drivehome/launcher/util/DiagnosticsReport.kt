package com.drivehome.launcher.util

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.drivehome.launcher.data.AppEntry
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DiagnosticsReport {

    fun build(activity: Activity, apps: List<AppEntry>, mediaAccess: Boolean, hasLocation: Boolean, mediaInfo: String): String =
        buildString {
            val pm = activity.packageManager
            val version = runCatching { pm.getPackageInfo(activity.packageName, 0).versionName }.getOrNull()
            appendLine("DriveHome $version diagnostics — ${Date()}")

            appendLine("\n== Device ==")
            appendLine("Manufacturer: ${Build.MANUFACTURER}   Brand: ${Build.BRAND}")
            appendLine("Model: ${Build.MODEL}   Device: ${Build.DEVICE}   Product: ${Build.PRODUCT}")
            appendLine("Board: ${Build.BOARD}   Hardware: ${Build.HARDWARE}")
            if (Build.VERSION.SDK_INT >= 31) appendLine("SoC: ${Build.SOC_MANUFACTURER} ${Build.SOC_MODEL}")
            appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("Build: ${Build.DISPLAY}")
            appendLine("Fingerprint: ${Build.FINGERPRINT}")

            appendLine("\n== Display ==")
            val dm = activity.resources.displayMetrics
            if (Build.VERSION.SDK_INT >= 30) {
                val b = activity.windowManager.maximumWindowMetrics.bounds
                appendLine("Full screen: ${b.width()} x ${b.height()} px")
            }
            appendLine("App area: ${dm.widthPixels} x ${dm.heightPixels} px")
            appendLine("Density: ${dm.densityDpi} dpi (x${dm.density})")
            appendLine("App area in dp: ${(dm.widthPixels / dm.density).toInt()} x ${(dm.heightPixels / dm.density).toInt()}")

            appendLine("\n== Permissions ==")
            appendLine("Song details (notification access): ${if (mediaAccess) "yes" else "no"}")
            appendLine("GPS: ${if (hasLocation) "yes" else "no"}")
            appendLine("GPS hardware: ${pm.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)}")

            appendLine("\n== Media sources ==")
            append(mediaInfo)

            appendLine("\n== Home apps ==")
            val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val default = pm.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo
            pm.queryIntentActivities(homeIntent, 0).forEach { ri ->
                val info = ri.activityInfo
                val mark = if (info.packageName == default?.packageName && info.name == default.name) "  [default]" else ""
                appendLine("- ${ri.loadLabel(pm)}  ${info.packageName}/${info.name}$mark")
            }

            appendLine("\n== Apps (${apps.size}) ==")
            apps.forEach { appendLine("- ${it.label}  ${it.packageName}/${it.activityName}") }

            appendLine("\n== Crash log ==")
            appendLine(CrashLog.read(activity).ifBlank { "(none)" })
        }

    /** Writes the report to Download/ so it can be copied to a USB stick. Returns where it went. */
    fun export(activity: Activity, report: String): String {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val name = "DriveHome-report-$stamp.txt"
        if (Build.VERSION.SDK_INT >= 29) {
            val resolver = activity.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("Could not create file in Downloads")
            resolver.openOutputStream(uri)!!.use { it.write(report.toByteArray()) }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return "${Environment.DIRECTORY_DOWNLOADS}/$name"
        }
        val file = File(activity.getExternalFilesDir(null), name)
        file.writeText(report)
        return file.absolutePath
    }
}
