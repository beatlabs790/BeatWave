/**
 * BeatWave Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.beatwave.music.ui.screens.artist

import com.beatwave.music.ui.utils.appTopBarWindowInsets
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.ExperimentalFoundationApi

import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.Box
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.Row
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.Spacer
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.padding
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.lazy.itemsIndexed
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.lazy.rememberLazyListState
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.Icon
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.IconButton
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.MaterialTheme
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.Text
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.TopAppBar
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.TopAppBarScrollBehavior
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.runtime.Composable
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.runtime.getValue
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.Alignment
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.Modifier
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.platform.LocalContext
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.platform.LocalHapticFeedback
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.res.painterResource
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.res.pluralStringResource
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.unit.dp
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.navigation.NavController
import com.beatwave.music.LocalPlayerAwareWindowInsets
import com.beatwave.music.LocalPlayerConnection
import com.beatwave.music.R
import com.beatwave.music.constants.ArtistSongSortDescendingKey
import com.beatwave.music.constants.ArtistSongSortType
import com.beatwave.music.constants.ArtistSongSortTypeKey
import com.beatwave.music.constants.CONTENT_TYPE_HEADER
import com.beatwave.music.constants.HideExplicitKey
import com.beatwave.music.extensions.toMediaItem
import com.beatwave.music.playback.queues.ListQueue
import com.beatwave.music.ui.component.HideOnScrollFAB
import com.beatwave.music.ui.component.IconButton
import com.beatwave.music.ui.component.ListScrollRail
import com.beatwave.music.ui.component.LocalMenuState
import com.beatwave.music.ui.component.SongListItem
import com.beatwave.music.ui.component.SortHeader
import com.beatwave.music.ui.component.buildAlphabetSectionIndex
import com.beatwave.music.ui.menu.SongMenu
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import com.beatwave.music.ui.utils.backToMain
import com.beatwave.music.ui.utils.bounceClick
import com.beatwave.music.ui.utils.combinedBounceClick
import com.beatwave.music.utils.listItemShape
import com.beatwave.music.utils.rememberEnumPreference
import com.beatwave.music.utils.rememberPreference
import com.beatwave.music.viewmodels.ArtistSongsViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ArtistSongsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: ArtistSongsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val (sortType, onSortTypeChange) = rememberEnumPreference(
        ArtistSongSortTypeKey,
        ArtistSongSortType.CREATE_DATE
    )
    val (sortDescending, onSortDescendingChange) = rememberPreference(
        ArtistSongSortDescendingKey,
        true
    )
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)
    val artist by viewModel.artist.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val lazyListState = rememberLazyListState()

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        ) {
            item(
                key = "header",
                contentType = CONTENT_TYPE_HEADER,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    SortHeader(
                        sortType = sortType,
                        sortDescending = sortDescending,
                        onSortTypeChange = onSortTypeChange,
                        onSortDescendingChange = onSortDescendingChange,
                        sortTypeText = { sortType ->
                            when (sortType) {
                                ArtistSongSortType.CREATE_DATE -> R.string.sort_by_create_date
                                ArtistSongSortType.NAME -> R.string.sort_by_name
                                ArtistSongSortType.PLAY_TIME -> R.string.sort_by_play_time
                            }
                        },
                    )

                    Spacer(Modifier.weight(1f))

                    Text(
                        text = pluralStringResource(R.plurals.n_song, songs.size, songs.size),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            itemsIndexed(
                items = songs,
                key = { _, item -> item.id },
            ) { index, song ->
                SongListItem(
                    song = song,
                    showInLibraryIcon = true,
                    isActive = song.id == mediaMetadata?.id,
                    isPlaying = isPlaying,
                    shape = listItemShape(index, songs.size),
                    trailingContent = {
                        IconButton(
                            onClick = {
                                menuState.show {
                                    SongMenu(
                                        originalSong = song,
                                        navController = navController,
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = null,
                            )
                        }
                    },
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .combinedBounceClick(
                            onClick = {
                                if (song.id == mediaMetadata?.id) {
                                    playerConnection.togglePlayPause()
                                } else {
                                    playerConnection.playQueue(
                                        ListQueue(
                                            title = context.getString(R.string.queue_all_songs),
                                            items = songs.map { it.toMediaItem() },
                                            startIndex = index,
                                        ),
                                    )
                                }
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuState.show {
                                    SongMenu(
                                        originalSong = song,
                                        navController = navController,
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            },
                        )
                        .animateItem(),
                )
            }
        }

        TopAppBar(
            windowInsets = appTopBarWindowInsets(),
            title = { Text(artist?.artist?.name.orEmpty()) },
            navigationIcon = {
                IconButton(
                    onClick = navController::navigateUp,
                    onLongClick = navController::backToMain,
                ) {
                    Icon(
                        painterResource(R.drawable.arrow_back),
                        contentDescription = null,
                    )
                }
            },
        )

        ListScrollRail(
            lazyListState = lazyListState,
            itemCount = songs.size,
            sectionIndexMap = if (sortType == ArtistSongSortType.NAME) {
                remember(songs) { buildAlphabetSectionIndex(songs) { it.title } }
            } else {
                null
            },
        )

        HideOnScrollFAB(
            lazyListState = lazyListState,
            icon = R.drawable.shuffle,
            onClick = {
                playerConnection.playQueue(
                    ListQueue(
                        title = artist?.artist?.name,
                        items = songs.shuffled().map { it.toMediaItem() },
                    ),
                )
            },
        )
    }
}
