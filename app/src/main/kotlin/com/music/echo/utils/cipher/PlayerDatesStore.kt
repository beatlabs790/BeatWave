package com.music.echo.utils.cipher

import android.content.Context
import android.util.Base64
import com.music.innertube.YouTube
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import timber.log.Timber
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Cosmetic "when did we add cipher support for this player" dates, shown in the song-details
 * sheet next to the player hash.
 *
 * Pulled **purely from a remote file** on the cipher repo — `player_dates.json` is NOT bundled
 * in the APK, so adding a date is just a push to that file and already-installed apps pick it
 * up with no APK update. A small on-disk cache makes it instant/offline on later launches.
 *
 * Deliberately decoupled from [PlayerConfigStore] and the decipher path: it is a separate file
 * old apps never fetch (so it cannot affect them), it is parsed tolerantly, and every failure
 * (no network, bad JSON, no cache yet) just yields an unknown date — playback is never touched.
 *
 * File shape — a flat map, no schemaVersion, no validation:
 *   { "959dabb2": "2026-06-12", "445213fb": "2026-06-10", ... }
 */
object PlayerDatesStore {
    private const val TAG = "EchoMusic_CipherDates"

    private val REMOTE_URL by lazy {
        val encoded = "aHR0cHM6Ly9yYXcuZ2l0aHVidXNlcmNvbnRlbnQuY29tL0VjaG9NdXNpY0FwcC9FY2hvLU11c2ljL21haW4vYXBwL3NyYy9tYWluL2Fzc2V0cy9wbGF5ZXJfZGF0ZXMuanNvbg=="
        String(Base64.decode(encoded, Base64.DEFAULT), StandardCharsets.UTF_8)
    }

    // Own dir, NOT the shared cipher_cache (PlayerJsFetcher purges/wipes that one).
    private const val CACHE_DIR = "cipher_dates"
    private const val CACHE_FILE = "player_dates.json"
    private const val META_FILE = "player_dates.meta"

    @Volatile
    private var dates: Map<String, String> = emptyMap()

    private val _lastFetchTimeMs = MutableStateFlow<Long?>(null)
    val lastFetchTimeMs: StateFlow<Long?> = _lastFetchTimeMs.asStateFlow()
    
    private var appContext: Context? = null

    /** Tolerant parse of a flat `hash -> date` object. Non-string values are skipped; never throws. */
    internal fun parse(text: String): Map<String, String> =
        runCatching {
            val root = Json.parseToJsonElement(text) as? JsonObject ?: return emptyMap()
            buildMap {
                for ((hash, value) in root) {
                    (value as? JsonPrimitive)?.takeIf { it.isString }?.content?.let { put(hash, it) }
                }
            }
        }.getOrDefault(emptyMap())

    /** Load the last-fetched cache (instant/offline), then refresh from the remote file in the background. */
    fun initialize(context: Context) {
        appContext = context.applicationContext
        val cacheDir = File(context.filesDir, CACHE_DIR).apply { mkdirs() }
        val cache = File(cacheDir, CACHE_FILE)
        val meta = File(cacheDir, META_FILE)

        dates = runCatching {
            if (cache.exists()) parse(cache.readText()) else emptyMap()
        }.getOrDefault(emptyMap())
        
        _lastFetchTimeMs.value = runCatching { 
            if (meta.exists()) meta.readText().toLongOrNull() else null 
        }.getOrNull()

        Thread {
            runCatching {
                val body = fetchRemote()
                val remote = parse(body)
                if (remote.isNotEmpty()) {
                    dates = remote // the remote file is the single source of truth
                    runCatching { 
                        cache.writeText(body) 
                        val now = System.currentTimeMillis()
                        meta.writeText(now.toString())
                        _lastFetchTimeMs.value = now
                    } // persist for the next launch / offline
                }
            }.onFailure { Timber.tag(TAG).d("dates refresh skipped: ${it.message}") }
        }.apply { isDaemon = true; name = "PlayerDatesRefresh" }.start()
    }

    /**
     * Manual user-triggered refresh from Settings.
     */
    suspend fun forceManualRefresh(): Boolean = withContext(Dispatchers.IO) {
        val context = appContext ?: return@withContext false
        runCatching {
            val body = fetchRemote()
            val remote = parse(body)
            if (remote.isNotEmpty()) {
                dates = remote
                val cacheDir = File(context.filesDir, CACHE_DIR).apply { mkdirs() }
                File(cacheDir, CACHE_FILE).writeText(body)
                val now = System.currentTimeMillis()
                File(cacheDir, META_FILE).writeText(now.toString())
                _lastFetchTimeMs.value = now
                true
            } else {
                false
            }
        }.getOrDefault(false)
    }

    /** Onboarding date for [hash] (`YYYY-MM-DD`), or null if unknown. */
    fun get(hash: String?): String? = hash?.let { dates[it] }

    private fun fetchRemote(): String {
        val url = URL(REMOTE_URL)
        val proxy = YouTube.proxy
        val conn = (if (proxy != null) url.openConnection(proxy) else url.openConnection()) as HttpURLConnection
        return try {
            conn.run {
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("User-Agent", "Mozilla/5.0")
                inputStream.bufferedReader().use { it.readText() }
            }
        } finally {
            conn.disconnect() // release the socket immediately, including on the error path
        }
    }
}
