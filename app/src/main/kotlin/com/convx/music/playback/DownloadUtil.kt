/**
 * BeatWave Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convx.music.playback

import android.util.Log
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest

import android.content.Context
import android.net.ConnectivityManager
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.media3.database.DatabaseProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import com.music.innertube.YouTube
import com.convx.music.constants.AudioQuality
import com.convx.music.constants.AudioQualityKey
import com.convx.music.constants.IpVersionKey
import com.music.innertube.models.IpVersion
import okhttp3.Dns
import java.net.InetAddress
import java.net.Inet4Address
import java.net.Inet6Address
import com.convx.music.db.MusicDatabase
import com.convx.music.db.entities.FormatEntity
import com.convx.music.db.entities.SongEntity
import com.convx.music.di.DownloadCache
import com.convx.music.di.PlayerCache
import com.convx.music.ui.utils.resize
import com.convx.music.utils.YTPlayerUtils
import com.convx.music.utils.enumPreference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import com.convx.music.applecanvas.AppleMusicCanvasProvider
import com.convx.music.canvas.AppleMusicArtistBackgroundProvider
import com.convx.music.constants.CanvasSource
import com.convx.music.constants.CanvasSourceKey
import com.convx.music.ui.player.normalizeCanvasArtistName
import com.convx.music.ui.player.normalizeCanvasSongTitle
import com.convx.music.utils.dataStore
import com.convx.music.vivimusiccanvas.EchoMusicCanvasProvider
import com.convx.music.vivimusiccanvas.ViviMusicCanvasProvider
import com.convx.music.canvas.TidalCanvasProvider
import java.util.Locale
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import java.time.LocalDateTime
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton

@UnstableApi
@Singleton
class DownloadUtil
@Inject
constructor(
    @ApplicationContext context: Context,
    val database: MusicDatabase,
    val databaseProvider: DatabaseProvider,
    @DownloadCache val downloadCache: SimpleCache,
    @PlayerCache val playerCache: SimpleCache,
) {
    private val connectivityManager = context.getSystemService<ConnectivityManager>()!!
    private val audioQuality by enumPreference(context, AudioQualityKey, AudioQuality.AUTO)
    private val ipVersion by enumPreference(context, IpVersionKey, IpVersion.AUTO)
    private val songUrlCache = HashMap<String, Pair<String, Long>>()
    // Keep a reference to context so we can read DataStore prefs for JioSaavn support
    private val appContext: Context = context

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val downloads = MutableStateFlow<Map<String, Download>>(emptyMap())

    private val dataSourceFactory =
        ResolvingDataSource.Factory(
            CacheDataSource
                .Factory()
                .setCache(playerCache)
                .setUpstreamDataSourceFactory(
                    OkHttpDataSource.Factory(
                        OkHttpClient.Builder()
                            .dns(object : Dns {
                                override fun lookup(hostname: String): List<InetAddress> {
                                    val addresses = Dns.SYSTEM.lookup(hostname)
                                    return when (this@DownloadUtil.ipVersion) {
                                        IpVersion.IPV4 -> addresses.filter { it is Inet4Address }.ifEmpty { addresses }
                                        IpVersion.IPV6 -> addresses.filter { it is Inet6Address }.ifEmpty { addresses }
                                        IpVersion.AUTO -> addresses
                                    }
                                }
                            })
                            .proxy(YouTube.proxy)
                            .proxyAuthenticator { _, response ->
                                YouTube.proxyAuth?.let { auth ->
                                    response.request.newBuilder()
                                        .header("Proxy-Authorization", auth)
                                        .build()
                                } ?: response.request
                            }
                            .build(),
                    ),
                ),
        ) { dataSpec ->
            val mediaId = dataSpec.key ?: error("No media id")
            val length = if (dataSpec.length >= 0) dataSpec.length else 1

            if (playerCache.isCached(mediaId, dataSpec.position, length)) {
                return@Factory dataSpec
            }

            songUrlCache[mediaId]?.takeIf { it.second < System.currentTimeMillis() }?.let {
                return@Factory dataSpec.withUri(it.first.toUri())
            }

            val playbackData = runBlocking(Dispatchers.IO) {
                YTPlayerUtils.playerResponseForPlayback(
                    mediaId,
                    audioQuality = audioQuality,
                    connectivityManager = connectivityManager,
                    // Pass context so the JioSaavn intercept fires when the toggle is ON
                    context = appContext,
                    // Lossless is streaming-only for now: downloads stay YouTube so the
                    // offline cache (keyed by videoId) never mixes FLAC and Opus bytes.
                    allowLossless = false,
                )
            }.getOrThrow()
            val format = playbackData.format

            database.query {
                upsert(
                    FormatEntity(
                        id = mediaId,
                        itag = format.itag,
                        mimeType = format.mimeType.split(";")[0],
                        codecs = format.mimeType.split("codecs=").getOrNull(1)?.removeSurrounding("\"") ?: "mp4a.40.2",
                        bitrate = format.bitrate,
                        sampleRate = format.audioSampleRate,
                        contentLength = format.contentLength ?: 0L,
                        loudnessDb = playbackData.audioConfig?.loudnessDb,
                        perceptualLoudnessDb = playbackData.audioConfig?.perceptualLoudnessDb,
                        playbackUrl = playbackData.playbackTracking?.videostatsPlaybackUrl?.baseUrl
                    ),
                )

                val now = LocalDateTime.now()
                val existing = getSongByIdBlocking(mediaId)?.song

                val updatedSong = if (existing != null) {
                    if (existing.dateDownload == null) {
                        existing.copy(dateDownload = now)
                    } else {
                        existing
                    }
                } else {
                    SongEntity(
                        id = mediaId,
                        title = playbackData.videoDetails?.title ?: "Unknown",
                        duration = playbackData.videoDetails?.lengthSeconds?.toIntOrNull() ?: 0,
                        thumbnailUrl = playbackData.videoDetails?.thumbnail?.thumbnails?.lastOrNull()?.url?.resize(1200, 1200),
                        dateDownload = now,
                        isDownloaded = false
                    )
                }

                upsert(updatedSong)

                // Pre-cache the high-res thumbnail immediately when download starts
                updatedSong.thumbnailUrl?.let { url ->
                    val request = ImageRequest.Builder(context)
                        .data(url)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build()
                    SingletonImageLoader.get(context).enqueue(request)
                }

                // --- CANVAS CACHING ---
                scope.launch {
                    val canvasSource = context.dataStore.data.map { it[CanvasSourceKey] ?: CanvasSource.AUTO.name }.first().let { name -> CanvasSource.entries.find { it.name == name } ?: CanvasSource.AUTO }

                    val storefront = Locale.getDefault().country.lowercase(Locale.ROOT).takeIf { it.length == 2 } ?: "us"
                    val requestedTitle = playbackData.videoDetails?.title.orEmpty()
                    val requestedArtist = playbackData.videoDetails?.author.orEmpty()
                    
                    val s = normalizeCanvasSongTitle(requestedTitle)
                    val a = normalizeCanvasArtistName(requestedArtist)

                    val canvas = when (canvasSource) {
                        CanvasSource.AUTO -> {
                            EchoMusicCanvasProvider.getBySongArtist(s, a)?.preferredAnimationUrl
                                ?: AppleMusicCanvasProvider.getBySongArtist(s, a, "", storefront)?.preferredAnimationUrl
                                ?: beatwaveCanvasProvider.getBySongArtist(s, a)?.preferredAnimationUrl
                                ?: TidalCanvasProvider.getBySongArtist(s, a, "")?.preferredAnimationUrl
                        }
                        CanvasSource.ECHO_MUSIC -> EchoMusicCanvasProvider.getBySongArtist(s, a)?.preferredAnimationUrl
                        CanvasSource.APPLE_MUSIC -> AppleMusicCanvasProvider.getBySongArtist(s, a, "", storefront)?.preferredAnimationUrl
                        CanvasSource.beatwave -> beatwaveCanvasProvider.getBySongArtist(s, a)?.preferredAnimationUrl
                        CanvasSource.TIDAL -> TidalCanvasProvider.getBySongArtist(s, a, "")?.preferredAnimationUrl
                    }

                    canvas?.let { url ->
                        val dataSpec = DataSpec.Builder()
                            .setUri(url.toUri())
                            .setKey("$mediaId#canvas")
                            .setFlags(DataSpec.FLAG_ALLOW_CACHE_FRAGMENTATION)
                            .build()
                            
                        val dataSource = CacheDataSource.Factory()
                            .setCache(downloadCache)
                            .setUpstreamDataSourceFactory(DefaultDataSource.Factory(context))
                            .setCacheWriteDataSinkFactory(null)
                            .createDataSource()
                        
                        kotlin.runCatching {
                            val writer = CacheWriter(
                                dataSource,
                                dataSpec,
                                null,
                                null
                            )
                            writer.cache()
                            Log.d("CanvasDownload", "Successfully cached canvas for $mediaId")
                        }.onFailure { e ->
                            Log.e("CanvasDownload", "Failed to cache canvas for $mediaId", e)
                        }
                    }
                }
            }

            // For YouTube streams: append the &range= param so the download cache can
            // handle progressive HTTP range requests. For JioSaavn/TIDAL/spine streams
            // the CDN doesn't need it and contentLength is null, so skip it.
            val streamUrl = if (playbackData.isSaavnStream || playbackData.isTidalStream || playbackData.isSpineStream) {
                playbackData.streamUrl
            } else {
                "${playbackData.streamUrl}&range=0-${format.contentLength ?: 10_000_000}"
            }

            songUrlCache[mediaId] = streamUrl to playbackData.streamExpiresInSeconds * 1000L
            dataSpec.withUri(streamUrl.toUri())
        }

    val downloadNotificationHelper =
        DownloadNotificationHelper(context, ExoDownloadService.CHANNEL_ID)

    @OptIn(DelicateCoroutinesApi::class)
    val downloadManager: DownloadManager =
        DownloadManager(
            context,
            databaseProvider,
            downloadCache,
            dataSourceFactory,
            Executor(Runnable::run)
        ).apply {
            maxParallelDownloads = 3
            addListener(
                object : DownloadManager.Listener {
                    override fun onDownloadChanged(
                        downloadManager: DownloadManager,
                        download: Download,
                        finalException: Exception?,
                    ) {
                        downloads.update { map ->
                            map.toMutableMap().apply {
                                set(download.request.id, download)
                            }
                        }

                        scope.launch {
                            when (download.state) {
                                Download.STATE_COMPLETED -> {
                                    database.updateDownloadedInfo(download.request.id, true, LocalDateTime.now())
                                }
                                Download.STATE_FAILED,
                                Download.STATE_STOPPED,
                                Download.STATE_REMOVING -> {
                                    database.updateDownloadedInfo(download.request.id, false, null)
                                }
                                else -> {
                                }
                            }
                        }
                    }
                }
            )
        }

    init {
        val result = mutableMapOf<String, Download>()
        val cursor = downloadManager.downloadIndex.getDownloads()
        while (cursor.moveToNext()) {
            result[cursor.download.request.id] = cursor.download
        }
        downloads.value = result
    }

    fun getDownload(songId: String): Flow<Download?> = downloads.map { it[songId] }

    fun release() {
        scope.cancel()
    }
}
