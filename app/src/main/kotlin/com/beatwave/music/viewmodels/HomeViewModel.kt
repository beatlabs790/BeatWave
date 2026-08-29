/**
 * BeatWave Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.beatwave.music.viewmodels

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.innertube.YouTube
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.Artist
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import kotlinx.coroutines.flow.combine
import com.music.innertube.models.WatchEndpoint
import com.music.innertube.models.BrowseEndpoint
import com.music.innertube.models.YTItem
import com.music.innertube.models.filterExplicit
import com.music.innertube.models.filterVideoSongs
import com.music.innertube.models.filterYoutubeShorts
import com.music.innertube.pages.ExplorePage
import com.music.innertube.pages.HomePage
import com.music.innertube.utils.completed
import com.beatwave.music.constants.DataSaverEnabledKey
import com.beatwave.music.constants.HideExplicitKey
import com.beatwave.music.constants.HideVideoSongsKey
import com.beatwave.music.constants.HideYoutubeShortsKey
import com.beatwave.music.constants.InnerTubeCookieKey
import com.beatwave.music.constants.LocalAlbumsByYearKey
import com.beatwave.music.constants.LocalOnlyModeKey
import com.beatwave.music.constants.LocalSongSortDescendingKey
import com.beatwave.music.constants.LocalSongSortTypeKey
import com.beatwave.music.constants.PlaylistSortType
import com.beatwave.music.constants.QuickPicks
import com.beatwave.music.constants.QuickPicksKey
import com.beatwave.music.constants.RecommendationEngine
import com.beatwave.music.constants.RecommendationEngineKey
import com.beatwave.music.constants.ShowWrappedCardKey
import com.beatwave.music.constants.SongSortType
import com.beatwave.music.constants.WrappedSeenKey
import com.beatwave.music.db.MusicDatabase
import com.beatwave.music.db.entities.Album
import com.beatwave.music.db.entities.LocalItem
import com.beatwave.music.db.entities.Playlist
import com.beatwave.music.db.entities.Song
import com.beatwave.music.db.entities.SpeedDialItem
import com.beatwave.music.extensions.filterVideoSongs
import com.beatwave.music.extensions.toEnum
import com.beatwave.music.models.SimilarRecommendation
import com.beatwave.music.ui.screens.wrapped.WrappedAudioService
import com.beatwave.music.ui.screens.wrapped.WrappedManager
import com.beatwave.music.utils.LocalAudioScanner
import com.beatwave.music.utils.LocalFolderIndex
import com.beatwave.music.utils.SyncUtils
import com.beatwave.music.utils.dataStore
import com.beatwave.music.utils.get
import com.beatwave.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import kotlin.random.Random

data class DailyDiscoverItem(
    val seed: Song,
    val recommendation: YTItem,
    val relatedEndpoint: BrowseEndpoint?
)

data class CommunityPlaylistItem(
    val playlist: PlaylistItem,
    val songs: List<SongItem>
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    val database: MusicDatabase,
    val syncUtils: SyncUtils,
    val wrappedManager: WrappedManager,
    private val wrappedAudioService: WrappedAudioService,
) : ViewModel() {
    val isRefreshing = MutableStateFlow(false)
    val isLoading = MutableStateFlow(false)
    val isRandomizing = MutableStateFlow(false)

    private val quickPicksEnum = context.dataStore.data.map {
        it[QuickPicksKey].toEnum(QuickPicks.QUICK_PICKS)
    }.distinctUntilChanged()

    val quickPicks = MutableStateFlow<List<Song>?>(null)
    val dailyDiscover = MutableStateFlow<List<DailyDiscoverItem>?>(null)
    val forgottenFavorites = MutableStateFlow<List<Song>?>(null)
    val keepListening = MutableStateFlow<List<LocalItem>?>(null)
    val similarRecommendations = MutableStateFlow<List<SimilarRecommendation>?>(null)
    val accountPlaylists = MutableStateFlow<List<PlaylistItem>?>(null)
    val homePage = MutableStateFlow<HomePage?>(null)
    val explorePage = MutableStateFlow<ExplorePage?>(null)
    val communityPlaylists = MutableStateFlow<List<CommunityPlaylistItem>?>(null)
    val selectedChip = MutableStateFlow<HomePage.Chip?>(null)
    private val previousHomePage = MutableStateFlow<HomePage?>(null)

    val allLocalItems = MutableStateFlow<List<LocalItem>>(emptyList())
    val allYtItems = MutableStateFlow<List<YTItem>>(emptyList())

    /** One switch decides whether Home talks to YouTube at all. */
    val localOnlyMode: StateFlow<Boolean> = context.dataStore.data
        .map { it[LocalOnlyModeKey] ?: false }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // Local-only Home rows. Room-backed, so a rescan updates them without a reload.
    val localSongs: StateFlow<List<Song>> = context.dataStore.data
        .map { prefs ->
            (prefs[LocalSongSortTypeKey].toEnum(SongSortType.NAME)) to (prefs[LocalSongSortDescendingKey] ?: false)
        }
        .distinctUntilChanged()
        .flatMapLatest { (sortType, descending) -> database.localSongs(sortType, descending) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    // Newest release first by default, not A-Z: an on-device library is something the
    // user assembled over time, so release order says more about it than the alphabet
    // does. Files with no year tag sort last -- SQLite puts NULLs at the end on DESC.
    val localAlbums: StateFlow<List<Album>> = context.dataStore.data
        .map { it[LocalAlbumsByYearKey] ?: true }
        .distinctUntilChanged()
        .flatMapLatest { byYear ->
            if (byYear) database.albumsLocalByYearDesc() else database.albumsLocalByNameAsc()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val localArtists: StateFlow<List<com.beatwave.music.db.entities.Artist>> = database.artistsLocalByNameAsc()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    // Playlists the user made here � a synced YouTube playlist has a browseId.
    val localPlaylists: StateFlow<List<Playlist>> =
        database.playlists(PlaylistSortType.CREATE_DATE, true)
            .map { playlists -> playlists.filter { it.playlist.browseId == null } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val localFolders = MutableStateFlow<List<LocalFolderIndex.Folder>>(emptyList())

    val speedDialItems: StateFlow<List<YTItem>> =
        combine(
            database.speedDialDao.getAll(),
            keepListening,
            quickPicks
        ) { pinned, keepListening, quick ->
            val pinnedItems = pinned.map { it.toYTItem() }
            val filled = pinnedItems.toMutableList()
            val targetSize = 27

            if (filled.size < targetSize) {
                // Keep Listening (History/Heavy Rotation)
                keepListening?.let { k ->
                    val needed = targetSize - filled.size
                    val available = k.filter { item ->
                        filled.none { p -> p.id == item.id }
                    }.mapNotNull { item ->
                        when (item) {
                            is Song -> SongItem(
                                id = item.id,
                                title = item.title,
                                artists = item.artists.map { Artist(name = it.name, id = it.id) },
                                thumbnail = item.thumbnailUrl ?: "",
                                explicit = false
                            )
                            is Album -> AlbumItem(
                                browseId = item.id,
                                playlistId = item.album.playlistId ?: "",
                                title = item.title,
                                artists = item.artists.map { Artist(name = it.name, id = it.id) },
                                year = item.album.year,
                                thumbnail = item.thumbnailUrl ?: ""
                            )
                            else -> null
                        }
                    }
                    filled.addAll(available.take(needed))
                }
            }

            if (filled.size < targetSize) {
                // Quick Picks
                quick?.let { q ->
                    val needed = targetSize - filled.size
                    val available = q.filter { song ->
                        filled.none { p -> p.id == song.id }
                    }.map { song ->
                        SongItem(
                            id = song.id,
                            title = song.title,
                            artists = song.artists.map { Artist(name = it.name, id = it.id) },
                            thumbnail = song.thumbnailUrl ?: "",
                            explicit = false
                        )
                    }
                    filled.addAll(available.take(needed))
                }
            }

            filled.take(targetSize)
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    suspend fun getRandomItem(): YTItem? {
        try {
            isRandomizing.value = true
            // Visual feedback for the animation
            kotlinx.coroutines.delay(1000)

            val userSongs = mutableListOf<YTItem>()
            val otherSources = mutableListOf<YTItem>()

            quickPicks.value?.let { songs ->
                userSongs.addAll(songs.map { song ->
                    SongItem(
                        id = song.id,
                        title = song.title,
                        artists = song.artists.map { Artist(name = it.name, id = it.id) },
                        thumbnail = song.thumbnailUrl ?: "",
                        explicit = false
                    )
                })
            }

            keepListening.value?.let { items ->
                items.forEach { item ->
                    when (item) {
                        is Song -> userSongs.add(SongItem(
                            id = item.id,
                            title = item.title,
                            artists = item.artists.map { Artist(name = it.name, id = it.id) },
                            thumbnail = item.thumbnailUrl ?: "",
                            explicit = false
                        ))
                        is Album -> otherSources.add(AlbumItem(
                            browseId = item.id,
                            playlistId = item.album.playlistId ?: "",
                            title = item.title,
                            artists = item.artists.map { Artist(name = it.name, id = it.id) },
                            year = item.album.year,
                            thumbnail = item.thumbnailUrl ?: ""
                        ))
                        else -> {}
                    }
                }
            }

            otherSources.addAll(allYtItems.value)

            // Probability: 80% User Songs, 20% Other Sources
            val item = if (userSongs.isNotEmpty() && (otherSources.isEmpty() || Random.nextFloat() < 0.8f)) {
                userSongs.distinctBy { it.id }.shuffled().firstOrNull()
            } else {
                otherSources.distinctBy { it.id }.shuffled().firstOrNull()
            } ?: userSongs.firstOrNull() ?: otherSources.firstOrNull()

            return item
        } finally {
            isRandomizing.value = false
        }
    }

    val accountName = MutableStateFlow("Guest")
    val accountImageUrl = MutableStateFlow<String?>(null)

    val showWrappedCard: StateFlow<Boolean> = context.dataStore.data.map { prefs ->
        val showWrappedPref = prefs[ShowWrappedCardKey] ?: false
        val seen = prefs[WrappedSeenKey] ?: false
        val isBeforeDate = LocalDate.now().isBefore(LocalDate.of(2026, 2, 1))

        isBeforeDate && (!seen || showWrappedPref)
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    val wrappedSeen: StateFlow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[WrappedSeenKey] ?: false
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun togglePin(item: YTItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val speedDialItem = SpeedDialItem.fromYTItem(item)
            val isPinned = database.speedDialDao.isPinned(speedDialItem.id).first()
            if (isPinned) {
                database.speedDialDao.delete(speedDialItem.id)
            } else {
                database.speedDialDao.insert(speedDialItem)
            }
        }
    }

    fun markWrappedAsSeen() {
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.edit {
                it[WrappedSeenKey] = true
            }
        }
    }
    // Track last processed cookie to avoid unnecessary updates
    private var lastProcessedCookie: String? = null
    // Track if we're currently processing account data
    private var isProcessingAccountData = false

    private suspend fun getDailyDiscover() {
        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false) || context.dataStore.get(DataSaverEnabledKey, false)
        val recEngine = context.dataStore.get(RecommendationEngineKey, RecommendationEngine.SPOTIFY.name)
        val likedSongs: List<Song> = database.likedSongsByCreateDateAsc().first()
        val recentSongs: List<Song> = database.events().first().mapNotNull { it.song }
        val mostPlayed: List<Song> = database.mostPlayedSongs(0L).first()
        val allLocal: List<Song> = database.songsByCreateDateAsc().first()
        val candidateSeeds: List<Song> = (likedSongs + recentSongs + mostPlayed + allLocal).distinctBy { it.id }

        val seeds: List<Song> = if (candidateSeeds.isNotEmpty()) {
            candidateSeeds.shuffled().take(5)
        } else emptyList()

        val items = java.util.Collections.synchronizedList(mutableListOf<DailyDiscoverItem>())

        if (recEngine == RecommendationEngine.SPOTIFY.name) {
            // SPOTIFY ALGORITHM
            val spotifySeedIds = java.util.Collections.synchronizedList(mutableListOf<String>())
            if (seeds.isNotEmpty()) {
                kotlinx.coroutines.coroutineScope {
                    seeds.map { localSong ->
                        launch(Dispatchers.IO) {
                            val query = "${localSong.song.title} ${localSong.artists.firstOrNull()?.name.orEmpty()}"
                            com.music.spotify.Spotify.searchTrack(query).getOrNull()?.let { spotifySeedIds.add(it) }
                        }
                    }.forEach { it.join() }
                }
            }

            val spotifyRecs = com.music.spotify.Spotify.getRecommendations(spotifySeedIds.toList(), limit = 35).getOrNull()
            if (!spotifyRecs.isNullOrEmpty()) {
                val defaultSeed = seeds.firstOrNull() ?: Song(
                    song = com.beatwave.music.db.entities.SongEntity(id = "seed", title = "Spotify Discovery", duration = 0),
                    artists = listOf(com.beatwave.music.db.entities.ArtistEntity(id = "artist", name = "Recommended for You"))
                )

                // Suggest both Songs and Albums from Spotify recommendations
                kotlinx.coroutines.coroutineScope {
                    // 1. Suggest Songs
                    spotifyRecs.take(24).map { spTrack ->
                        launch(Dispatchers.IO) {
                            val query = "${spTrack.name} ${spTrack.artists.firstOrNull()?.name.orEmpty()}"
                            val searchResult = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                            val ytItem = searchResult?.items?.firstOrNull() as? SongItem
                            if (ytItem != null && (!hideVideoSongs || !ytItem.isVideoSong) && !ytItem.explicit) {
                                val seed = if (seeds.isNotEmpty()) seeds.random() else defaultSeed
                                items.add(DailyDiscoverItem(seed, ytItem, null))
                            }
                        }
                    }.forEach { it.join() }

                    // 2. Suggest Albums at the same time
                    val uniqueAlbums = spotifyRecs.mapNotNull { it.album }
                        .filter { it.name.isNotBlank() }
                        .distinctBy { it.name.lowercase() }
                        .take(6)

                    uniqueAlbums.map { spAlbum ->
                        launch(Dispatchers.IO) {
                            val matchingTrack = spotifyRecs.firstOrNull { it.album?.id == spAlbum.id || it.name == spAlbum.name }
                            val artistName = matchingTrack?.artists?.firstOrNull()?.name.orEmpty()
                            val albumQuery = "${spAlbum.name} $artistName".trim()
                            val albumSearch = YouTube.search(albumQuery, YouTube.SearchFilter.FILTER_ALBUM).getOrNull()
                            val albumItem = albumSearch?.items?.firstOrNull() as? AlbumItem
                            if (albumItem != null) {
                                val seed = if (seeds.isNotEmpty()) seeds.random() else defaultSeed
                                items.add(DailyDiscoverItem(seed, albumItem, null))
                            }
                        }
                    }.forEach { it.join() }
                }
            }
        }

        // Fallback to YOUTUBE ALGORITHM if Spotify failed or if setting is YouTube
        if (items.isEmpty()) {
            if (seeds.isNotEmpty()) {
                kotlinx.coroutines.coroutineScope {
                    seeds.map { seed ->
                        launch(Dispatchers.IO) {
                            val endpoint = YouTube.next(WatchEndpoint(videoId = seed.id)).getOrNull()?.relatedEndpoint
                            if (endpoint != null) {
                                YouTube.related(endpoint).onSuccess { page ->
                                    val songRecs = page.songs
                                        .filter { item ->
                                            if (hideVideoSongs && item.isVideoSong) return@filter false
                                            if (item.explicit) return@filter false
                                            true
                                        }
                                        .shuffled()

                                    songRecs.take(5).forEach { recommendation ->
                                        if (recommendation.id != seed.id) {
                                            items.add(DailyDiscoverItem(seed, recommendation, endpoint))
                                        }
                                    }

                                    // Also include distinct album suggestions from YouTube Related page
                                    page.albums.shuffled().take(2).forEach { albumRec ->
                                        items.add(DailyDiscoverItem(seed, albumRec, endpoint))
                                    }
                                }
                            }
                        }
                    }.forEach { it.join() }
                }
            } else {
                // If user is brand new with no local songs at all, fetch Explore page new releases & charts
                val explore = YouTube.explore().getOrNull()
                val defaultSeed = Song(
                    song = com.beatwave.music.db.entities.SongEntity(id = "new", title = "New Releases", duration = 0),
                    artists = listOf(com.beatwave.music.db.entities.ArtistEntity(id = "artist", name = "Featured"))
                )
                explore?.newReleaseAlbums?.shuffled()?.take(6)?.forEach { album ->
                    items.add(DailyDiscoverItem(defaultSeed, album, null))
                }
                val hits = YouTube.search("top songs", YouTube.SearchFilter.FILTER_SONG).getOrNull()
                hits?.items?.filterIsInstance<SongItem>()?.shuffled()?.take(20)?.forEach { songItem ->
                    if (!hideVideoSongs || !songItem.isVideoSong) {
                        items.add(DailyDiscoverItem(defaultSeed, songItem, null))
                    }
                }
            }
        }

        dailyDiscover.value = items.toList().distinctBy { it.recommendation.id }.shuffled()
    }

    private suspend fun getQuickPicks() {
        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false) || context.dataStore.get(DataSaverEnabledKey, false)
        when (quickPicksEnum.first()) {
            QuickPicks.QUICK_PICKS -> {
                val relatedSongs = database.quickPicks().first().filterVideoSongs(hideVideoSongs)
                val forgotten = database.forgottenFavorites().first().filterVideoSongs(hideVideoSongs).take(8)
                val recEngine = context.dataStore.get(RecommendationEngineKey, RecommendationEngine.SPOTIFY.name)

                val recentSong: Song? = database.events().first().firstOrNull()?.song 
                    ?: database.likedSongsByCreateDateAsc().first().firstOrNull()
                    ?: database.songsByCreateDateAsc().first().firstOrNull()
                val ytSimilarSongs = mutableListOf<Song>()

                if (recentSong != null) {
                    if (recEngine == RecommendationEngine.SPOTIFY.name) {
                        // SPOTIFY ALGORITHM
                        val query = "${recentSong.title} ${recentSong.artists.firstOrNull()?.name.orEmpty()}"
                        val spotifySeedId = com.music.spotify.Spotify.searchTrack(query).getOrNull()
                        val seedsList = listOfNotNull(spotifySeedId)
                        val spotifyRecs = com.music.spotify.Spotify.getRecommendations(seedsList, limit = 25).getOrNull()
                        if (!spotifyRecs.isNullOrEmpty()) {
                            val ytSimilarSongsSync = java.util.Collections.synchronizedList(ytSimilarSongs)
                            kotlinx.coroutines.coroutineScope {
                                spotifyRecs.map { spTrack ->
                                    launch(Dispatchers.IO) {
                                        val sQuery = "${spTrack.name} ${spTrack.artists.firstOrNull()?.name.orEmpty()}"
                                        val searchResult = YouTube.search(sQuery, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                                        val ytItem = searchResult?.items?.firstOrNull() as? SongItem
                                        if (ytItem != null && (!hideVideoSongs || !ytItem.isVideoSong) && !ytItem.explicit) {
                                            val localSong = database.song(ytItem.id).first() ?: Song(
                                                song = com.beatwave.music.db.entities.SongEntity(
                                                    id = ytItem.id,
                                                    title = ytItem.title,
                                                    duration = ytItem.duration ?: 0,
                                                    thumbnailUrl = ytItem.thumbnail
                                                ),
                                                artists = ytItem.artists.map { com.beatwave.music.db.entities.ArtistEntity(id = it.id.orEmpty(), name = it.name) }
                                            )
                                            ytSimilarSongsSync.add(localSong)
                                        }
                                    }
                                }.forEach { it.join() }
                            }
                        }
                    }

                    // Fallback to YOUTUBE ALGORITHM if Spotify failed or if setting is YouTube
                    if (ytSimilarSongs.isEmpty()) {
                        val endpoint = YouTube.next(WatchEndpoint(videoId = recentSong.id)).getOrNull()?.relatedEndpoint
                        if (endpoint != null) {
                            YouTube.related(endpoint).onSuccess { page ->
                                page.songs.take(20).forEach { ytSong ->
                                    val localSong = database.song(ytSong.id).first() ?: Song(
                                        song = com.beatwave.music.db.entities.SongEntity(
                                            id = ytSong.id,
                                            title = ytSong.title,
                                            duration = ytSong.duration ?: 0,
                                            thumbnailUrl = ytSong.thumbnail
                                        ),
                                        artists = ytSong.artists.map { com.beatwave.music.db.entities.ArtistEntity(id = it.id.orEmpty(), name = it.name) }
                                    )
                                    if (!hideVideoSongs || !localSong.song.isVideo) {
                                        ytSimilarSongs.add(localSong)
                                    }
                                }
                            }
                        }
                    }
                }

                val combined = (relatedSongs + forgotten + ytSimilarSongs)
                    .distinctBy { it.id }
                    .shuffled()
                    .take(35)

                quickPicks.value = combined.ifEmpty { relatedSongs.shuffled().take(35) }
            }
            QuickPicks.LAST_LISTEN -> {
                val song = database.events().first().firstOrNull()?.song
                if (song != null && database.hasRelatedSongs(song.id)) {
                    quickPicks.value = database.getRelatedSongs(song.id).first().filterVideoSongs(hideVideoSongs).shuffled().take(30)
                }
            }
        }
    }

    private suspend fun getCommunityPlaylists() {
        val fromTimeStamp = System.currentTimeMillis() - 86400000L * 7 * 4
        val artistSeeds = database.mostPlayedArtists(fromTimeStamp, limit = 10).first()
            .filter { it.artist.isYouTubeArtist }
            .shuffled().take(3)
        val songSeeds = database.mostPlayedSongs(fromTimeStamp, limit = 5).first()
            .shuffled().take(2)

        val candidatePlaylists = java.util.Collections.synchronizedList(mutableListOf<PlaylistItem>())

        kotlinx.coroutines.coroutineScope {
            artistSeeds.map { seed ->
                launch(Dispatchers.IO) {
                    YouTube.artist(seed.id).onSuccess { page ->
                        page.sections.forEach { section ->
                            section.items.filterIsInstance<PlaylistItem>().forEach { playlist ->
                                if (playlist.author?.name != "YouTube Music" &&
                                    playlist.author?.name != "YouTube" &&
                                    playlist.author?.name != "Playlist" &&
                                    playlist.author?.name != seed.artist.name &&
                                    !playlist.id.startsWith("RD") &&
                                    !playlist.id.startsWith("OLAK")
                                ) {
                                    candidatePlaylists.add(playlist)
                                }
                            }
                        }
                    }
                }
            }

            songSeeds.map { seed ->
                launch(Dispatchers.IO) {
                    val endpoint = YouTube.next(WatchEndpoint(videoId = seed.id)).getOrNull()?.relatedEndpoint
                    if (endpoint != null) {
                        YouTube.related(endpoint).onSuccess { page ->
                            page.playlists.forEach { playlist ->
                                if (playlist.author?.name != "YouTube Music" &&
                                    playlist.author?.name != "YouTube" &&
                                    playlist.author?.name != "Playlist" &&
                                    !playlist.id.startsWith("RD") &&
                                    !playlist.id.startsWith("OLAK")
                                ) {
                                    candidatePlaylists.add(playlist)
                                }
                            }
                        }
                    }
                }
            }
        }

        val uniqueCandidates = candidatePlaylists.distinctBy { it.id }.shuffled().take(5)

        val playlists = java.util.Collections.synchronizedList(mutableListOf<CommunityPlaylistItem>())

        kotlinx.coroutines.coroutineScope {
            uniqueCandidates.map { playlist ->
                launch(Dispatchers.IO) {
                    YouTube.playlist(playlist.id).onSuccess { page ->
                        val songs = page.songs.take(10)
                        if (songs.isNotEmpty()) {
                            // Use song count from the playlist page if available, otherwise use original
                            val songCountText = page.playlist.songCountText ?: playlist.songCountText
                            val updatedPlaylist = playlist.copy(songCountText = songCountText)
                            playlists.add(CommunityPlaylistItem(updatedPlaylist, songs))
                        }
                    }
                }
            }.forEach { it.join() }
        }

        communityPlaylists.value = playlists.shuffled()
    }

    /**
     * Phase 1: Reads all local DB data and immediately drops the loading indicator.
     * Guarantees the UI shows real content before any network call is made.
     */
    private suspend fun loadLocalDataPhase() {
        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false) || context.dataStore.get(DataSaverEnabledKey, false)

        getQuickPicks()

        // On-device tracks are the whole of local-only mode, which is a separate Home
        // (see localHomeContent). Leaking them into the online feed put files with no
        // artwork and no metadata beside YouTube rows that have both, so every shelf
        // they landed in looked broken.
        quickPicks.value = quickPicks.value?.filterNot { it.song.isLocal }

        forgottenFavorites.value = database.forgottenFavorites().first()
            .filterVideoSongs(hideVideoSongs).filterNot { it.song.isLocal }.shuffled().take(20)

        val fromTimeStamp = System.currentTimeMillis() - 86400000L * 7 * 2
        val keepListeningSongs = database.mostPlayedSongs(fromTimeStamp, limit = 15, offset = 5).first()
            .filterVideoSongs(hideVideoSongs).filterNot { it.song.isLocal }.shuffled().take(10)
        val keepListeningAlbums = database.mostPlayedAlbums(fromTimeStamp, limit = 8, offset = 2).first()
            .filter { !it.album.isLocal && it.album.thumbnailUrl != null }.shuffled().take(5)
        val keepListeningArtists = database.mostPlayedArtists(fromTimeStamp).first()
            .filter { it.artist.isYouTubeArtist && it.artist.thumbnailUrl != null }.shuffled().take(5)
        keepListening.value = (keepListeningSongs + keepListeningAlbums + keepListeningArtists).shuffled()

        allLocalItems.value = (quickPicks.value.orEmpty() + forgottenFavorites.value.orEmpty() + keepListening.value.orEmpty())
            .filter { it is Song || it is Album }
    }

    /**
     * Fetches all three recommendation sources (artists, songs, albums) concurrently
     * using async/awaitAll, replacing the previous sequential mapNotNull chains.
     */
    private suspend fun loadSimilarRecommendations() {
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false) || context.dataStore.get(DataSaverEnabledKey, false)
        val fromTimeStamp = System.currentTimeMillis() - 86400000L * 7 * 2

        coroutineScope {
            val artistDeferreds = database.mostPlayedArtists(fromTimeStamp, limit = 15).first()
                .filter { it.artist.isYouTubeArtist }
                .shuffled().take(4)
                .map { artist ->
                    async(Dispatchers.IO) {
                        val items = mutableListOf<YTItem>()
                        YouTube.artist(artist.id).onSuccess { page ->
                            page.sections.takeLast(3).forEach { section -> items += section.items }
                        }
                        SimilarRecommendation(
                            title = artist,
                            items = items
                                .distinctBy { item -> item.id }
                                .filterExplicit(hideExplicit)
                                .filterVideoSongs(hideVideoSongs)
                                .shuffled()
                                .take(12)
                                .ifEmpty { return@async null }
                        )
                    }
                }

            val songDeferreds = database.mostPlayedSongs(fromTimeStamp, limit = 15).first()
                .filter { it.album != null }
                .shuffled().take(3)
                .map { song ->
                    async(Dispatchers.IO) {
                        val endpoint = YouTube.next(WatchEndpoint(videoId = song.id)).getOrNull()?.relatedEndpoint
                            ?: return@async null
                        val page = YouTube.related(endpoint).getOrNull() ?: return@async null
                        SimilarRecommendation(
                            title = song,
                            items = (page.songs.shuffled().take(10) +
                                    page.albums.shuffled().take(5) +
                                    page.artists.shuffled().take(3) +
                                    page.playlists.shuffled().take(3))
                                .distinctBy { it.id }
                                .filterExplicit(hideExplicit)
                                .filterVideoSongs(hideVideoSongs)
                                .shuffled()
                                .ifEmpty { return@async null }
                        )
                    }
                }

            val albumDeferreds = database.mostPlayedAlbums(fromTimeStamp, limit = 10).first()
                .filter { it.album.thumbnailUrl != null }
                .shuffled().take(2)
                .map { album ->
                    async(Dispatchers.IO) {
                        val items = mutableListOf<YTItem>()
                        YouTube.album(album.id).onSuccess { page ->
                            page.otherVersions.let { items += it }
                        }
                        album.artists.firstOrNull()?.id?.let { artistId ->
                            YouTube.artist(artistId).onSuccess { page ->
                                page.sections.lastOrNull()?.items?.let { items += it }
                            }
                        }
                        SimilarRecommendation(
                            title = album,
                            items = items
                                .distinctBy { it.id }
                                .filterExplicit(hideExplicit)
                                .filterVideoSongs(hideVideoSongs)
                                .shuffled()
                                .take(10)
                                .ifEmpty { return@async null }
                        )
                    }
                }

            val results = (artistDeferreds + songDeferreds + albumDeferreds).awaitAll()
            similarRecommendations.value = results.filterNotNull().shuffled()
        }
    }

    /**
     * Phase 2: Fires all network sections concurrently.
     * Because isLoading is already false, each section streams into the UI
     * as its data arrives — no spinner blocking the user.
     */
    private suspend fun loadNetworkDataPhase() {
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false) || context.dataStore.get(DataSaverEnabledKey, false)
        val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)

        coroutineScope {
            launch(Dispatchers.IO) { getDailyDiscover() }
            launch(Dispatchers.IO) { getCommunityPlaylists() }
            launch(Dispatchers.IO) { loadSimilarRecommendations() }
            launch(Dispatchers.IO) {
                YouTube.home().onSuccess { page ->
                    homePage.value = page.copy(
                        sections = page.sections.mapNotNull { section ->
                            val filteredItems = section.items
                                .filterExplicit(hideExplicit)
                                .filterVideoSongs(hideVideoSongs)
                                .filterYoutubeShorts(hideYoutubeShorts)
                            if (filteredItems.isEmpty()) null else section.copy(items = filteredItems)
                        }
                    )
                }.onFailure { reportException(it) }
            }
            launch(Dispatchers.IO) {
                YouTube.explore().onSuccess { page ->
                    explorePage.value = page.copy(
                        newReleaseAlbums = page.newReleaseAlbums.filterExplicit(hideExplicit)
                    )
                }.onFailure { reportException(it) }
            }
            if (YouTube.cookie != null) {
                launch(Dispatchers.IO) { loadAccountPlaylists() }
            }
        }

        // Update combined YT items once all network data has settled
        allYtItems.value = similarRecommendations.value?.flatMap { it.items }.orEmpty() +
                homePage.value?.sections?.flatMap { it.items }.orEmpty()
    }

    private suspend fun load() {
        isLoading.value = true

        // Local-only mode never touches the network � the Room flows above already
        // feed Home, so the only thing left to fetch is the folder index.
        if (localOnlyMode.value) {
            localFolders.value = runCatching { LocalFolderIndex.load(context) }.getOrDefault(emptyList())
            isLoading.value = false
            return
        }

        // Phase 1: Local DB only — UI renders immediately after this
        loadLocalDataPhase()
        isLoading.value = false

        // Phase 2: All network sections in parallel — streams in progressively
        loadNetworkDataPhase()
    }

    /** Pull-to-refresh in local-only mode rescans the device instead of the network. */
    private suspend fun rescanLocal() {
        runCatching { LocalAudioScanner.scanAndInsert(context, database) }
        localFolders.value = runCatching { LocalFolderIndex.load(context) }.getOrDefault(emptyList())
    }

    private val _isLoadingMore = MutableStateFlow(false)
    fun loadMoreYouTubeItems(continuation: String?) {
        if (continuation == null || _isLoadingMore.value) return
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false) || context.dataStore.get(DataSaverEnabledKey, false)
        val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)

        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingMore.value = true
            val nextSections = YouTube.home(continuation).getOrNull() ?: run {
                _isLoadingMore.value = false
                return@launch
            }

            homePage.value = nextSections.copy(
                chips = homePage.value?.chips,
                sections = (homePage.value?.sections.orEmpty() + nextSections.sections).mapNotNull { section ->
                    val filteredItems = section.items.filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs).filterYoutubeShorts(hideYoutubeShorts)
                    if (filteredItems.isEmpty()) null else section.copy(items = filteredItems)
                }
            )
            _isLoadingMore.value = false
        }
    }

    fun toggleChip(chip: HomePage.Chip?) {
        if (chip == null || chip == selectedChip.value && previousHomePage.value != null) {
            homePage.value = previousHomePage.value
            previousHomePage.value = null
            selectedChip.value = null
            return
        }

        if (selectedChip.value == null) {
            previousHomePage.value = homePage.value
        }

        viewModelScope.launch(Dispatchers.IO) {
            val hideExplicit = context.dataStore.get(HideExplicitKey, false)
            val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false) || context.dataStore.get(DataSaverEnabledKey, false)
            val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
            val nextSections = YouTube.home(params = chip.endpoint?.params).getOrNull() ?: return@launch

            homePage.value = nextSections.copy(
                chips = homePage.value?.chips,
                sections = nextSections.sections.map { section ->
                    section.copy(items = section.items.filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs).filterYoutubeShorts(hideYoutubeShorts))
                }
            )
            selectedChip.value = chip
        }
    }

    private suspend fun loadAccountPlaylists() {
        val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
        YouTube.library("FEmusic_liked_playlists").completed().onSuccess {
            accountPlaylists.value = it.items.filterIsInstance<PlaylistItem>()
                .filterNot { it.id == "SE" }
                .filterYoutubeShorts(hideYoutubeShorts)
        }.onFailure {
            reportException(it)
        }
    }

    fun refresh() {
        if (isRefreshing.value) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                isRefreshing.value = true
                if (localOnlyMode.value) rescanLocal() else load()
            } finally {
                isRefreshing.value = false
            }
        }
        if (localOnlyMode.value) return
        // Run sync when user manually refreshes
        viewModelScope.launch(Dispatchers.IO) {
            syncUtils.tryAutoSync()
        }
    }

    init {

        // Load home data. Re-runs when local-only mode is toggled, so Home swaps
        // between the YouTube feed and the on-device library without a restart.
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.data
                .map { it[InnerTubeCookieKey] }
                .distinctUntilChanged()
                .first()

            localOnlyMode.collect { load() }
        }

        // Run sync in separate coroutine with cooldown to avoid blocking UI
        viewModelScope.launch(Dispatchers.IO) {
            if (!localOnlyMode.first()) syncUtils.tryAutoSync()
        }

        // Prepare wrapped data in background
        viewModelScope.launch(Dispatchers.IO) {
            showWrappedCard.collect { shouldShow ->
                if (shouldShow && !wrappedManager.state.value.isDataReady) {
                    try {
                        wrappedManager.prepare()
                        val state = wrappedManager.state.first { it.isDataReady }
                        val trackMap = state.trackMap
                        if (trackMap.isNotEmpty()) {
                            val firstTrackId = trackMap.entries.first().value
                            wrappedAudioService.prepareTrack(firstTrackId)
                        }
                    } catch (e: Exception) {
                        reportException(e)
                    }
                }
            }
        }

        // Listen for cookie changes and reload account data
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.data
                .map { it[InnerTubeCookieKey] }
                .collect { cookie ->
                    // Avoid processing if already processing
                    if (isProcessingAccountData) return@collect

                    // Always process cookie changes, even if same value (for logout/login scenarios)
                    lastProcessedCookie = cookie
                    isProcessingAccountData = true

                    try {
                        if (cookie != null && cookie.isNotEmpty()) {

                            // Update YouTube.cookie manually to ensure it's set
                            YouTube.cookie = cookie

                            // Fetch new account data
                            YouTube.accountInfo().onSuccess { info ->
                                accountName.value = info.name
                                accountImageUrl.value = info.thumbnailUrl
                            }.onFailure {
                                if (it.message != "Active account info not found in header") {
                                    reportException(it)
                                }
                            }
                        } else {
                            accountName.value = "Guest"
                            accountImageUrl.value = null
                            accountPlaylists.value = null
                        }
                    } finally {
                        isProcessingAccountData = false
                    }
                }
        }

        // Listen for HideYoutubeShorts preference changes and reload account playlists instantly
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.data
                .map { it[HideYoutubeShortsKey] ?: false }
                .distinctUntilChanged()
                .collect {
                    if (YouTube.cookie != null && accountPlaylists.value != null) {
                        loadAccountPlaylists()
                    }
                }
        }
    }
}
