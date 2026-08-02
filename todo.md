# Pulse Music Player AI — v1.14.0 Update (JioSaavn Fix)

## CRITICAL FIX: JioSaavn / Desi Hits playback restored
- [x] Root cause: saavn.sumit.co /songs/{id} now returns "data" as an ARRAY,
      but getJioSaavnSongDetails() only parsed it as an OBJECT -> resolution
      returned null -> JioSaavn + Desi Hits songs never resolved / never played
- [x] Fix getJioSaavnSongDetails() to handle BOTH array and object shapes
      (plus nested data.results / data.songs)
- [x] Use httpGetSafe() for song details (no silent throw)
- [x] getTrending(): JioSaavn full songs first, Deezer as fallback
- [x] Verified full chain: search -> resolve -> playable 320kbps URL (end-to-end)
- [x] Bump version to 1.14.0 (versionCode 11400)

# Pulse Music Player AI — v1.13.0 Update

## Phase 0: SoundCloud Platform + JioSaavn Sync (v1.13.0)
- [x] Add searchSoundCloud() to YouTubeRepository.kt (free full tracks, public API v2)
- [x] Add dynamic SoundCloud client_id resolver + transcoding->stream resolver
- [x] Add searchSoundCloud() to YouTubeViewModel.kt
- [x] Add SOUNDCLOUD to MusicSource enum + filter chip + routing in YouTubeScreen.kt
- [x] Sync SoundCloud into searchAllSources() (JioSaavn kept first for full-song sync)
- [x] Resolve SoundCloud stream URL on-demand at playback (fast + slow path)
- [x] Bump version to 1.13.0 (versionCode 11300)

# Pulse Music Player AI — v1.10.0 Update

## Phase 1: JioSaavn Search Fix (COMPLETED in prior session)
- [x] Fix HTML entity decoding in JioSaavn API responses
- [x] Add httpGetSafe() to prevent silent failures
- [x] Apply decodeHtmlEntities() to title/artist names

## Phase 2: YouTube Music Trending (COMPLETED)
- [x] Add getYouTubeTrending() to YouTubeRepository.kt
- [x] Add loadYouTubeTrending() + state flows to YouTubeViewModel.kt
- [x] Add state collection + LaunchedEffect to YouTubeScreen.kt
- [x] Insert YouTube Music trending display block in YouTubeScreen.kt

## Phase 3: Apple Music Playback Fix
- [x] Verify resolveFullSong() works (depends on JioSaavn fix)
- [ ] Add better fallback: if JioSaavn resolution fails, try YouTube directly
- [ ] Improve isPreviewOnlySource() handling and user feedback

## Phase 4: Premium App Icon
- [ ] Redesign ic_launcher_foreground.xml with premium look
- [ ] Redesign ic_launcher_background.xml with gradient
- [ ] Verify mipmap/adaptive icon references

## Phase 5: Premium Animations
- [ ] Enhance SplashScreen with more premium animations
- [ ] Add smooth UI transition animations

## Phase 6: App Size 30-40 MB
- [ ] Add assets/resources to increase APK size to 30-40MB
- [ ] Verify release APK size

## Phase 7: Version Bump & Release
- [ ] Bump version to 1.10.0 (versionCode 11000) in build.gradle.kts
- [ ] Build debug + release APKs
- [ ] Commit, push, PR, merge
- [ ] Create GitHub Release v1.10.0 with APKs
