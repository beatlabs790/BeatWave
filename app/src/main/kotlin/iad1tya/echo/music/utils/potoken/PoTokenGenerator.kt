package iad1tya.echo.music.utils.potoken

import android.webkit.CookieManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

class PoTokenGenerator {
    private val TAG = "PoTokenGenerator"

    private val webViewSupported by lazy { runCatching { CookieManager.getInstance() }.isSuccess }
    private var webViewBadImpl = false

    private val webPoTokenGenLock = Mutex()
    private var webPoTokenSessionId: String? = null
    private var webPoTokenStreamingPot: String? = null
    private var webPoTokenGenerator: PoTokenWebView? = null

    fun getWebClientPoToken(videoId: String, sessionId: String): PoTokenResult? {
        Timber.tag(TAG).d("getWebClientPoToken called: videoId=$videoId, sessionId=$sessionId")
        if (!webViewSupported || webViewBadImpl) {
            Timber.tag(TAG).d("WebView not available: supported=$webViewSupported, badImpl=$webViewBadImpl")
            return null
        }

        return try {
            runBlocking { getWebClientPoTokenInternal(videoId, sessionId, forceRecreate = false) }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "poToken generation exception: ${e.javaClass.simpleName}: ${e.message}")
            when (e) {
                is BadWebViewException -> {
                    Timber.tag(TAG).e(e, "Could not obtain poToken because WebView is broken")
                    webViewBadImpl = true
                    null
                }
                else -> throw e
            }
        }
    }

    private suspend fun getWebClientPoTokenInternal(
        videoId: String,
        sessionId: String,
        forceRecreate: Boolean,
    ): PoTokenResult {
        Timber.tag(TAG).d("Web poToken requested: videoId=$videoId, sessionId=$sessionId")

        val (poTokenGenerator, streamingPot, hasBeenRecreated) =
            webPoTokenGenLock.withLock {
                val shouldRecreate =
                    forceRecreate ||
                        webPoTokenGenerator == null ||
                        webPoTokenGenerator!!.isExpired ||
                        webPoTokenSessionId != sessionId

                if (shouldRecreate) {
                    Timber.tag(TAG).d("Creating new PoTokenWebView (forceRecreate=$forceRecreate)")
                    webPoTokenSessionId = sessionId

                    withContext(Dispatchers.Main) {
                        webPoTokenGenerator?.close()
                    }

                    webPoTokenGenerator = PoTokenWebView.getNewPoTokenGenerator(AppContextHolder.appContext)

                    webPoTokenStreamingPot = webPoTokenGenerator!!.generatePoToken(webPoTokenSessionId!!)
                    Timber.tag(TAG).d("Streaming poToken generated")
                }

                Triple(webPoTokenGenerator!!, webPoTokenStreamingPot!!, shouldRecreate)
            }

        val playerPot = try {
            poTokenGenerator.generatePoToken(videoId)
        } catch (throwable: Throwable) {
            if (hasBeenRecreated) {
                throw throwable
            } else {
                Timber.tag(TAG).e(throwable, "Failed to obtain poToken, retrying")
                return getWebClientPoTokenInternal(
                    videoId = videoId,
                    sessionId = sessionId,
                    forceRecreate = true,
                )
            }
        }

        // Log base64 lengths to help verify token size (valid = ~148-172 chars for 110-128 raw bytes)
        Timber.tag(TAG).d("poToken generated: playerLen=${playerPot.length} streamingLen=${streamingPot.length}")
        Timber.tag(TAG).d("poToken player=${playerPot.take(20)}..., streaming=${streamingPot.take(20)}...")
        return PoTokenResult(playerPot, streamingPot)
    }
}
