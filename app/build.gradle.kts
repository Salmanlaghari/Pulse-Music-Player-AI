plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.salmanlaghari.pulsemusicplayerai"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.salmanlaghari.pulsemusicplayerai"
        minSdk = 24
        targetSdk = 35
        versionCode = 11500
        versionName = "1.15.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // YouTube Data API v3 key for listing "My Channel" uploads. Leave blank to
        // fall back to the key-free public RSS feed. Supply via the
        // YOUTUBE_DATA_API_KEY Gradle property (e.g. from a CI secret) so the key
        // is never committed to the repo.
        val youTubeDataApiKey = providers.gradleProperty("YOUTUBE_DATA_API_KEY").orNull ?: ""
        buildConfigField("String", "YOUTUBE_DATA_API_KEY", "\"${youTubeDataApiKey}\"")
    }

    signingConfigs {
        create("release") {
            // Release signing is driven entirely by CI secrets (RELEASE_KEYSTORE_*)
            // so a single, permanent keystore signs every release build — keeping
            // the SHA-1/SHA-256 fingerprints stable for Play Store / Firebase / OAuth.
            // The raw keystore file and passwords are never committed to the repo.
            val keystoreFile = System.getenv("RELEASE_KEYSTORE_FILE")
                ?: "keystore/pulse-release.keystore"
            storeFile = file(keystoreFile)
            storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("RELEASE_KEY_ALIAS") ?: "pulse-release"
            keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Sign release with a proper release keystore so Google Play Protect
            // does not flag it as an unsigned/debug app.
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        // Avoid lint errors from AdMob SDK and third-party libs
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui)
    implementation(libs.coil.compose)

    // AdMob (22.x for stable API compatibility)
    implementation("com.google.android.gms:play-services-ads:22.6.0")

    // Required for Unit Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
// v1.12.0 session 9 stable build trigger
