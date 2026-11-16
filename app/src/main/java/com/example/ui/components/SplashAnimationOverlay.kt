package com.example.ui.components

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

private class AndroidSplashBridge(
    private val onFinished: () -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onSplashComplete() {
        mainHandler.postDelayed({
            onFinished()
        }, 280L)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SplashAnimationOverlay(
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val latestOnComplete by rememberUpdatedState(onAnimationComplete)
    var hasCompleted by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val triggerComplete = remember {
        {
            if (!hasCompleted) {
                hasCompleted = true
                latestOnComplete()
            }
        }
    }

    // Safety fallback timeout so the app never stays blocked on splash
    LaunchedEffect(Unit) {
        delay(4200L)
        triggerComplete()
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.apply {
                stopLoading()
                removeJavascriptInterface("AndroidSplash")
                destroy()
            }
            webViewRef = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("splash_animation_overlay")
    ) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(android.graphics.Color.BLACK)
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    overScrollMode = View.OVER_SCROLL_NEVER

                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true

                    addJavascriptInterface(
                        AndroidSplashBridge { triggerComplete() },
                        "AndroidSplash"
                    )

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            view?.evaluateJavascript(
                                """
                                (function() {
                                  var btn = document.getElementById('pe-replay-btn');
                                  if (btn) btn.style.display = 'none';
                                })();
                                """.trimIndent(),
                                null
                            )
                        }
                    }

                    loadUrl("file:///android_asset/aistudio/parkeasy_splash_screen_animation_aab75e0a.html")
                    webViewRef = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        TextButton(
            onClick = { triggerComplete() },
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.textButtonColors(
                containerColor = Color.White.copy(alpha = 0.10f),
                contentColor = Color.White.copy(alpha = 0.85f)
            ),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 12.dp, end = 16.dp)
                .testTag("splash_skip_button")
        ) {
            Text(
                text = "Skip",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
