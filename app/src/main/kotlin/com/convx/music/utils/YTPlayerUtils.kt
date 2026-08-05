/**
 * BeatWave Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convx.music.utils

import android.net.ConnectivityManager
import android.util.Log
import androidx.media3.common.PlaybackException
import com.music.innertube.NewPipeExtractor
import com.music.innertube.YouTube
import com.music.innertube.models.YouTubeClient
import com.music.innertube.models.YouTubeClient.Companion.ANDROID_CREATOR
import com.convx.music.utils.BotDetectionMitigator
import com.music.innertube.models.YouTubeClient.Companion.ANDROID_VR_1_43_32
import com.music.innertube.models.YouTubeClient.Companion.ANDROID_VR_1_61_48
import com.music.innertube.models.YouTubeClient.Companion.ANDROID_VR_NO_AUTH
import com.music.innertube.models.YouTubeClient.Companion.IOS
import com.music.innertube.models.YouTubeClient.Companion.IPADOS
import com.music.innertube.models.YouTubeClient.Companion.MOBILE
import com.music.innertube.models.YouTubeClient.Companion.TVHTML5
import com.music.innertube.models.YouTubeClient.Companion.TVHTML5_SIMPLY_EMBEDDED_PLAYER
import com.music.innertube.models.YouTubeClient.Companion.WEB
import com.music.innertube.models.YouTubeClient.Companion.WEB_CREATOR
import com.music.innertube.models.YouTubeClient.Companion.WEB_REMIX
import com.music.innertube.models.response.PlayerResponse
import com.convx.music.constants.AudioQuality
import com.convx.music.constants.EnableSaavnStreamingKey
import com.convx.music.constants.EnableTidalStreamingKey
import com.convx.music.constants.EnabledModulesKey
import com.convx.music.constants.ModuleSourcesKey
import com.convx.music.constants.ModuleSettingsKey
import com.convx.music.constants.SaavnAudioQuality
import com.convx.music.constants.SaavnAudioQualityKey
import com.convx.music.constants.TidalInstanceUrlKey
import com.convx.music.constants.TidalQuality
import com.convx.music.constants.TidalQualityKey
import com.convx.music.utils.tidal.TidalService
import com.music.spine.ModuleManager
import org.json.JSONArray
import com.convx.music.utils.cipher.CipherDeobfuscator
import com.convx.music.utils.YTPlayerUtils.MAIN_CLIENT
import com.convx.music.utils.YTPlayerUtils.STREAM_FALLBACK_CLIENTS
import com.convx.music.utils.YTPlayerUtils.validateStatus
import com.convx.music.utils.potoken.PoTokenGenerator
import com.convx.music.utils.potoken.PoTokenResult
import com.convx.music.utils.sabr.EjsNTransformSolver
import com.convx.music.utils.PlaybackLogLevel
import com.convx.music.utils.PlaybackLogManager
import com.music.innertube.models.IpVersion
import com.music.innertube.models.WatchEndpoint
import com.music.jiosaavn.SaavnService
import okhttp3.Dns
import okhttp3.OkHttpClient
import timber.log.Timber
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import java.io.IOException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

object YTPlayerUtils {
    private const val logTag = "YTPlayerUtils"
    private const val TAG = "YTPlayerUtils"

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .dns(object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                val addresses = Dns.SYSTEM.lookup(hostname)
                return when (YouTube.ipVersion) {
                    IpVersion.IPV4 -> addresses.filter { it is Inet4Address }.ifEmpty { addresses }
                    IpVersion.IPV6 -> addresses.filter { it is Inet6Address }.ifEmpty { addresses }
                    IpVersion.AUTO -> addresses
                }
            }
        })
        .proxySelector(object : ProxySelector() {
            override fun select(uri: URI?): List<Proxy> = listOfNotNull(YouTube.proxy ?: Proxy.NO_PROXY)
            override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {
                Timber.tag(TAG).e(ioe, "Proxy connection failed for URI: $uri")
            }
        })
        .proxyAuthenticator { _, response ->
            YouTube.proxyAuth?.let { auth ->
                response.request.newBuilder()
                    .header("Proxy-Authorization", auth)
                    .build()
            } ?: response.request
        }
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val poTokenGenerator = PoTokenGenerator()

    /**
     * Client used for fast, low-latency stream resolution.
     * ANDROID_VR clients don't require PoToken and start instantly.
     * Note: ANDROID_VR has loginSupported=false, so metadata like audioConfig and
     * playbackTracking must be supplemented from an authenticated client (WEB_REMIX)
     * when the user is logged in.
     */
    private val MAIN_CLIENT: YouTubeClient = ANDROID_VR_1_43_32

    /**
     * Client used to fetch metadata (audioConfig, playbackTracking) when the user is
     * logged in. This ensures remote YouTube history is correctly updated.
     */
    private val METADATA_CLIENT: YouTubeClient = WEB_REMIX

    private val STREAM_FALLBACK_CLIENTS: Array<YouTubeClient> = arrayOf(
        ANDROID_VR_1_61_48,
        WEB_REMIX,
        TVHTML5_SIMPLY_EMBEDDED_PLAYER,  // Try embedded player first for age-restricted content
        TVHTML5,
        ANDROID_CREATOR,
        IPADOS,
        ANDROID_VR_NO_AUTH,
        MOBILE,
        IOS,
        WEB,
        WEB_CREATOR
    )
    data class PlaybackData(
        val audioConfig: PlayerResponse.PlayerConfig.AudioConfig?,
        val videoDetails: PlayerResponse.VideoDetails?,
        val playbackTracking: PlayerResponse.PlaybackTracking?,
        val format: PlayerResponse.StreamingData.Format,
        val streamUrl: String,
        val streamExpiresInSeconds: Int,
        /** True when the stream is sourced from JioSaavn (not YouTube). */
        val isSaavnStream: Boolean = false,
        /** True when the stream is lossless FLAC sourced from TIDAL (hifi-api). */
        val isTidalStream: Boolean = false,
        /** True when the stream is sourced from an 8spine module. */
        val isSpineStream: Boolean = false,
    )
    /**
     * Custom player response intended to use for playback.
     * Stream URLs come from [MAIN_CLIENT] or [STREAM_FALLBACK_CLIENTS] for fast loading.
     * Metadata (audioConfig, playbackTracking) come from [METADATA_CLIENT] (WEB_REMIX)
     * when the user is logged in, to ensure remote history recording works correctly.
     */
    suspend fun playerResponseForPlayback(
        videoId: String,
        playlistId: String? = null,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
        context: android.content.Context? = null,
        /** Downloads pass false so the offline copy stays YouTube; playback allows lossless. */
        allowLossless: Boolean = true,
        /** Set when a specific track has already failed lossless playback too many
         *  times (stall/parsing errors) — skips the Spine and Tidal intercepts
         *  entirely and resolves the plain YouTube stream for this call only. */
        forceStandardAudio: Boolean = false,
    ): Result<PlaybackData> {
        // ── JioSaavn intercept ───────────────────────────────────────────────
        // If the user has enabled JioSaavn streaming, try to resolve the stream
        // URL from JioSaavn first. We fall through to YouTube on ANY failure so
        // the user always hears audio.
        if (context != null) {
            // ── 8spine module intercept ─────────────────────────────────────────
            // Try enabled 8spine modules for streaming before other sources.
            // Falls through to TIDAL/Saavn/YouTube on ANY failure.
            if (!forceStandardAudio) {
            Log.d(TAG, "═══ SPINE INTERCEPT START ═══ videoId=$videoId")
            val enabledModulesJson = context.dataStore.get(EnabledModulesKey, "[]")
            val moduleSourcesJson = context.dataStore.get(ModuleSourcesKey, "[]")
            val moduleSettingsJson = context.dataStore.get(ModuleSettingsKey, "{}")
            Log.d(TAG, "  enabledModules: $enabledModulesJson")
            Log.d(TAG, "  moduleSources: $moduleSourcesJson")
            Log.d(TAG, "  moduleSettings: $moduleSettingsJson")
            runCatching {
                val enabledIds = runCatching {
                    val arr = JSONArray(enabledModulesJson)
                    (0 until arr.length()).map { arr.getString(it) }.toSet()
                }.getOrElse { emptySet<String>() }

                val sourceUrls = runCatching {
                    val arr = JSONArray(moduleSourcesJson)
                    (0 until arr.length()).map { arr.getString(it) }
                }.getOrElse { emptyList<String>() }

                val allModuleSettings = runCatching {
                    val obj = org.json.JSONObject(moduleSettingsJson)
                    obj.keys().asSequence().associateWith { key ->
                        val inner = obj.optJSONObject(key)
                        if (inner != null) {
                            inner.keys().asSequence().associateWith { k -> inner.optString(k, "") }
                        } else emptyMap()
                    }
                }.getOrElse { emptyMap<String, Map<String, String>>() }

                Log.d(TAG, "  Parsed enabledIds: $enabledIds")
                Log.d(TAG, "  Parsed sourceUrls: $sourceUrls")
                Log.d(TAG, "  Parsed allModuleSettings: $allModuleSettings")

                if (enabledIds.isEmpty() || sourceUrls.isEmpty()) {
                    Log.d(TAG, "  SPINE SKIP: enabledIds=${enabledIds.size} sourceUrls=${sourceUrls.size} — falling through")
                    Log.d(TAG, "═══ SPINE INTERCEPT END (skipped) ═══")
                }

                if (enabledIds.isNotEmpty() && sourceUrls.isNotEmpty()) {
                    Log.d(TAG, "  Resolving YouTube metadata for search query...")

                    val (currentSong, meta) = coroutineScope {
                        val nextDeferred = async {
                            val nextResult = YouTube.next(WatchEndpoint(videoId = videoId)).getOrNull()
                            nextResult?.items?.getOrNull(nextResult.currentIndex ?: 0)
                                ?: nextResult?.items?.firstOrNull()
                        }
                        val metaDeferred = async { playerResponseForMetadata(videoId, playlistId).getOrNull() }
                        nextDeferred.await() to metaDeferred.await()
                    }

                    val title = currentSong?.title ?: meta?.videoDetails?.title.orEmpty()
                    Log.d(TAG, "  Resolved title=\"$title\" from ${if (currentSong != null) "YouTube.next" else "videoDetails"}")
                    if (title.isNotBlank()) {
                        val artistNames: List<String> = if (currentSong?.artists?.isNotEmpty() == true) {
                            currentSong.artists.map { it.name }
                        } else {
                            listOf(
                                meta?.videoDetails?.author.orEmpty()
                                    .replace(Regex("(?i)\\s*-\\s*topic\\b"), "")
                                    .replace(Regex("(?i)\\s*VEVO\\b"), "")
                                    .trim()
                            ).filter { it.isNotBlank() }
                        }
                        var cleanTitle = title
                        for (artist in artistNames) {
                            cleanTitle = cleanTitle.replace(Regex("(?i)^\\s*${Regex.escape(artist)}\\s*[-–—]\\s*"), "")
                        }
                        // Strip YouTube noise that confuses search queries
                        cleanTitle = cleanTitle
                            .replace(Regex("(?i)\\(\\s*official\\s*(video|music\\s*video|audio)?\\s*\\)"), "")
                            .replace(Regex("(?i)\\(\\s*lyrics?\\s*\\)"), "")
                            .replace(Regex("(?i)\\[.*?\\]"), "")
                            .replace(Regex("[\u266A\u266B]"), "")
                            .replace(Regex("\\s+"), " ").trim()
                        val query = "$cleanTitle ${artistNames.joinToString(" ")}"
                            .replace("&", " ").replace(",", " ")
                            .replace(Regex("\\s+"), " ").trim()

                        Log.d(TAG, "  Search query: \"$query\" (artists=$artistNames)")

                        val moduleManager = ModuleManager()
                        for ((sourceIdx, sourceUrl) in sourceUrls.withIndex()) {
                            Log.d(TAG, "  ── Source [${sourceIdx + 1}/${sourceUrls.size}]: $sourceUrl")
                            val modules = moduleManager.fetchIndex(sourceUrl).getOrElse { e ->
                                Log.e(TAG, "  ✗ Failed to fetch index from $sourceUrl: ${e.message}")
                                continue
                            }
                            Log.d(TAG, "  ✓ Fetched ${modules.size} modules from $sourceUrl")
                            for ((modIdx, module) in modules.withIndex()) {
                                if (module.id !in enabledIds) {
                                    Log.d(TAG, "    [${modIdx + 1}] SKIP ${module.id} (not enabled)")
                                    continue
                                }
                                Log.d(TAG, "    [${modIdx + 1}/${modules.size}] TRY ${module.id} — isLossless=${module.isLossless} hasHiRes=${module.hasHiRes} isAtmos=${module.isDolbyAtmos}")
                                val spineResult = runCatching {
                                    val loadBaseUrl = sourceUrl.substringBeforeLast("/")
                                    Log.d(TAG, "      Loading module JS from baseUrl=$loadBaseUrl")
                                    val loaded = moduleManager.loadModule(module) { loadBaseUrl }.getOrElse { e ->
                                        Log.e(TAG, "      ✗ Failed to load module ${module.id}: ${e.message}")
                                        return@runCatching null
                                    }

                                    val moduleSettings = allModuleSettings[module.id] ?: emptyMap()
                                    Log.d(TAG, "      Module settings: $moduleSettings")
                                    Log.d(TAG, "      Calling searchTracks(\"$query\", 5, settings)...")
                                    var searchResult = moduleManager.searchTracks(loaded, query, 5, moduleSettings).getOrElse { e ->
                                        Log.e(TAG, "      ✗ Search failed for module ${module.id}: ${e.message}")
                                        return@runCatching null
                                    }
                                    // Retry with artist-first query if no matches (helps GEOLIER2/Apple fallback)
                                    if (searchResult.tracks.isEmpty() && artistNames.isNotEmpty()) {
                                        val retryQuery = "${artistNames.first()} $cleanTitle"
                                            .replace("&", " ").replace(",", " ")
                                            .replace(Regex("\\s+"), " ").trim()
                                        Log.d(TAG, "      No results, retrying with artist-first: \"$retryQuery\"")
                                        searchResult = moduleManager.searchTracks(loaded, retryQuery, 5, moduleSettings).getOrElse { e ->
                                            Log.e(TAG, "      ✗ Retry search failed: ${e.message}")
                                            return@runCatching null
                                        }
                                    }
                                    val matchedTrack = searchResult.tracks.minByOrNull { track ->
                                        val t = track.title.lowercase().trim()
                                        val a = track.artist.lowercase().trim()
                                        val q = cleanTitle.lowercase().trim()
                                        val artistsLower = artistNames.map { it.lowercase().trim() }
                                        var score = 0
                                        if (q in t || t in q) score -= 10
                                        if (artistsLower.any { it in a || a in it }) score -= 5
                                        val trackDur = track.duration
                                        val songDur = currentSong?.duration
                                        if (trackDur != null && songDur != null) {
                                            val diff = Math.abs(trackDur - songDur)
                                            if (diff < 5) score -= 3
                                        }
                                        score
                                    }
                                    if (matchedTrack == null) {
                                        Log.d(TAG, "      ✗ No tracks found by module ${module.id}")
                                        return@runCatching null
                                    }

                                    Log.d(TAG, "      ✓ Match: \"${matchedTrack.title}\" by \"${matchedTrack.artist}\" id=${matchedTrack.id} quality=${matchedTrack.audioQuality} duration=${matchedTrack.duration}s")

                                    Log.d(TAG, "      Calling getStreamUrl(\"${matchedTrack.id}\", settings)...")
                                    val streamResult = moduleManager.getStreamUrl(loaded, matchedTrack.id, moduleSettings).getOrElse { e ->
                                        Log.e(TAG, "      ✗ Stream URL fetch failed for module ${module.id} track ${matchedTrack.id}: ${e.message}")
                                        return@runCatching null
                                    }
                                    val streamUrl = streamResult.streamUrl?.ifBlank { null }
                                    if (streamUrl == null) {
                                        Log.d(TAG, "      ✗ Empty stream URL from module ${module.id}")
                                        Log.d(TAG, "      Full streamResult: streamUrl=${streamResult.streamUrl} track=${streamResult.track}")
                                        return@runCatching null
                                    }

                                    Log.d(TAG, "      ✓ Stream URL: ${streamUrl.take(150)}...")
                                    Log.d(TAG, "      Track audioQuality: ${streamResult.track?.audioQuality}")
                                    Log.d(TAG, "      Track mimeType: ${streamResult.track?.mimeType}")
                                    Log.d(TAG, "      Track audioModes: ${streamResult.track?.audioModes}")
                                    Log.d(TAG, "      Track bitDepth: ${streamResult.track?.bitDepth}")
                                    Log.d(TAG, "      Track sampleRate: ${streamResult.track?.sampleRate}")

                                    val isAtmosSupported = run {
                                        val list = android.media.MediaCodecList(android.media.MediaCodecList.ALL_CODECS)
                                        list.codecInfos.any { !it.isEncoder && it.supportedTypes.any { t -> t.contains("eac3", ignoreCase = true) } }
                                    }

                                    val isAtmos = isAtmosSupported && (module.isDolbyAtmos ||
                                        streamResult.track?.audioQuality?.uppercase()?.contains("ATMOS") == true ||
                                        streamResult.track?.audioModes?.any { it.uppercase().contains("ATMOS") } == true ||
                                        streamResult.track?.mimeType?.uppercase()?.contains("EAC3") == true)

                                    if (!isAtmosSupported && (streamResult.track?.audioQuality?.uppercase()?.contains("ATMOS") == true)) {
                                        Log.w(TAG, "      ! Device does not support EAC3/Atmos - will attempt fallback to FLAC/High")
                                    }

                                    val isLossless = !isAtmos && (
                                        module.isLossless ||
                                        streamResult.track?.audioQuality?.uppercase()?.contains("LOSSLESS") == true ||
                                        streamResult.track?.audioQuality?.uppercase()?.contains("HIRES") == true ||
                                        streamResult.track?.audioQuality?.uppercase()?.contains("FLAC") == true
                                    )

                                    Log.d(TAG, "      isAtmos=$isAtmos (module.isDolbyAtmos=${module.isDolbyAtmos} audioQuality=${streamResult.track?.audioQuality} audioModes=${streamResult.track?.audioModes} mimeType=${streamResult.track?.mimeType})")
                                    Log.d(TAG, "      isLossless=$isLossless (module.isLossless=${module.isLossless})")

                                    val mimeType = when {
                                        isAtmos -> "audio/eac3-joc; codecs=ec-3"
                                        isLossless -> "audio/flac; codecs=\"flac\""
                                        else -> "audio/mpeg; codecs=mp3"
                                    }
                                    val bitrate = when {
                                        isAtmos -> 768_000
                                        isLossless -> 1_411_000
                                        else -> 320_000
                                    }

                                    Log.d(TAG, "      ✓ STREAMING from module ${module.id}: mimeType=$mimeType bitrate=$bitrate itag=${if (isAtmos) 9997 else if (isLossless) 9998 else 9996}")
                                    PlaybackData(
                                        audioConfig      = meta?.playerConfig?.audioConfig,
                                        videoDetails     = meta?.videoDetails,
                                        playbackTracking = meta?.playbackTracking,
                                        format           = PlayerResponse.StreamingData.Format(
                                            itag             = when {
                                                isAtmos -> 9997
                                                isLossless -> 9998
                                                else -> 9996
                                            },
                                            url              = streamUrl,
                                            mimeType         = mimeType,
                                            bitrate          = bitrate,
                                            width            = null,
                                            height           = null,
                                            contentLength    = null,
                                            quality          = when {
                                                isAtmos -> "DOLBY_ATMOS"
                                                isLossless -> "LOSSLESS"
                                                else -> "HIGH"
                                            },
                                            fps              = null,
                                            qualityLabel     = null,
                                            averageBitrate   = null,
                                            audioQuality     = when {
                                                isAtmos -> "DOLBY_ATMOS"
                                                isLossless -> "LOSSLESS"
                                                else -> "HIGH"
                                            },
                                            approxDurationMs = matchedTrack.duration?.let { (it * 1000L).toString() },
                                            audioSampleRate  = when {
                                                isAtmos -> 48000
                                                isLossless -> 44100
                                                else -> null
                                            },
                                            audioChannels    = when {
                                                isAtmos -> 6
                                                isLossless -> 2
                                                else -> null
                                            },
                                            loudnessDb       = null,
                                            lastModified     = null,
                                            signatureCipher  = null,
                                            cipher           = null,
                                            audioTrack       = null,
                                        ),
                                        streamUrl              = streamUrl,
                                        streamExpiresInSeconds = 3600,
                                        isSpineStream          = true,
                                    )
                                }.getOrNull()

                                if (spineResult != null) return Result.success(spineResult)
                            }
                        }
                    }
                }
            }.onFailure { e ->
                Log.e(TAG, "═══ SPINE INTERCEPT ERROR ═══", e)
            }
            Log.d(TAG, "═══ SPINE INTERCEPT END ═══")
            } // !forceStandardAudio
            // ── End 8spine intercept ───────────────────────────────────────────

            // ── Lossless (TIDAL) intercept ───────────────────────────────────────
            // Opt-in FLAC from a public hifi-api instance. Tried BEFORE JioSaavn so
            // lossless wins. Falls through to Saavn/YouTube on ANY failure.
            if (!forceStandardAudio && allowLossless && context.dataStore.get(EnableTidalStreamingKey, false)) {
                Timber.tag(TAG).d("Lossless enabled — trying TIDAL for videoId=$videoId")
                val tidalResult = runCatching {
                    val (currentSong, meta) = coroutineScope {
                        val nextDeferred = async {
                            val nextResult = YouTube.next(WatchEndpoint(videoId = videoId)).getOrNull()
                            nextResult?.items?.getOrNull(nextResult.currentIndex ?: 0)
                                ?: nextResult?.items?.firstOrNull()
                        }
                        val metaDeferred = async { playerResponseForMetadata(videoId, playlistId).getOrNull() }
                        nextDeferred.await() to metaDeferred.await()
                    }

                    val title = currentSong?.title ?: meta?.videoDetails?.title.orEmpty()
                    if (title.isBlank()) return@runCatching null

                    val artistNames: List<String> = if (currentSong?.artists?.isNotEmpty() == true) {
                        currentSong.artists.map { it.name }
                    } else {
                        listOf(
                            meta?.videoDetails?.author.orEmpty()
                                .replace(Regex("(?i)\\s*-\\s*topic\\b"), "")
                                .replace(Regex("(?i)\\s*VEVO\\b"), "")
                                .trim()
                        ).filter { it.isNotBlank() }
                    }
                    val query = "$title ${artistNames.joinToString(" ")}"
                        .replace("&", " ").replace(",", " ")
                        .replace(Regex("\\s+"), " ").trim()

                    val wantedTitle = title.lowercase(java.util.Locale.US)
                    val wantedArtists = artistNames.map { it.lowercase(java.util.Locale.US) }
                    val customUrl = context.dataStore.get(TidalInstanceUrlKey, "").ifBlank { null }

                    val candidates = TidalService.search(query, customUrl)
                    val best = candidates.firstOrNull { c ->
                        val ct = c.title.lowercase(java.util.Locale.US)
                        val ca = c.artistNames.map { it.lowercase(java.util.Locale.US) }
                        val titleOk = ct.contains(wantedTitle) || wantedTitle.contains(ct)
                        val artistOk = wantedArtists.isEmpty() || wantedArtists.any { w ->
                            ca.any { it.contains(w) || w.contains(it) }
                        }
                        titleOk && artistOk
                    } ?: run {
                        Timber.tag(TAG).d("TIDAL: no match for \"$query\" — falling back")
                        return@runCatching null
                    }

                    val quality = runCatching {
                        TidalQuality.valueOf(context.dataStore.get(TidalQualityKey, TidalQuality.LOSSLESS.name))
                    }.getOrDefault(TidalQuality.LOSSLESS)

                    val streamUrl = TidalService.streamUrl(best.id, quality.toApiValue(), customUrl)
                        ?: run {
                            Timber.tag(TAG).d("TIDAL: no stream URL for id=${best.id} — falling back")
                            return@runCatching null
                        }

                    Timber.tag(TAG).i("Tidal: streaming FLAC \"${best.title}\" (id=${best.id}, ${quality.toApiValue()}) for videoId=$videoId")
                    PlaybackData(
                        audioConfig      = meta?.playerConfig?.audioConfig,
                        videoDetails     = meta?.videoDetails,
                        playbackTracking = meta?.playbackTracking,
                        format           = PlayerResponse.StreamingData.Format(
                            itag             = 9999,               // sentinel: lossless FLAC
                            url              = streamUrl,
                            // codecs= segment so the media-info sheet shows "flac", not the "mp3" default
                            mimeType         = "audio/flac; codecs=\"flac\"",
                            bitrate          = 1_411_000,          // nominal 16/44.1 stereo, for the badge
                            width            = null,
                            height           = null,
                            contentLength    = null,
                            quality          = quality.toApiValue(),
                            fps              = null,
                            qualityLabel     = null,
                            averageBitrate   = null,
                            audioQuality     = quality.toApiValue(),
                            approxDurationMs = best.duration?.let { (it * 1000L).toString() },
                            audioSampleRate  = 44100,
                            audioChannels    = 2,
                            loudnessDb       = null,
                            lastModified     = null,
                            signatureCipher  = null,
                            cipher           = null,
                            audioTrack       = null,
                        ),
                        streamUrl              = streamUrl,
                        streamExpiresInSeconds = 3600,
                        isTidalStream          = true,
                    )
                }.getOrNull()

                if (tidalResult != null) return Result.success(tidalResult)
                Timber.tag(TAG).d("TIDAL intercept failed or returned null — trying next source")
            }
            // ── End TIDAL intercept ──────────────────────────────────────────────

            val saavnEnabled = context.dataStore.get(EnableSaavnStreamingKey, false)
            if (saavnEnabled) {
                Timber.tag(TAG).d("JioSaavn streaming enabled — trying Saavn for videoId=$videoId")
                val saavnResult = runCatching {
                    // Step 1: fetch YouTube Music next items and player metadata concurrently
                    val (currentSong, meta) = coroutineScope {
                        val nextDeferred = async {
                            val nextResult = YouTube.next(WatchEndpoint(videoId = videoId)).getOrNull()
                            nextResult?.items?.getOrNull(nextResult.currentIndex ?: 0)
                                ?: nextResult?.items?.firstOrNull()
                        }
                        val metaDeferred = async {
                            playerResponseForMetadata(videoId, playlistId).getOrNull()
                        }
                        nextDeferred.await() to metaDeferred.await()
                    }

                    // Prefer the YouTube Music next() title; fall back to videoDetails title
                    val title = currentSong?.title
                        ?: meta?.videoDetails?.title.orEmpty()

                    // Use the proper artist list from SongItem (real artist names).
                    // Fall back to videoDetails.author with "- Topic" stripped.
                    val artistNames: List<String> = if (currentSong?.artists?.isNotEmpty() == true) {
                        currentSong.artists.map { it.name }
                    } else {
                        listOf(
                            meta?.videoDetails?.author.orEmpty()
                                .replace(Regex("(?i)\\s*-\\s*topic\\b"), "")
                                .replace(Regex("(?i)\\s*VEVO\\b"), "")
                                .trim()
                        ).filter { it.isNotBlank() }
                    }
                    val artist = artistNames.joinToString(", ")

                    if (title.isBlank()) return@runCatching null

                    Timber.tag(TAG).d("Saavn: resolved title=\"$title\" artists=$artistNames for videoId=$videoId")

                    val albumName = currentSong?.album?.name.orEmpty()
                    val wantedTitleLower = title.lowercase(java.util.Locale.US)
                    val wantedArtistsLower = artistNames.map { it.lowercase(java.util.Locale.US) }

                    val primaryQuery = if (albumName.isNotBlank()) {
                        "$albumName $title $artist"
                    } else {
                        "$title $artist"
                    }
                    .replace("&", " ")
                    .replace(",", " ")
                    .replace(Regex("\\s+"), " ")
                    .trim()

                    val fallbackQuery = "$title $artist"
                    .replace("&", " ")
                    .replace(",", " ")
                    .replace(Regex("\\s+"), " ")
                    .trim()

                    suspend fun findMatch(searchQuery: String): com.music.jiosaavn.SaavnSong? {
                        if (searchQuery.isBlank()) return null
                        Timber.tag(TAG).d("Saavn: searching with query: \"$searchQuery\"")
                        val songs = SaavnService.searchSongs(searchQuery).getOrNull() ?: return null
                        return songs.firstOrNull { candidate ->
                            val candidateTitleLower = candidate.name.lowercase(java.util.Locale.US)
                            val candidateArtists = candidate.artists.primary.map { it.name.lowercase(java.util.Locale.US) }
                            
                            val titleMatches = candidateTitleLower.contains(wantedTitleLower) || wantedTitleLower.contains(candidateTitleLower)
                            val artistMatches = wantedArtistsLower.isEmpty() || wantedArtistsLower.any { wanted ->
                                candidateArtists.any { candidateArtist ->
                                    candidateArtist.contains(wanted) || wanted.contains(candidateArtist)
                                }
                            }
                            
                            val isMatch = titleMatches && artistMatches
                            if (isMatch) {
                                Timber.tag(TAG).d("Saavn: candidate matched: \"${candidate.name}\" on album \"${candidate.album?.name}\" by ${candidate.artists.primary.joinToString { it.name }}")
                            } else {
                                Timber.tag(TAG).d("Saavn: candidate rejected (name/artist mismatch): \"${candidate.name}\" on album \"${candidate.album?.name}\" by ${candidate.artists.primary.joinToString { it.name }}")
                            }
                            isMatch
                        }
                    }

                    var bestSong = findMatch(primaryQuery)
                    if (bestSong == null && primaryQuery != fallbackQuery) {
                        Timber.tag(TAG).d("Saavn: no match found with primary query, trying fallback: \"$fallbackQuery\"")
                        bestSong = findMatch(fallbackQuery)
                    }

                    if (bestSong == null) {
                        Timber.tag(TAG).d("Saavn: no matching candidate found — falling back to YT")
                        return@runCatching null
                    }

                    Timber.tag(TAG).i("Saavn: matched \"${bestSong.name}\" (id=${bestSong.id}, album=\"${bestSong.album?.name}\")")

                    // Step 4: resolve stream URL at requested quality
                    val qualityKey = context.dataStore.get(SaavnAudioQualityKey, SaavnAudioQuality.QUALITY_320.name)
                    val quality = runCatching { SaavnAudioQuality.valueOf(qualityKey) }
                        .getOrDefault(SaavnAudioQuality.QUALITY_320)

                    // First try to resolve stream URL directly from the search result's downloadUrl list
                    // to avoid an extra details API call (saves 300ms-800ms).
                    var streamUrl = SaavnService.selectBestUrl(bestSong.downloadUrl, quality.toApiValue())
                    if (streamUrl.isNullOrBlank()) {
                        Timber.tag(TAG).d("Saavn: downloadUrl list empty in search results, fetching via getBestStreamUrl for songId=${bestSong.id}")
                        streamUrl = SaavnService.getBestStreamUrl(bestSong.id, quality.toApiValue())
                    } else {
                        Timber.tag(TAG).d("Saavn: resolved stream URL directly from search results: $streamUrl")
                    }

                    if (streamUrl.isNullOrBlank()) {
                        Timber.tag(TAG).d("Saavn: no stream URL for songId=${bestSong.id} — falling back to YT")
                        return@runCatching null
                    }

                    // Optimization: Skip synchronous HTTP HEAD request to get content length during playback.
                    // ExoPlayer parses the content length automatically from HTTP GET response headers on buffer.
                    val contentLength: Long? = null

                    Timber.tag(TAG).i("Saavn: streaming from JioSaavn (quality=${quality.toApiValue()}) for videoId=$videoId")
                    // Return a minimal PlaybackData using the Saavn URL.
                    // Reuse the YouTube metadata already fetched in Step 1 — no second
                    // network call needed. This keeps audioConfig/videoDetails/playbackTracking
                    // intact so history and normalization still work properly.
                    PlaybackData(
                        audioConfig      = meta?.playerConfig?.audioConfig,
                        videoDetails     = meta?.videoDetails,
                        playbackTracking = meta?.playbackTracking,
                        format           = PlayerResponse.StreamingData.Format(
                            itag             = when (quality) {
                                SaavnAudioQuality.QUALITY_320 -> 141
                                SaavnAudioQuality.QUALITY_160 -> 140
                                SaavnAudioQuality.QUALITY_96  -> 139
                            },
                            url              = streamUrl,
                            // JioSaavn delivers AAC-LC audio inside a regular MP4 container
                            // (e.g. https://aac.saavncdn.com/.../{id}_320.mp4)
                            mimeType         = "audio/mp4; codecs=\"mp4a.40.2\"",
                            bitrate          = when (quality) {
                                SaavnAudioQuality.QUALITY_320 -> 320_000
                                SaavnAudioQuality.QUALITY_160 -> 160_000
                                SaavnAudioQuality.QUALITY_96  -> 96_000
                            },
                            width            = null,
                            height           = null,
                            contentLength    = contentLength,
                            quality          = quality.toApiValue(),
                            fps              = null,
                            qualityLabel     = null,
                            averageBitrate   = null,
                            audioQuality     = quality.toApiValue(),
                            approxDurationMs = null,
                            audioSampleRate  = null,
                            audioChannels    = null,
                            loudnessDb       = null,
                            lastModified     = null,
                            signatureCipher  = null,
                            cipher           = null,
                            audioTrack       = null,
                        ),
                        streamUrl              = streamUrl,
                        streamExpiresInSeconds = 3600,
                        isSaavnStream          = true,   // ← mark as Saavn so downloads skip YT range trick
                    )
                }.getOrNull()

                if (saavnResult != null) {
                    return Result.success(saavnResult)
                }
                // Any exception or null → fall through to YouTube below
                Timber.tag(TAG).d("Saavn intercept failed or returned null — falling back to YouTube")
            }
        }
        // ── End JioSaavn intercept ───────────────────────────────────────────

        val firstAttempt = resolvePlaybackData(videoId, playlistId, audioQuality, connectivityManager)
        
        if (firstAttempt.isFailure && YouTube.cookie == null) {
            Timber.tag(TAG).w("Playback failed for guest. Rotating session and retrying...")
            PlaybackLogManager.log(PlaybackLogLevel.BOT, "Playback failed for guest", "Triggering bot detection mitigation (rotating guest session)")
            BotDetectionMitigator.rotateGuestSession()
            val retryResult = resolvePlaybackData(videoId, playlistId, audioQuality, connectivityManager)
            retryResult.onSuccess { BotDetectionMitigator.notifyPlaybackSuccess() }
            return retryResult
        }
        
        firstAttempt.onSuccess { BotDetectionMitigator.notifyPlaybackSuccess() }
        return firstAttempt
    }

    private suspend fun resolvePlaybackData(
        videoId: String,
        playlistId: String? = null,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
    ): Result<PlaybackData> = runCatching {
        Timber.tag(logTag).d("Fetching player response for videoId: $videoId, playlistId: $playlistId")
        PlaybackLogManager.log(PlaybackLogLevel.INFO, "Resolving playback data", "Video: $videoId")
        
        // Debug: Log ALL playback attempts
        println("[PLAYBACK_DEBUG] playerResponseForPlayback called: videoId=$videoId, playlistId=$playlistId")
        // Check if this is an uploaded/privately owned track
        val isUploadedTrack = playlistId == "MLPT" || playlistId?.contains("MLPT") == true

        val isLoggedIn = YouTube.cookie != null
        Timber.tag(logTag).d("Session authentication status: ${if (isLoggedIn) "Logged in" else "Not logged in"}")

        // Get signature timestamp (same as before for normal content)
        val signatureTimestamp = getSignatureTimestampOrNull(videoId)
        Timber.tag(logTag).d("Signature timestamp: ${signatureTimestamp.timestamp}")

        // Generate PoToken ONLY if MAIN_CLIENT uses it (which it now doesn't since we use ANDROID_VR)
        var poToken: PoTokenResult? = null
        val sessionId = if (isLoggedIn) YouTube.dataSyncId else YouTube.visitorData
        if (MAIN_CLIENT.useWebPoTokens && sessionId != null) {
            Timber.tag(logTag).d("Generating PoToken for MAIN_CLIENT with sessionId")
            try {
                poToken = poTokenGenerator.getWebClientPoToken(videoId, sessionId)
                if (poToken != null) {
                    Timber.tag(logTag).d("PoToken generated successfully")
                }
            } catch (e: Exception) {
                Timber.tag(logTag).e(e, "PoToken generation failed: ${e.message}")
            }
        }

        // Try MAIN_CLIENT (ANDROID_VR) for fast stream resolution and METADATA_CLIENT (WEB_REMIX) for history tracking in parallel
        var (mainPlayerResponse, metadataResponse) = coroutineScope {
            val mainDeferred = async {
                Timber.tag(logTag).d("Attempting to get player response using MAIN_CLIENT: ${MAIN_CLIENT.clientName}")
                PlaybackLogManager.log(PlaybackLogLevel.DEBUG, "Trying ${MAIN_CLIENT.clientName} (Main)")
                YouTube.player(videoId, playlistId, MAIN_CLIENT, signatureTimestamp.timestamp, poToken?.playerRequestPoToken).getOrThrow()
            }
            val metaDeferred = async {
                if (isLoggedIn) {
                    Timber.tag(logTag).d("Fetching metadata from METADATA_CLIENT (WEB_REMIX) for authenticated tracking")
                    try {
                        // Only generate PoToken for web client metadata fetch
                        var metaPoToken: PoTokenResult? = null
                        val metaSessionId = YouTube.dataSyncId
                        if (METADATA_CLIENT.useWebPoTokens && metaSessionId != null) {
                            try {
                                metaPoToken = poTokenGenerator.getWebClientPoToken(videoId, metaSessionId)
                            } catch (e: Exception) {
                                Timber.tag(logTag).e(e, "Metadata PoToken generation failed")
                            }
                        }
                        YouTube.player(
                            videoId, playlistId, METADATA_CLIENT,
                            signatureTimestamp.timestamp, metaPoToken?.playerRequestPoToken
                        ).getOrNull().also { response ->
                            Timber.tag(logTag).d("Metadata response obtained: ${response?.playabilityStatus?.status}")
                        }
                    } catch (e: Exception) {
                        Timber.tag(logTag).e(e, "Failed to fetch metadata from METADATA_CLIENT")
                        null
                    }
                } else {
                    null
                }
            }
            mainDeferred.await() to metaDeferred.await()
        }

        // Debug uploaded track response
        if (isUploadedTrack || playlistId?.contains("MLPT") == true) {
            println("[PLAYBACK_DEBUG] Main player response status: ${mainPlayerResponse.playabilityStatus.status}")
            println("[PLAYBACK_DEBUG] Playability reason: ${mainPlayerResponse.playabilityStatus.reason}")
            println("[PLAYBACK_DEBUG] Video details: title=${mainPlayerResponse.videoDetails?.title}, videoId=${mainPlayerResponse.videoDetails?.videoId}")
            println("[PLAYBACK_DEBUG] Streaming data null? ${mainPlayerResponse.streamingData == null}")
            println("[PLAYBACK_DEBUG] Adaptive formats count: ${mainPlayerResponse.streamingData?.adaptiveFormats?.size ?: 0}")
        }

        var usedAgeRestrictedClient: YouTubeClient? = null
        val wasOriginallyAgeRestricted: Boolean

        // Check if MAIN_CLIENT response indicates age-restricted.
        // NOTE: Do NOT include LOGIN_REQUIRED here — ANDROID_VR returns LOGIN_REQUIRED as a
        // bot-detection / client-not-supported signal, NOT a content age gate. Treating it as
        // age-restricted incorrectly reroutes every bot-flagged request through WEB_CREATOR
        // and causes streaming failures for logged-in users.
        val mainStatus = mainPlayerResponse.playabilityStatus.status
        val isAgeRestrictedFromResponse = mainStatus in listOf(
            "AGE_CHECK_REQUIRED",
            "AGE_VERIFICATION_REQUIRED",
            "CONTENT_CHECK_REQUIRED"
        )
        wasOriginallyAgeRestricted = isAgeRestrictedFromResponse

        if (isAgeRestrictedFromResponse && isLoggedIn) {
            // Age-restricted: use WEB_CREATOR directly (no NewPipe needed from here)
            Timber.tag(logTag).d("Age-restricted detected, using WEB_CREATOR")
            Log.i(TAG, "Age-restricted: using WEB_CREATOR for videoId=$videoId")
            val creatorResponse = YouTube.player(videoId, playlistId, WEB_CREATOR, null, null).getOrNull()
            if (creatorResponse?.playabilityStatus?.status == "OK") {
                Timber.tag(logTag).d("WEB_CREATOR works for age-restricted content")
                mainPlayerResponse = creatorResponse
                usedAgeRestrictedClient = WEB_CREATOR
            }
        }

        // If we still don't have a valid response, throw
        if (mainPlayerResponse == null) {
            throw Exception("Failed to get player response")
        }

        // Fetch audioConfig and playbackTracking from the metadata client if available (authenticated)
        // Fall back to mainPlayerResponse values if metadata fetch failed or user is not logged in
        val audioConfig = metadataResponse?.playerConfig?.audioConfig ?: mainPlayerResponse.playerConfig?.audioConfig
        val videoDetails = metadataResponse?.videoDetails ?: mainPlayerResponse.videoDetails
        val playbackTracking = metadataResponse?.playbackTracking ?: mainPlayerResponse.playbackTracking
        var format: PlayerResponse.StreamingData.Format? = null
        var streamUrl: String? = null
        var streamExpiresInSeconds: Int? = null
        var streamPlayerResponse: PlayerResponse? = null
        var retryMainPlayerResponse: PlayerResponse? = if (usedAgeRestrictedClient != null) mainPlayerResponse else null

        // Check current status
        val currentStatus = mainPlayerResponse.playabilityStatus.status
        var isAgeRestricted = currentStatus in listOf(
            "AGE_CHECK_REQUIRED",
            "AGE_VERIFICATION_REQUIRED",
            "CONTENT_CHECK_REQUIRED"
        )

        if (isAgeRestricted) {
            Timber.tag(logTag).d("Content is still age-restricted (status: $currentStatus), will try fallback clients")
            Log.i(TAG, "Age-restricted content detected: videoId=$videoId, status=$currentStatus")
        }

        // Check if this is a privately owned track (uploaded song)
        val isPrivateTrack = mainPlayerResponse.videoDetails?.musicVideoType == "MUSIC_VIDEO_TYPE_PRIVATELY_OWNED_TRACK"

        // For private tracks: use TVHTML5 (index 1) with PoToken + n-transform
        // For age-restricted: skip main client, start with fallbacks
        // For normal content: standard order
        val startIndex = when {
            isPrivateTrack -> 1  // TVHTML5
            isAgeRestricted -> 0
            else -> -1
        }

        for (clientIndex in (startIndex until STREAM_FALLBACK_CLIENTS.size)) {
            // reset for each client
            format = null
            streamUrl = null
            streamExpiresInSeconds = null

            // decide which client to use for streams and load its player response
            val client: YouTubeClient
            if (clientIndex == -1) {
                // try with streams from main client first (use retry response if available)
                client = MAIN_CLIENT
                streamPlayerResponse = retryMainPlayerResponse ?: mainPlayerResponse
                Timber.tag(logTag).d("Trying stream from MAIN_CLIENT: ${client.clientName}")
            } else {
                // after main client use fallback clients
                client = STREAM_FALLBACK_CLIENTS[clientIndex]
                Timber.tag(logTag).d("Trying fallback client ${clientIndex + 1}/${STREAM_FALLBACK_CLIENTS.size}: ${client.clientName}")
                PlaybackLogManager.log(PlaybackLogLevel.DEBUG, "Trying fallback [${clientIndex + 1}/${STREAM_FALLBACK_CLIENTS.size}]", client.clientName)

                if (client.loginRequired && !isLoggedIn && YouTube.cookie == null) {
                    // skip client if it requires login but user is not logged in
                    Timber.tag(logTag).d("Skipping client ${client.clientName} - requires login but user is not logged in")
                    continue
                }

                // Lazily generate PoToken for fallback web clients if we haven't already
                if (client.useWebPoTokens && poToken == null && sessionId != null) {
                    Timber.tag(logTag).d("Lazily generating PoToken for fallback web client: ${client.clientName}")
                    try {
                        poToken = poTokenGenerator.getWebClientPoToken(videoId, sessionId)
                    } catch (e: Exception) {
                        Timber.tag(logTag).e(e, "Lazy PoToken generation failed")
                    }
                }

                Timber.tag(logTag).d("Fetching player response for fallback client: ${client.clientName}")
                // Only pass poToken for clients that support it
                val clientPoToken = if (client.useWebPoTokens) poToken?.playerRequestPoToken else null
                // Skip signature timestamp for age-restricted (faster), use it for normal content
                val clientSigTimestamp = if (wasOriginallyAgeRestricted) null else signatureTimestamp.timestamp
                streamPlayerResponse =
                    YouTube.player(videoId, playlistId, client, clientSigTimestamp, clientPoToken).getOrNull()
            }

            // process current client response
            if (streamPlayerResponse?.playabilityStatus?.status == "OK") {
                Timber.tag(logTag).d("Player response status OK for client: ${if (clientIndex == -1) MAIN_CLIENT.clientName else STREAM_FALLBACK_CLIENTS[clientIndex].clientName}")
                PlaybackLogManager.log(PlaybackLogLevel.INFO, "Player response OK", if (clientIndex == -1) MAIN_CLIENT.clientName else STREAM_FALLBACK_CLIENTS[clientIndex].clientName)

                // Check if formats have direct URLs (no signatureCipher needed)
                val hasDirectUrls = streamPlayerResponse.streamingData?.adaptiveFormats
                    ?.any { !it.url.isNullOrEmpty() } == true
                val hasSignatureCipher = streamPlayerResponse.streamingData?.adaptiveFormats
                    ?.any { !it.signatureCipher.isNullOrEmpty() || !it.cipher.isNullOrEmpty() } == true

                Timber.tag(logTag).d("URL check: hasDirectUrls=$hasDirectUrls, hasSignatureCipher=$hasSignatureCipher")

                // Skip NewPipe - use direct URLs or custom cipher in findUrlOrNull
                val responseToUse = streamPlayerResponse

                format =
                    findFormat(
                        responseToUse,
                        audioQuality,
                        connectivityManager,
                    )

                if (format == null) {
                    Timber.tag(logTag).d("No suitable format found for client: ${if (clientIndex == -1) MAIN_CLIENT.clientName else STREAM_FALLBACK_CLIENTS[clientIndex].clientName}")
                    continue
                }

                Timber.tag(logTag).d("Format found: ${format.mimeType}, bitrate: ${format.bitrate}")

                streamUrl = findUrlOrNull(format, videoId, responseToUse, skipNewPipe = wasOriginallyAgeRestricted)
                if (streamUrl == null) {
                    Timber.tag(logTag).d("Stream URL not found for format")
                    continue
                }

                // Apply n-transform for throttle parameter handling
                val currentClient = if (clientIndex == -1) {
                    usedAgeRestrictedClient ?: MAIN_CLIENT
                } else {
                    STREAM_FALLBACK_CLIENTS[clientIndex]
                }

                // Check if this is a privately owned track
                val isPrivatelyOwnedTrack = streamPlayerResponse.videoDetails?.musicVideoType == "MUSIC_VIDEO_TYPE_PRIVATELY_OWNED_TRACK"

                // Apply n-transform FIRST for web clients (main branch order - critical!)
                if (currentClient.useWebPoTokens) {
                    try {
                        Timber.tag(logTag).d("Applying n-transform to stream URL for ${currentClient.clientName}")
                        val transformed = EjsNTransformSolver.transformNParamInUrl(streamUrl!!)
                        if (transformed != streamUrl) {
                            streamUrl = transformed
                            Timber.tag(logTag).d("N-transform applied successfully")
                        }
                    } catch (e: Exception) {
                        Timber.tag(logTag).e(e, "N-transform failed: ${e.message}")
                    }
                }

                // Apply PoToken SECOND (after n-transform - main branch order)
                // Note: pot token is base64 - do NOT Uri.encode it (breaks validation)
                if (currentClient.useWebPoTokens && poToken?.streamingDataPoToken != null) {
                    Timber.tag(logTag).d("Appending pot= parameter to stream URL")
                    val separator = if ("?" in streamUrl!!) "&" else "?"
                    streamUrl = "${streamUrl}${separator}pot=${poToken.streamingDataPoToken}"
                }

                streamExpiresInSeconds = streamPlayerResponse.streamingData?.expiresInSeconds
                if (streamExpiresInSeconds == null) {
                    Timber.tag(logTag).d("Stream expiration time not found")
                    continue
                }

                Timber.tag(logTag).d("Stream expires in: $streamExpiresInSeconds seconds")

                // Debug: Log URL host and pot token for debugging
                val urlHost = try { java.net.URL(streamUrl).host } catch (e: Exception) { "unknown" }
                Timber.tag(logTag).d("Stream URL host: $urlHost, pot length: ${poToken?.streamingDataPoToken?.length ?: 0}")

                // Check if this is a privately owned track (uploaded song)
                val isPrivatelyOwned = streamPlayerResponse.videoDetails?.musicVideoType == "MUSIC_VIDEO_TYPE_PRIVATELY_OWNED_TRACK"

                if (clientIndex == STREAM_FALLBACK_CLIENTS.size - 1 || isPrivatelyOwned) {
                    /** skip [validateStatus] for last client or private tracks */
                    if (isPrivatelyOwned) {
                        Timber.tag(logTag).d("Skipping validation for privately owned track: ${currentClient.clientName}")
                        println("[PLAYBACK_DEBUG] Using stream without validation for PRIVATELY_OWNED_TRACK")
                    } else {
                        Timber.tag(logTag).d("Using last fallback client without validation: ${STREAM_FALLBACK_CLIENTS[clientIndex].clientName}")
                    }
                    Log.i(TAG, "Playback: client=${currentClient.clientName}, videoId=$videoId, private=$isPrivatelyOwned")
                    break
                }

                if (validateStatus(streamUrl!!)) {
                    // working stream found
                    Timber.tag(logTag).d("Stream validated successfully with client: ${currentClient.clientName}")
                    PlaybackLogManager.log(PlaybackLogLevel.INFO, "Stream validated", currentClient.clientName)
                    // Log for release builds
                    Log.i(TAG, "Playback: client=${currentClient.clientName}, videoId=$videoId")
                    break
                } else {
                    Timber.tag(logTag).d("Stream validation failed for client: ${currentClient.clientName}")

                    // For web clients: try alternate n-transform and re-validate (Zemer approach)
                    if (currentClient.useWebPoTokens) {
                        var nTransformWorked = false

                        // Try CipherDeobfuscator n-transform
                        try {
                            val nTransformed = CipherDeobfuscator.transformNParamInUrl(streamUrl!!)
                            if (nTransformed != streamUrl) {
                                Timber.tag(logTag).d("CipherDeobfuscator n-transform applied, re-validating...")
                                if (validateStatus(nTransformed)) {
                                    Timber.tag(logTag).d("N-transformed URL VALIDATED OK!")
                                    streamUrl = nTransformed
                                    nTransformWorked = true
                                    Log.i(TAG, "Playback: client=${currentClient.clientName}, videoId=$videoId (cipher n-transform)")
                                }
                            }
                        } catch (e: Exception) {
                            Timber.tag(logTag).e(e, "CipherDeobfuscator n-transform error")
                        }

                        if (nTransformWorked) break
                    }
                }
            } else {
                val status = streamPlayerResponse?.playabilityStatus?.status ?: "Unknown"
                val reason = streamPlayerResponse?.playabilityStatus?.reason ?: "No reason"
                Timber.tag(logTag).d("Player response status not OK: $status, reason: $reason")
                PlaybackLogManager.log(PlaybackLogLevel.WARNING, "Client failed: ${client.clientName}", "$status: $reason")
                
                // Restore original Timber log for Logcat
                Timber.tag(logTag).d("Player response status not OK: ${streamPlayerResponse?.playabilityStatus?.status}, reason: ${streamPlayerResponse?.playabilityStatus?.reason}")
            }
        }

        if (streamPlayerResponse == null) {
            Timber.tag(logTag).e("Bad stream player response - all clients failed")
            if (isUploadedTrack) {
                println("[PLAYBACK_DEBUG] FAILURE: All clients failed for uploaded track videoId=$videoId")
            }
            throw Exception("Bad stream player response")
        }

        if (streamPlayerResponse.playabilityStatus.status != "OK") {
            val errorReason = streamPlayerResponse.playabilityStatus.reason
            Timber.tag(logTag).e("Playability status not OK: $errorReason")
            if (isUploadedTrack) {
                println("[PLAYBACK_DEBUG] FAILURE: Playability not OK for uploaded track - status=${streamPlayerResponse.playabilityStatus.status}, reason=$errorReason")
            }
            throw PlaybackException(
                errorReason,
                null,
                PlaybackException.ERROR_CODE_REMOTE_ERROR
            )
        }

        if (streamExpiresInSeconds == null) {
            Timber.tag(logTag).e("Missing stream expire time")
            throw Exception("Missing stream expire time")
        }

        if (format == null) {
            Timber.tag(logTag).e("Could not find format")
            throw Exception("Could not find format")
        }

        if (streamUrl == null) {
            Timber.tag(logTag).e("Could not find stream url")
            throw Exception("Could not find stream url")
        }

        Timber.tag(logTag).d("Successfully obtained playback data with format: ${format.mimeType}, bitrate: ${format.bitrate}")
        if (isUploadedTrack) {
            println("[PLAYBACK_DEBUG] SUCCESS: Got playback data for uploaded track - format=${format.mimeType}, streamUrl=${streamUrl?.take(100)}...")
        }
        PlaybackData(
            audioConfig,
            videoDetails,
            playbackTracking,
            format,
            streamUrl,
            streamExpiresInSeconds,
        )
    }.onFailure { e ->
        Timber.tag(logTag).e(e, "Playback resolution failed")
        PlaybackLogManager.log(PlaybackLogLevel.ERROR, "Playback failed", "${e::class.simpleName}: ${e.message}")
        
        // Restore original println for Logcat
        println("[PLAYBACK_DEBUG] EXCEPTION during playback for videoId=$videoId: ${e::class.simpleName}: ${e.message}")
        e.printStackTrace()
    }
    /**
     * Simple player response intended to use for metadata only.
     * Stream URLs of this response might not work so don't use them.
     */
    suspend fun playerResponseForMetadata(
        videoId: String,
        playlistId: String? = null,
    ): Result<PlayerResponse> {
        Timber.tag(logTag).d("Fetching metadata-only player response for videoId: $videoId using MAIN_CLIENT: ${MAIN_CLIENT.clientName}")
        return YouTube.player(videoId, playlistId, client = WEB_REMIX) // ANDROID_VR does not work with history
            .onSuccess { Timber.tag(logTag).d("Successfully fetched metadata") }
            .onFailure { Timber.tag(logTag).e(it, "Failed to fetch metadata") }
    }

    private fun findFormat(
        playerResponse: PlayerResponse,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
    ): PlayerResponse.StreamingData.Format? {
        Timber.tag(logTag).d("Finding format with audioQuality: $audioQuality, network metered: ${connectivityManager.isActiveNetworkMetered}")

        val format = playerResponse.streamingData?.adaptiveFormats
            ?.filter { it.isAudio && it.isOriginal }
            ?.maxByOrNull {
                it.bitrate * when (audioQuality) {
                    AudioQuality.AUTO -> if (connectivityManager.isActiveNetworkMetered) -1 else 1
                    AudioQuality.HIGH -> 1
                    AudioQuality.LOW -> -1
                } + (if (it.mimeType.startsWith("audio/webm")) 10240 else 0) // prefer opus stream
            }

        if (format != null) {
            Timber.tag(logTag).d("Selected format: ${format.mimeType}, bitrate: ${format.bitrate}")
        } else {
            Timber.tag(logTag).d("No suitable audio format found")
        }

        return format
    }
    /**
     * Checks if the stream url returns a successful status.
     * If this returns true the url is likely to work.
     * If this returns false the url might cause an error during playback.
     */
    private fun validateStatus(url: String): Boolean {
        Timber.tag(logTag).d("Validating stream URL status")
        try {
            val requestBuilder = okhttp3.Request.Builder()
                .head()
                .url(url)
                .header("User-Agent", YouTubeClient.USER_AGENT_WEB)

            // Do NOT add Cookie header — googlevideo.com CDN rejects account cookies with 403.
            // Stream URLs are already authenticated via signed URL parameters.

            val response = httpClient.newCall(requestBuilder.build()).execute()
            val isSuccessful = response.isSuccessful
            Timber.tag(logTag).d("Stream URL validation result: ${if (isSuccessful) "Success" else "Failed"} (${response.code})")
            return isSuccessful
        } catch (e: Exception) {
            Timber.tag(logTag).e(e, "Stream URL validation failed with exception")
            reportException(e)
        }
        return false
    }
    data class SignatureTimestampResult(
        val timestamp: Int?,
        val isAgeRestricted: Boolean
    )

    private fun getSignatureTimestampOrNull(videoId: String): SignatureTimestampResult {
        Timber.tag(logTag).d("Getting signature timestamp for videoId: $videoId")
        val result = NewPipeExtractor.getSignatureTimestamp(videoId)
        return result.fold(
            onSuccess = { timestamp ->
                Timber.tag(logTag).d("Signature timestamp obtained: $timestamp")
                SignatureTimestampResult(timestamp, isAgeRestricted = false)
            },
            onFailure = { error ->
                val isAgeRestricted = error.message?.contains("age-restricted", ignoreCase = true) == true ||
                    error.cause?.message?.contains("age-restricted", ignoreCase = true) == true
                if (isAgeRestricted) {
                    Timber.tag(logTag).d("Age-restricted content detected from NewPipe")
                    Log.i(TAG, "Age-restricted detected early via NewPipe: videoId=$videoId")
                } else {
                    Timber.tag(logTag).e(error, "Failed to get signature timestamp")
                    reportException(error)
                }
                SignatureTimestampResult(null, isAgeRestricted)
            }
        )
    }

    private suspend fun findUrlOrNull(
        format: PlayerResponse.StreamingData.Format,
        videoId: String,
        playerResponse: PlayerResponse,
        skipNewPipe: Boolean = false
    ): String? {
        Timber.tag(logTag).d("Finding stream URL for format: ${format.mimeType}, videoId: $videoId, skipNewPipe: $skipNewPipe")

        // First check if format already has a URL
        if (!format.url.isNullOrEmpty()) {
            Timber.tag(logTag).d("Using URL from format directly")
            return format.url
        }

        // Try custom cipher deobfuscation for signatureCipher formats
        val signatureCipher = format.signatureCipher ?: format.cipher
        if (!signatureCipher.isNullOrEmpty()) {
            Timber.tag(logTag).d("Format has signatureCipher, using custom deobfuscation")
            val customDeobfuscatedUrl = CipherDeobfuscator.deobfuscateStreamUrl(signatureCipher, videoId)
            if (customDeobfuscatedUrl != null) {
                Timber.tag(logTag).d("Stream URL obtained via custom cipher deobfuscation")
                return customDeobfuscatedUrl
            }
            Timber.tag(logTag).d("Custom cipher deobfuscation failed")
        }

        // Skip NewPipe for age-restricted content
        if (skipNewPipe) {
            Timber.tag(logTag).d("Skipping NewPipe methods for age-restricted content")
            return null
        }

        // Try to get URL using NewPipeExtractor signature deobfuscation
        val deobfuscatedUrl = NewPipeExtractor.getStreamUrl(format, videoId)
        if (deobfuscatedUrl != null) {
            Timber.tag(logTag).d("Stream URL obtained via NewPipe deobfuscation")
            return deobfuscatedUrl
        }

        // Fallback: try to get URL from StreamInfo
        Timber.tag(logTag).d("Trying StreamInfo fallback for URL")
        val streamUrls = YouTube.getNewPipeStreamUrls(videoId)
        if (streamUrls.isNotEmpty()) {
            val streamUrl = streamUrls.find { it.first == format.itag }?.second
            if (streamUrl != null) {
                Timber.tag(logTag).d("Stream URL obtained from StreamInfo")
                return streamUrl
            }

            // If exact itag not found, try to find any audio stream
            val audioStream = streamUrls.find { urlPair ->
                playerResponse.streamingData?.adaptiveFormats?.any {
                    it.itag == urlPair.first && it.isAudio
                } == true
            }?.second

            if (audioStream != null) {
                Timber.tag(logTag).d("Audio stream URL obtained from StreamInfo (different itag)")
                return audioStream
            }
        }

        Timber.tag(logTag).e("Failed to get stream URL")
        return null
    }

    fun forceRefreshForVideo(videoId: String) {
        Timber.tag(logTag).d("Force refreshing for videoId: $videoId")
    }
}
