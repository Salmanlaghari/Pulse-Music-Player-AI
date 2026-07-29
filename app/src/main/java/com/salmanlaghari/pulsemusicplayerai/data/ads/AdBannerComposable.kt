package com.salmanlaghari.pulsemusicplayerai.data.ads

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Compose-compatible AdMob Banner component.
 * Use in any screen with: AdMobBanner(adUnitId = AdManager.getBannerLibraryId())
 */
@Composable
fun AdMobBanner(
    adUnitId: String,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            AdView(context).apply {
                this.adUnitId = adUnitId
                setAdSize(AdSize.BANNER)
                loadAd(AdRequest.Builder().build())
            }
        },
        update = { adView ->
            adView.adUnitId = adUnitId
            adView.loadAd(AdRequest.Builder().build())
        }
    )
}
