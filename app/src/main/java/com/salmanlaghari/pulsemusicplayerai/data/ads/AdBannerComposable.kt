package com.salmanlaghari.pulsemusicplayerai.data.ads

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * Compose-compatible AdMob Banner component with robust error handling.
 * Use in any screen with: AdMobBanner(adUnitId = AdManager.getBannerLibraryId())
 */
@Composable
fun AdMobBanner(
    adUnitId: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color.Transparent)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                AdView(ctx).apply {
                    this.adUnitId = adUnitId
                    setAdSize(AdSize.BANNER)
                    adListener = object : AdListener() {
                        override fun onAdFailedToLoad(error: LoadAdError) {
                            Log.w("AdMobBanner", "Ad load failed: ${error.message}")
                        }
                        override fun onAdOpened() {
                            Log.d("AdMobBanner", "Ad opened")
                        }
                        override fun onAdClosed() {
                            Log.d("AdMobBanner", "Ad closed")
                        }
                    }
                    loadAd(AdRequest.Builder().build())
                }
            },
            update = { adView ->
                // Refresh ad on recomposition if needed
                if (adView.parent != null) {
                    try {
                        adView.loadAd(AdRequest.Builder().build())
                    } catch (e: Exception) {
                        Log.e("AdMobBanner", "Failed to update AdView: ${e.message}")
                    }
                }
            }
        )
    }
}
