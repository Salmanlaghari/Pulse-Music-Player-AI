# Pulse Music Player AI – Session 1 (Project Foundation)

Welcome to **Pulse Music Player AI**, a professional, modern, and production-ready Android audio application built using the latest industry-standard technologies and architecture principles.

This repository serves as the complete project foundation (Session 1) of the Pulse Music Player AI suite.

---

## 🛠️ Technology Stack & Best Practices

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3 Design Guidelines)
- **Architecture**: MVVM (Model-View-ViewModel) + Clean Architecture Principles
- **Build System**: Gradle Kotlin DSL + Version Catalog (`libs.versions.toml`)
- **Navigation**: Official Jetpack Compose Navigation
- **Minimum SDK**: API 24 (Android 7.0)
- **Target SDK**: Latest Stable (API 35)

---

## 📂 Project Architecture & Directory Structure

The project strictly adheres to **Clean Architecture** and **MVVM** design patterns. The codebase is organized into dedicated functional directories under the package root `com.salmanlaghari.pulsemusicplayerai`:

- 📦 **`app`**: The application's entry point and application class configuration.
- 📦 **`core`**: Base services and background processing interfaces.
- 📦 **`data`**: Local and remote data repositories, scanners, and preferences.
- 📦 **`domain`**: Business rules, core interfaces, and model representations.
- 📦 **`presentation`**: UI screens and view-model states divided by features:
  - `splash` (Animated branding intro)
  - `home` (Dynamic dashboards & welcome content)
  - `library` (Sound library & storage scanning)
  - `audiotools` (High-fidelity sound labs & editors)
  - `aihub` (Advanced smart prompt catalogs)
  - `settings` (User theme preferences & legal docs)
- 📦 **`navigation`**: Centralized route mapping and Compose transition hosts.
- 📦 **`ui`**: Custom graphic canvases, drawing tools, and shared UI assets.
- 📦 **`theme`**: Professional Material 3 color system definitions, typography, and shape scales.
- 📦 **`utils`**: High-performance helpers and extension functions.
- 📦 **`common`**: Reusable component cards and generic utilities.

---

## 🚀 Key Features Implemented

1. **Modern Splashtastic Entry**: Features an immersive, animated fade-in, overshoot scale, and smooth radial-glow pulse effect.
2. **Beautiful Home Dashboard**: Complete with Welcome cards, Recently Played carousels, Trending Playlist cards, Quick Access grid, and a floating Mini Player control.
3. **High-Fidelity Audio Studio Placeholders**: Designed to support MP3 Cutting, Audio Merging, Formats Converting, MP3 to MP4 Visualizers, Audio Extractors, Compressors, and Pitch/Speed Shapers.
4. **Interactive AI Hub**: Layout ready for AI Prompts, AI Generator, Neural Music Assistant, Lyrics Syncer, and futuristic voice, image, and video renderers.
5. **Seamless Bottom Navigation**: Integrated bottom tab bars with responsive feedback, custom active indicators, and robust backstack preservation.
6. **Advanced Color System & Dark Mode**: Embedded user theme preference manager powered by `androidx.datastore` allowing manual/automatic theme selection.

---

## 🔨 Compiling and Verifying the Project

To compile and verify the project, run the following Gradle task:

```bash
gradle compileDebugKotlin --no-daemon
```

*Note: All code compiles perfectly with zero warnings or deprecation blockages.*
