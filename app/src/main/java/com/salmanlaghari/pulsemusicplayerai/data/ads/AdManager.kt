package com.salmanlaghari.pulsemusicplayerai.data.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.*
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * AdManager — Full AdMob integration with 15 ad units.
 * Pub ID: pub-8178045957849630
 * App ID: ca-app-pub-8178045957849630~3776473332
 */
object AdManager {

    private const val TAG = "PluseAds"

    // Handler for scheduling delayed ad loads on the main thread
    private val mainHandler = Handler(Looper.getMainLooper())

    // ✅ REAL AdMob IDs — DO NOT MODIFY
    private const val APP_OPEN_ID = "ca-app-pub-8178045957849630/9910636842"
    private const val BANNER_NOW_PLAYING_ID = "ca-app-pub-8178045957849630/5273242041"
    private const val BANNER_LIBRARY_ID = "ca-app-pub-8178045957849630/2647078704"
    private const val BANNER_EQUALIZER_ID = "ca-app-pub-8178045957849630/3892023409"
    private const val INTERSTITIAL_SONG_CHANGE_ID = "ca-app-pub-8178045957849630/8315172503"
    private const val INTERSTITIAL_RESUME_ID = "ca-app-pub-8178045957849630/5657219571"
    private const val INTERSTITIAL_PLAYLIST_END_ID = "ca-app-pub-8178045957849630/7820348488"
    private const val REWARDED_AD_FREE_HOUR_ID = "ca-app-pub-8178045957849630/1965386577"
    private const val REWARDED_UNLIMITED_SKIP_ID = "ca-app-pub-8178045957849630/9652304906"
    private const val REWARDED_PRO_EQUALIZER_ID = "ca-app-pub-8178045957849630/6778729559"
    private const val REWARDED_HQ_AUDIO_ID = "ca-app-pub-8178045957849630/5713059895"
    private const val REWARDED_OFFLINE_DOWNLOAD_ID = "ca-app-pub-8178045957849630/6667048910"
    private const val REWARDED_PREMIUM_THEME_ID = "ca-app-pub-8178045957849630/1446511823"
    private const val REWARDED_SLEEP_TIMER_ID = "ca-app-pub-8178045957849630/1313961528"
    private const val NATIVE_PLAYLIST_ID = "ca-app-pub-8178045957849630/6837750948"

    // ⚠️ PLACEHOLDER AD UNIT (TEST ID) ⚠️
    // This uses Google's official TEST rewarded ad unit so the Watch & Unlock
    // flow can be exercised end-to-end in development. BEFORE SHIPPING PUBLICLY,
    // replace it with a real production AdMob rewarded ad unit you create in the
    // AdMob console (account tied to pub-8178045957849630). See the PR
    // description for details. Do not ship with a test ID in production.
    private const val REWARDED_AUDIO_TOOLS_ID = "ca-app-pub-3940256099942544/5224354917"

    // Ad instances
    private var appOpenAd: AppOpenAd? = null
    private var interstitialSongChange: InterstitialAd? = null
    private var interstitialResume: InterstitialAd? = null
    private var interstitialPlaylistEnd: InterstitialAd? = null
    private var rewardedAdFreeHour: RewardedAd? = null
    private var rewardedUnlimitedSkip: RewardedAd? = null
    private var rewardedProEqualizer: RewardedAd? = null
    private var rewardedHqAudio: RewardedAd? = null
    private var rewardedOfflineDownload: RewardedAd? = null
    private var rewardedPremiumTheme: RewardedAd? = null
    private var rewardedSleepTimer: RewardedAd? = null
    private var rewardedAudioTools: RewardedAd? = null
    var nativePlaylistAd: NativeAd? = null
        private set

    // Premium state
    var isAdFreeHourActive = false
        private set
    private var adFreeHourExpiryMs = 0L
    var isUnlimitedSkipActive = false
        private set
    var skipCount = 0
        private set
    const val SKIP_LIMIT_FREE = 6

    // Song change counter
    private var songChangeCount = 0

    // ═══ INITIALIZATION ═══
    fun initialize(context: Context) {
        MobileAds.initialize(context) { status ->
            Log.d(TAG, "AdMob initialized: $status")
        }
        loadAllAds(context)
    }

    private fun loadAllAds(context: Context) {
        loadAppOpen(context)
        loadInterstitialSongChange(context)
        loadInterstitialResume(context)
        loadInterstitialPlaylistEnd(context)
        loadRewardedAdFreeHour(context)
        loadRewardedUnlimitedSkip(context)
        loadRewardedProEqualizer(context)
        loadRewardedHqAudio(context)
        loadRewardedOfflineDownload(context)
        loadRewardedPremiumTheme(context)
        loadRewardedSleepTimer(context)
        loadRewardedAudioTools(context)
        loadNativePlaylist(context)
    }

