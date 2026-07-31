package com.salmanlaghari.pulsemusicplayerai

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.salmanlaghari.pulsemusicplayerai.data.ads.AdManager

class PulseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("PulseApp", "Application started")

        // Defer AdMob initialization to after the first frame renders.
        // Loading 15 ad units simultaneously during Application.onCreate() blocks
        // the main thread and causes the app to feel frozen/hung on startup.
        // By posting to the main looper with a 2s delay, the splash screen and
        // first UI frame render instantly, and ads initialize in the background.
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                AdManager.initialize(this)
            } catch (e: Exception) {
                Log.w("PulseApp", "AdManager init deferred failed: ${e.message}")
            }
        }, 2000)
    }
}
