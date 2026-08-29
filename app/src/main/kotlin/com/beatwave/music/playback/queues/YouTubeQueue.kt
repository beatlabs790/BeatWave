/**
 * BeatWave Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.beatwave.music.playback.queues

import androidx.media3.common.MediaItem
import com.music.innertube.YouTube
import com.music.innertube.models.WatchEndpoint
import com.beatwave.music.extensions.toMediaItem
import com.beatwave.music.models.MediaMetadata
import com.beatwave.music.constants.RecommendationEngineStyle
import com.beatwave.music.db.MusicDatabase
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.firstOrNull

class YouTubeQueue(
    private var endpoint: WatchEndpoint,
    override val preloadItem: MediaMetadata? = null,
    var recommendationStyle: RecommendationEngineStyle = RecommendationEngineStyle.HYBRID
) : Queue {
    var database: MusicDatabase? = null
    private var continuation: String? = null
    private var retryCount = 0
    private val maxRetries = 3

    override suspend fun getInitialStatus(): Queue.Status {
        return withContext(IO) {
            var lastException: Throwable? = null
            
            // Try with original endpoint first (allows YouTube to personalize recommendations)
            for (attempt in 0..maxRetries) {
                try {
                    val nextResult = YouTube.next(endpoint, continuation).getOrThrow()
                    endpoint = nextResult.endpoint
                    continuation = nextResult.continuation
                    retryCount = 0
                    
                    val tailoredItems = tailorRecommendations(nextResult.items.map { it.toMediaItem() })
                    
                    return@withContext Queue.Status(
                        title = nextResult.title,
                        items = tailoredItems,
                        mediaItemIndex = nextResult.currentIndex ?: 0,
                    )
                } catch (e: Exception) {
                    lastException = e
                    // If first attempt fails and we have a videoId, try with fallback radio params
                    if (attempt == 0 && endpoint.videoId != null && endpoint.playlistId == null) {
                        endpoint = WatchEndpoint(
                            videoId = endpoint.videoId,
                            playlistId = "RDAMVM${endpoint.videoId}"
                        )
                    }
                }
            }
            throw lastException ?: Exception("Failed to get initial status")
        }
    }

    override fun hasNextPage(): Boolean = continuation != null

    override suspend fun nextPage(): List<MediaItem> {
        return withContext(IO) {
            var lastException: Throwable? = null
            
            for (attempt in 0..maxRetries) {
                try {
                    val nextResult = YouTube.next(endpoint, continuation).getOrThrow()
                    endpoint = nextResult.endpoint
                    continuation = nextResult.continuation
                    retryCount = 0
                    return@withContext tailorRecommendations(nextResult.items.map { it.toMediaItem() })
                } catch (e: Exception) {
                    lastException = e
                    retryCount++
                    if (retryCount >= maxRetries) {
                        continuation = null // Stop trying to load more
                    }
                }
            }
            throw lastException ?: Exception("Failed to get next page")
        }
    }

    private suspend fun tailorRecommendations(items: List<MediaItem>): List<MediaItem> {
        if (recommendationStyle == RecommendationEngineStyle.YOUTUBE_MUSIC) {
            return items
        }

        val db = database ?: return items
        
        val history = db.events().firstOrNull() ?: emptyList()
        val likedTracks = db.likedSongsByRowIdAsc().firstOrNull() ?: emptyList()
        
        val userArtists = (history.flatMap { eventWithSong -> eventWithSong.song.artists.map { a -> a.name } } + 
                           likedTracks.flatMap { song -> song.artists.map { a -> a.name } }).toSet()

        if (recommendationStyle == RecommendationEngineStyle.SPOTIFY) {
            // Sort by whether the artist is in user's history
            return items.sortedByDescending { mediaItem ->
                 val artistName = mediaItem.mediaMetadata.artist?.toString()
                 if (artistName != null && userArtists.any { userArtist -> userArtist.contains(artistName, ignoreCase = true) || artistName.contains(userArtist, ignoreCase = true) }) 1 else 0
            }
        }

        if (recommendationStyle == RecommendationEngineStyle.HYBRID) {
            // Interleave
            val personalized = items.filter { mediaItem ->
                 val artistName = mediaItem.mediaMetadata.artist?.toString()
                 artistName != null && userArtists.any { userArtist -> userArtist.contains(artistName, ignoreCase = true) || artistName.contains(userArtist, ignoreCase = true) }
            }
            val generic = items - personalized.toSet()
            val hybrid = mutableListOf<MediaItem>()
            var pIndex = 0
            var gIndex = 0
            while(pIndex < personalized.size || gIndex < generic.size) {
                if (pIndex < personalized.size) hybrid.add(personalized[pIndex++])
                if (gIndex < generic.size) hybrid.add(generic[gIndex++])
            }
            return hybrid
        }

        return items
    }

    companion object {
        /**
         * Creates a radio queue based on a song.
         * Uses only videoId to let YouTube personalize recommendations based on user's listening history.
         */
        fun radio(song: MediaMetadata): YouTubeQueue {
            return YouTubeQueue(
                WatchEndpoint(videoId = song.id),
                song
            )
        }
    }
}