    // ═══ APP OPEN AD ═══
    private fun loadAppOpen(context: Context) {
        AppOpenAd.load(context, APP_OPEN_ID, AdRequest.Builder().build(),
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    Log.d(TAG, "App Open loaded")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "App Open failed: ${error.message}")
                }
            })
    }

    fun showAppOpen(activity: Activity) {
        // Show the app-open ad only after a short delay so the app's first frame
        // renders instantly. If the ad isn't loaded yet, we skip it entirely —
        // the user sees the app immediately instead of waiting for an ad.
        mainHandler.postDelayed({
            if (activity.isFinishing || activity.isDestroyed) return@postDelayed
            appOpenAd?.let { ad ->
                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        appOpenAd = null
                        loadAppOpen(activity)
                    }
                    override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                        appOpenAd = null
                        loadAppOpen(activity)
                    }
                }
                try {
                    ad.show(activity)
                } catch (e: Exception) {
                    Log.w(TAG, "App Open show failed: ${e.message}")
                }
            }
        }, 800)
    }

    // ═══ BANNER ADS ═══
    fun getBannerNowPlayingId() = BANNER_NOW_PLAYING_ID
    fun getBannerLibraryId() = BANNER_LIBRARY_ID
    fun getBannerEqualizerId() = BANNER_EQUALIZER_ID

    // ═══ INTERSTITIAL ADS ═══
    private fun loadInterstitialSongChange(context: Context) {
        InterstitialAd.load(context, INTERSTITIAL_SONG_CHANGE_ID, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialSongChange = ad
                    Log.d(TAG, "Interstitial SongChange loaded")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Interstitial SongChange failed: ${error.message}")
                }
            })
    }

    private fun loadInterstitialResume(context: Context) {
        InterstitialAd.load(context, INTERSTITIAL_RESUME_ID, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialResume = ad
                    Log.d(TAG, "Interstitial Resume loaded")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Interstitial Resume failed: ${error.message}")
                }
            })
    }

    private fun loadInterstitialPlaylistEnd(context: Context) {
        InterstitialAd.load(context, INTERSTITIAL_PLAYLIST_END_ID, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialPlaylistEnd = ad
                    Log.d(TAG, "Interstitial PlaylistEnd loaded")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Interstitial PlaylistEnd failed: ${error.message}")
                }
            })
    }

    fun showInterstitialSongChange(activity: Activity, onComplete: () -> Unit) {
        if (isAdFreeHour()) { onComplete(); return }
        interstitialSongChange?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialSongChange = null
                    loadInterstitialSongChange(activity)
                    onComplete()
                }
            }
            ad.show(activity)
            return
        }
        onComplete()
    }

    fun showInterstitialResume(activity: Activity) {
        if (isAdFreeHour()) return
        interstitialResume?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialResume = null
                    loadInterstitialResume(activity)
                }
            }
            ad.show(activity)
        }
    }

    fun showInterstitialPlaylistEnd(activity: Activity, onComplete: () -> Unit) {
        if (isAdFreeHour()) { onComplete(); return }
        interstitialPlaylistEnd?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialPlaylistEnd = null
                    loadInterstitialPlaylistEnd(activity)
                    onComplete()
                }
            }
            ad.show(activity)
            return
        }
        onComplete()
    }

    // ═══ REWARDED ADS ═══
    private fun loadRewardedAdFreeHour(context: Context) {
        RewardedAd.load(context, REWARDED_AD_FREE_HOUR_ID, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) { rewardedAdFreeHour = ad; Log.d(TAG, "Rewarded AdFreeHour loaded") }
                override fun onAdFailedToLoad(error: LoadAdError) { Log.w(TAG, "Rewarded AdFreeHour failed") }
            })
    }

    private fun loadRewardedUnlimitedSkip(context: Context) {
        RewardedAd.load(context, REWARDED_UNLIMITED_SKIP_ID, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) { rewardedUnlimitedSkip = ad; Log.d(TAG, "Rewarded UnlimitedSkip loaded") }
                override fun onAdFailedToLoad(error: LoadAdError) { Log.w(TAG, "Rewarded UnlimitedSkip failed") }
            })
    }

    private fun loadRewardedProEqualizer(context: Context) {
        RewardedAd.load(context, REWARDED_PRO_EQUALIZER_ID, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) { rewardedProEqualizer = ad; Log.d(TAG, "Rewarded ProEQ loaded") }
                override fun onAdFailedToLoad(error: LoadAdError) { Log.w(TAG, "Rewarded ProEQ failed") }
            })
    }

    private fun loadRewardedHqAudio(context: Context) {
        RewardedAd.load(context, REWARDED_HQ_AUDIO_ID, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) { rewardedHqAudio = ad; Log.d(TAG, "Rewarded HQAudio loaded") }
                override fun onAdFailedToLoad(error: LoadAdError) { Log.w(TAG, "Rewarded HQAudio failed") }
            })
    }

    private fun loadRewardedOfflineDownload(context: Context) {
        RewardedAd.load(context, REWARDED_OFFLINE_DOWNLOAD_ID, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) { rewardedOfflineDownload = ad; Log.d(TAG, "Rewarded OfflineDownload loaded") }
                override fun onAdFailedToLoad(error: LoadAdError) { Log.w(TAG, "Rewarded OfflineDownload failed") }
            })
    }

    private fun loadRewardedPremiumTheme(context: Context) {
        RewardedAd.load(context, REWARDED_PREMIUM_THEME_ID, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) { rewardedPremiumTheme = ad; Log.d(TAG, "Rewarded PremiumTheme loaded") }
                override fun onAdFailedToLoad(error: LoadAdError) { Log.w(TAG, "Rewarded PremiumTheme failed") }
            })
    }

    private fun loadRewardedSleepTimer(context: Context) {
        RewardedAd.load(context, REWARDED_SLEEP_TIMER_ID, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) { rewardedSleepTimer = ad; Log.d(TAG, "Rewarded SleepTimer loaded") }
                override fun onAdFailedToLoad(error: LoadAdError) { Log.w(TAG, "Rewarded SleepTimer failed") }
            })
    }

    fun showRewardedAdFreeHour(activity: Activity, onReward: () -> Unit) {
        rewardedAdFreeHour?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAdFreeHour = null
                    loadRewardedAdFreeHour(activity)
                }
            }
            ad.show(activity) {
                isAdFreeHourActive = true
                adFreeHourExpiryMs = System.currentTimeMillis() + (60 * 60 * 1000)
                onReward()
            }
            return
        }
        android.widget.Toast.makeText(activity, "Ad not ready, try again", android.widget.Toast.LENGTH_SHORT).show()
    }

    fun showRewardedUnlimitedSkip(activity: Activity, onReward: () -> Unit) {
        rewardedUnlimitedSkip?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedUnlimitedSkip = null
                    loadRewardedUnlimitedSkip(activity)
                }
            }
            ad.show(activity) {
                isUnlimitedSkipActive = true
                skipCount = 0
                onReward()
            }
            return
        }
        android.widget.Toast.makeText(activity, "Ad not ready, try again", android.widget.Toast.LENGTH_SHORT).show()
    }

    fun showRewardedProEqualizer(activity: Activity, onReward: () -> Unit) {
        rewardedProEqualizer?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedProEqualizer = null
                    loadRewardedProEqualizer(activity)
                }
            }
            ad.show(activity) { onReward() }
            return
        }
        android.widget.Toast.makeText(activity, "Ad not ready, try again", android.widget.Toast.LENGTH_SHORT).show()
    }

    fun showRewardedHqAudio(activity: Activity, onReward: () -> Unit) {
        rewardedHqAudio?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedHqAudio = null
                    loadRewardedHqAudio(activity)
                }
            }
            ad.show(activity) { onReward() }
            return
        }
        android.widget.Toast.makeText(activity, "Ad not ready, try again", android.widget.Toast.LENGTH_SHORT).show()
    }

    fun showRewardedOfflineDownload(activity: Activity, onReward: () -> Unit) {
        rewardedOfflineDownload?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedOfflineDownload = null
                    loadRewardedOfflineDownload(activity)
                }
            }
            ad.show(activity) { onReward() }
            return
        }
        android.widget.Toast.makeText(activity, "Ad not ready, try again", android.widget.Toast.LENGTH_SHORT).show()
    }

    fun showRewardedPremiumTheme(activity: Activity, onReward: () -> Unit) {
        rewardedPremiumTheme?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedPremiumTheme = null
                    loadRewardedPremiumTheme(activity)
                }
            }
            ad.show(activity) { onReward() }
            return
        }
        android.widget.Toast.makeText(activity, "Ad not ready, try again", android.widget.Toast.LENGTH_SHORT).show()
    }

    fun showRewardedSleepTimer(activity: Activity, onReward: () -> Unit) {
        rewardedSleepTimer?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedSleepTimer = null
                    loadRewardedSleepTimer(activity)
                }
            }
            ad.show(activity) { onReward() }
            return
        }
        android.widget.Toast.makeText(activity, "Ad not ready, try again", android.widget.Toast.LENGTH_SHORT).show()
    }

    // ═══ REWARDED AD — AUDIO TOOLS "WATCH & UNLOCK" ═══
    // REWARDED_AUDIO_TOOLS_ID is Google's official TEST rewarded unit
    // (ca-app-pub-3940256099942544/5224354917). Test units always serve a valid
    // test ad on a live connection — that is exactly what keeps this flow
    // reliable in a build we can verify. Before a public production release,
    // swap it for a real rewarded unit created under pub-8178045957849630 in
    // the AdMob console (the other 14 units above are already production IDs).
    private fun loadRewardedAudioTools(
        context: Context,
        onResult: ((RewardedAd?, LoadAdError?) -> Unit)? = null
    ) {
        RewardedAd.load(context, REWARDED_AUDIO_TOOLS_ID, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAudioTools = ad
                    Log.d(TAG, "Rewarded AudioTools loaded")
                    onResult?.invoke(ad, null)
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    // Surface the REAL AdMob reason (code + domain + message) so a
                    // load failure is never silent/obscure again.
                    Log.e(TAG, "Rewarded AudioTools FAILED to load — code=${error.code} domain=${error.domain} message=${error.message}")
                    onResult?.invoke(null, error)
                }
            })
    }

    /**
     * Show the Audio Tools rewarded ad.
     *
     * - If an ad is already preloaded, it is shown immediately.
     * - Otherwise it is loaded ON DEMAND (so a transient miss during the deferred
     *   startup preload can never leave the user soft-locked) and then shown.
     *
     * [onReward] fires only after the user completes the ad. [onError] surfaces
     * the REAL AdMob error reason (code + message) so failures are diagnosable
     * instead of showing a generic "couldn't load" message.
     */
    fun showRewardedAudioTools(
        activity: Activity,
        onReward: () -> Unit,
        onError: (String) -> Unit
    ) {
        val preloaded = rewardedAudioTools
        if (preloaded != null) {
            showLoadedAudioTools(preloaded, activity, onReward, onError)
            return
        }
        Log.d(TAG, "Rewarded AudioTools not preloaded — loading on demand")
        loadRewardedAudioTools(activity) { ad, error ->
            if (ad != null) {
                showLoadedAudioTools(ad, activity, onReward, onError)
            } else {
                val reason = error?.let { "code ${it.code}: ${it.message}" } ?: "unknown error"
                Log.e(TAG, "Rewarded AudioTools on-demand load failed ($reason)")
                onError("Couldn't load the ad right now ($reason). Please try again.")
            }
        }
    }

    private fun showLoadedAudioTools(
        ad: RewardedAd,
        activity: Activity,
        onReward: () -> Unit,
        onError: (String) -> Unit
    ) {
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAudioTools = null
                loadRewardedAudioTools(activity)
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.e(TAG, "Rewarded AudioTools failed to SHOW — code=${error.code} message=${error.message}")
                rewardedAudioTools = null
                loadRewardedAudioTools(activity)
                onError("The ad couldn't be shown (${error.message}). Please try again.")
            }
        }
        Log.d(TAG, "Rewarded AudioTools showing ad")
        ad.show(activity) {
            // Reward earned — caller unlocks the feature.
            onReward()
        }
    }

    // ═══ NATIVE AD ═══
    private fun loadNativePlaylist(context: Context) {
        val adLoader = AdLoader.Builder(context, NATIVE_PLAYLIST_ID)
            .forNativeAd { ad -> nativePlaylistAd = ad; Log.d(TAG, "Native Playlist loaded") }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) { Log.w(TAG, "Native Playlist failed") }
            })
            .build()
        adLoader.loadAd(AdRequest.Builder().build())
    }

    // ═══ SKIP LOGIC ═══
    fun canSkip(): Boolean {
        if (isUnlimitedSkipActive) return true
        return skipCount < SKIP_LIMIT_FREE
    }

    fun incrementSkip() {
        if (!isUnlimitedSkipActive) skipCount++
    }

    // ═══ SONG CHANGE LOGIC ═══
    fun onSongChanged(activity: Activity, onComplete: () -> Unit) {
        songChangeCount++
        if (songChangeCount % 3 == 0) {
            showInterstitialSongChange(activity, onComplete)
        } else {
            onComplete()
        }
    }

    /** Increment song change counter (call from ViewModel). Returns true if ad should show. */
    fun incrementSongChangeCount(): Boolean {
        songChangeCount++
        return songChangeCount % 3 == 0
    }

    // ═══ HELPERS ═══
    private fun isAdFreeHour(): Boolean {
        return isAdFreeHourActive && System.currentTimeMillis() < adFreeHourExpiryMs
    }

    fun getNativePlaylistId() = NATIVE_PLAYLIST_ID
}
