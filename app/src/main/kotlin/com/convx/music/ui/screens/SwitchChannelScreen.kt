/**
 * BeatWave Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convx.music.ui.screens

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import android.widget.Toast
import com.music.innertube.YouTube
import com.convx.music.LocalPlayerAwareWindowInsets
import com.convx.music.R
import com.convx.music.constants.DataSyncIdKey
import com.convx.music.ui.component.IconButton
import com.convx.music.ui.utils.backToMain
import com.convx.music.utils.rememberPreference

/**
 * Lets an already-logged-in user switch which YouTube channel on their Google
 * Account the app acts as, without repeating the whole sign-in flow.
 *
 * Deliberately has NO Compose UI drawn over the WebView (no button, no
 * overlay) — that's what made [LoginScreen] glitch and eat touches when it
 * sat over YouTube's own in-page content (e.g. the Premium upsell card).
 * Instead this just opens music.youtube.com (already logged in via the
 * existing cookie jar) and watches DATASYNC_ID: the moment the user taps
 * their own profile picture and picks a different channel — pure YouTube UI,
 * nothing of ours drawn on top of it — YouTube reloads the page with a new
 * DATASYNC_ID, which this detects and saves automatically. No manual
 * confirm step needed.
 */
@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwitchChannelScreen(navController: NavController) {
    val context = LocalContext.current
    var dataSyncId by rememberPreference(DataSyncIdKey, "")
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    var switched by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AndroidView(
            modifier = Modifier
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                .fillMaxSize(),
            factory = { webViewContext ->
                WebView(webViewContext).apply {
                    settings.javaScriptEnabled = true
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String?) {
                            loadUrl("javascript:Android.onRetrieveDataSyncId(window.yt.config_.DATASYNC_ID)")
                        }
                    }
                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun onRetrieveDataSyncId(newDataSyncId: String?) {
                            if (newDataSyncId != null && newDataSyncId.isNotEmpty() && newDataSyncId != dataSyncId) {
                                dataSyncId = newDataSyncId
                                YouTube.dataSyncId = newDataSyncId
                                switched = true
                                Toast.makeText(context, context.getString(R.string.switch_channel_success), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }, "Android")
                    webViewRef.value = this
                    loadUrl("https://music.youtube.com")
                }
            }
        )

        TopAppBar(
            title = { Text(stringResource(if (switched) R.string.switch_channel_switched_title else R.string.switch_channel_title)) },
            navigationIcon = {
                IconButton(
                    onClick = navController::navigateUp,
                    onLongClick = navController::backToMain
                ) {
                    Icon(
                        painterResource(R.drawable.arrow_back),
                        contentDescription = null
                    )
                }
            }
        )
    }

    BackHandler(enabled = webViewRef.value?.canGoBack() == true) {
        webViewRef.value?.goBack()
    }
}
