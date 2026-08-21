package com.salmanlaghari.pulsemusicplayerai

import android.app.Application
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.salmanlaghari.pulsemusicplayerai.data.ads.AdManager
import com.salmanlaghari.pulsemusicplayerai.utils.CrashLogger
import com.salmanlaghari.pulsemusicplayerai.presentation.ui.CrashDetailsActivity

class PulseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("PulseApp", "Application started")

        setupGlobalCrashHandler()

        Handler(Looper.getMainLooper()).postDelayed({
            try {
                AdManager.initialize(this)
            } catch (e: Exception) {
                Log.w("PulseApp", "AdManager init deferred failed: ${e.message}")
            }
        }, 2000)
    }

    private fun setupGlobalCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                CrashLogger.logException(throwable, "GLOBAL_UNCAUGHT")
                val intent = Intent(this, CrashDetailsActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra(CrashDetailsActivity.EXTRA_CRASH_MESSAGE, throwable.message)
                    putExtra(CrashDetailsActivity.EXTRA_CRASH_CLASS, throwable.javaClass.name)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("PulseApp", "Crash handler failed: ${e.message}", e)
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }
}
