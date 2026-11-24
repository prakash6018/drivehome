package com.drivehome.launcher.util

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Date

/** No ADB on this head unit, so crashes are saved to a file and shown in Diagnostics. */
object CrashLog {
    private const val FILE_NAME = "crash_log.txt"
    private const val MAX_CHARS = 20_000

    fun install(context: Context) {
        val file = File(context.filesDir, FILE_NAME)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }
                val old = if (file.exists()) file.readText().takeLast(MAX_CHARS) else ""
                file.writeText("$old=== ${Date()} [${thread.name}] ===\n$trace\n")
            }
            previous?.uncaughtException(thread, error)
        }
    }

    fun read(context: Context): String =
        File(context.filesDir, FILE_NAME).takeIf { it.exists() }?.readText().orEmpty()

    fun clear(context: Context) {
        File(context.filesDir, FILE_NAME).delete()
    }
}
