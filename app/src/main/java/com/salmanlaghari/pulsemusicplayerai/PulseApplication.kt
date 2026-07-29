package com.salmanlaghari.pulsemusicplayerai

import android.app.Application
import android.util.Log
import com.salmanlaghari.pulsemusicplayerai.data.ads.AdManager

class PulseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("PulseApp", "Application started")

        // Initialize AdMob and load all 15 ad units
        AdManager.initialize(this)
    }
}
