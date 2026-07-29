# Proguard rules for Pulse Music Player

# AdMob
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.gms.ads.appopen.** { *; }
-keep class com.google.android.gms.ads.interstitial.** { *; }
-keep class com.google.android.gms.ads.rewarded.** { *; }
-keep class com.google.android.gms.ads.nativead.** { *; }
-keep class com.google.android.gms.ads.query.** { *; }

# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Coil
-keep class coil.** { *; }
-dontwarn coil.**

# Compose
-dontwarn androidx.compose.**

# Kotlin Coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Keep data classes
-keep class com.salmanlaghari.pulsemusicplayerai.domain.model.** { *; }
