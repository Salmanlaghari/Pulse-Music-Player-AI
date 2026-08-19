package com.salmanlaghari.pulsemusicplayerai.presentation.youtube

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.salmanlaghari.pulsemusicplayerai.theme.*

/**
 * Plays a video from the owner's YouTube channel using YouTube's OFFICIAL,
 * ToS-compliant IFrame Player API (rendered inside a WebView). This keeps
 * YouTube's own UI/controls and never strips the video to fake "audio-only"
 * playback.
 *
 * Root cause of the previous "tap a video → nothing plays" bug:
 * the old implementation loaded a bare <iframe src="…embed/ID?autoplay=1">
 * directly. Inside an Android WebView, YouTube's embed frequently refuses to
 * autoplay (no direct user gesture on the WebView surface) and there was no
 * fallback, so the player opened but stayed paused/blank.
 *
 * Fix: drive playback through the official YT IFrame Player API and call
 * player.playVideo() from onReady(), which reliably starts playback for a
 * WebView with mediaPlaybackRequiresUserGesture=false. We also expose a
 * manual "Tap to play" overlay that invokes playVideo() via JS, guaranteeing
 * a user-gesture path when autoplay is blocked, and we log any load/player
 * errors so failures are visible instead of silent.
 */
private const val TAG_CHANNEL_PLAYER = "ChannelPlayer"

@Composable
fun ChannelPlayerScreen(
    videoId: String,
    title: String,
    channelName: String,
    onNavigateBack: () -> Unit
) {
    // Overlay state: shown until the player signals it's ready (or on error).
    var showPlayOverlay by remember { mutableStateOf(true) }
    var playerError by remember { mutableStateOf<String?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BaseDeepNavy, BaseNavyBlue, BaseNavyBlue)
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = "Now Playing — My Channel",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier.padding(start = 48.dp)
                )
            }

            if (videoId.isBlank()) {
                // Defensive guard: never ship a blank/black embed.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "This video is unavailable right now.",
                        color = TextDim,
                        fontSize = 14.sp
                    )
                }
            } else {
                // Embedded official YouTube player (16:9)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black)
                ) {
                    Box {
                        AndroidView(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            factory = { context ->
                                val webView = YouTubePlayerWebView(
                                    context = context,
                                    videoId = videoId,
                                    onReady = { showPlayOverlay = false },
                                    onError = { msg -> playerError = msg }
                                )
                                webViewRef = webView
                                webView
                            },
                            onRelease = { it.destroy() }
                        )

                        // Manual play fallback — guarantees a user-gesture path if
                        // autoplay is blocked by the WebView. Tapping it forces
                        // playVideo() via JS and dismisses the overlay.
                        if (showPlayOverlay && playerError == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .clickable {
                                        webViewRef?.evaluateJavascript("playVideoWeb();", null)
                                        showPlayOverlay = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Tap to play",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(CyanGlow.copy(alpha = 0.85f))
                                        .padding(12.dp)
                                )
                            }
                        }

                        // Error surfaced to the user instead of a silent black box.
                        if (playerError != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .background(Color.Black.copy(alpha = 0.85f))
                                    .clickable {
                                        webViewRef?.evaluateJavascript("playVideoWeb();", null)
                                        playerError = null
                                        showPlayOverlay = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = CyanGlow,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    androidx.compose.foundation.layout.Spacer(
                                        modifier = Modifier.height(8.dp)
                                    )
                                    Text(
                                        text = "Tap to retry playback",
                                        color = Color.White,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Metadata — matches the app's now-playing style
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = channelName,
                    fontSize = 14.sp,
                    color = CyanGlow,
                    fontWeight = FontWeight.Medium
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = GlassBg)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        androidx.compose.foundation.layout.Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = CyanGlow,
                                modifier = Modifier.size(18.dp)
                            )
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "Playing via YouTube's official player",
                                fontSize = 12.sp,
                                color = TextDim
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * JS bridge so the IFrame Player API can notify the Compose layer when the
 * player is ready (hide the manual-play overlay) or when playback errors out.
 */
private class YouTubePlayerBridge(
    private val onReady: () -> Unit,
    private val onError: (String) -> Unit
) {
    @JavascriptInterface
    fun onReady() {
        onReady()
    }

    @JavascriptInterface
    fun onError(code: String) {
        onError("Playback error (code $code)")
    }
}

/**
 * Builds a WebView that loads YouTube's official IFrame Player API for
 * [videoId] and starts playback from onReady(). Autoplay is enabled; the user's
 * tap on the channel list item is what opened this screen, and the manual
 * "Tap to play" overlay provides an explicit gesture if the embed still blocks
 * autoplay.
 */
@SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
private fun YouTubePlayerWebView(
    context: android.content.Context,
    videoId: String,
    onReady: () -> Unit,
    onError: (String) -> Unit
): WebView {
    return WebView(context).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.loadWithOverviewMode = true
        // Allow the embedded YouTube iframe to autoplay without requiring a direct
        // gesture on the WebView surface (the user's tap on the list item opened it).
        settings.mediaPlaybackRequiresUserGesture = false
        webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                val msg = error?.description?.toString() ?: "unknown error"
                Log.e(TAG_CHANNEL_PLAYER, "WebView error loading player: $msg")
                onError("Couldn't load the video player ($msg)")
            }
        }
        webChromeClient = WebChromeClient()

        // Bridge to receive player-ready / error callbacks from JS.
        addJavascriptInterface(YouTubePlayerBridge(onReady, onError), "Android")

        val safeVideoId = videoId.trim()
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <style>
                html,body{margin:0;padding:0;background:#000;height:100%;overflow:hidden}
                #player{position:absolute;top:0;left:0;width:100%;height:100%}
              </style>
            </head>
            <body>
              <div id="player"></div>
              <script>
                var player;
                function onYouTubeIframeAPIReady() {
                  player = new YT.Player('player', {
                    videoId: '$safeVideoId',
                    playerVars: {
                      autoplay: 1, rel: 0, modestbranding: 1,
                      playsinline: 1, controls: 1
                    },
                    events: {
                      onReady: function(e) {
                        try { e.target.playVideo(); } catch (err) {}
                        if (window.Android) Android.onReady();
                      },
                      onError: function(e) {
                        if (window.Android) Android.onError(String(e.data));
                      }
                    }
                  });
                }
                function playVideoWeb() {
                  if (player && player.playVideo) {
                    try { player.playVideo(); } catch (err) {}
                  }
                }
              </script>
              <script src="https://www.youtube.com/iframe_api"></script>
            </body>
            </html>
        """.trimIndent()

        loadDataWithBaseURL(
            "https://www.youtube.com",
            html,
            "text/html",
            "utf-8",
            null
        )
    }
}
