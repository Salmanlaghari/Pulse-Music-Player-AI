# Proguard rules for Pulse Music Player AI

# 1. Keep Jetpack Compose classes and runtime targets
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# 2. Keep AndroidX Media3 (ExoPlayer, Session, UI)
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.session.** { *; }
-keep class androidx.media3.common.** { *; }
-keep class androidx.media3.datasource.** { *; }
-keep class androidx.media3.decoder.** { *; }
-keep class androidx.media3.extractor.** { *; }
-keep class androidx.media3.ui.** { *; }
-dontwarn androidx.media3.**

# Keep MediaSession and MediaSessionService JNI/Reflection targets
-keepclassmembers class * extends androidx.media3.session.MediaSessionService {
    public <init>();
}
-keepclassmembers class * extends androidx.media3.session.MediaSession {
    public <init>();
}

# 3. Keep Coil Compose Image Loader structures
-keep class coil.** { *; }
-dontwarn coil.**

# 4. Keep Kotlin Coroutines & Flow structures
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# 5. Keep DataStore Preferences serializers and cache targets
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# 6. Keep AndroidX Lifecycle and ViewModel targets
-keep class androidx.lifecycle.** { *; }
-dontwarn androidx.lifecycle.**

# 7. Keep native platform JNI callbacks
-keepclasseswithmembernames class * {
    native <methods>;
}
