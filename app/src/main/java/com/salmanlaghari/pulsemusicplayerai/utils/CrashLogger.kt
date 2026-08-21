package com.salmanlaghari.pulsemusicplayerai.utils

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashLogger {
    private const val TAG = "CrashLogger"
    private const val FILE_NAME = "crash_log.txt"
    private const val MAX_LOG_BYTES = 512 * 1024

    private var lastCrashFile: File? = null

    fun getLogFile(context: Context): File {
        val externalDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getExternalFilesDir(null)
        } else {
            context.getExternalFilesDir(null)
        }
        return File(externalDir, FILE_NAME)
    }

    fun logException(throwable: Throwable, tag: String = "Crash") {
        try {
            val logFile = getLogFile(null)
            ensureParentExists(logFile)
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val header = buildString {
                append("\n========================================")
                append("\n[$timestamp] $tag")
                append("\nException: ${throwable.javaClass.name}: ${throwable.message}")
                append("\nThread: ${Thread.currentThread().name}")
                append("\n----------------------------------------\n")
                append(sw.toString())
                append("\n")
            }
            appendToFile(logFile, header)
            Log.e(TAG, header)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write crash log: ${e.message}", e)
        }
    }

    fun logMessage(message: String, tag: String = "Crash") {
        try {
            val logFile = getLogFile(null)
            ensureParentExists(logFile)
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val line = "\n[$timestamp] [$tag] $message\n"
            appendToFile(logFile, line)
            Log.e(TAG, line)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write crash log message: ${e.message}", e)
        }
    }

    fun readLog(context: Context): String {
        return try {
            val logFile = getLogFile(context)
            if (!logFile.exists()) "No crash log available." else logFile.readText()
        } catch (e: Exception) {
            "Failed to read crash log: ${e.message}"
        }
    }

    fun clearLog(context: Context) {
        try {
            getLogFile(context).delete()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear crash log: ${e.message}", e)
        }
    }

    private fun ensureParentExists(file: File) {
        val parent = file.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }
    }

    private fun appendToFile(file: File, content: String) {
        if (file.exists() && file.length() > MAX_LOG_BYTES) {
            file.writeText("")
        }
        file.appendText(content)
    }
}
