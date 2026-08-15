/**
 * BeatWave Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.beatwave.music.ui.screens

import com.beatwave.music.ui.utils.appTopBarWindowInsets
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.ExperimentalFoundationApi

import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.lazy.grid.GridCells
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.lazy.grid.GridItemSpan
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.lazy.grid.items
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.Icon
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
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.runtime.getValue
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.runtime.rememberCoroutineScope
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.Modifier
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.platform.LocalHapticFeedback
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.res.painterResource
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.res.stringResource
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.unit.dp
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.navigation.NavController
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import com.beatwave.music.LocalPlayerAwareWindowInsets
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import com.beatwave.music.R
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import com.beatwave.music.ui.utils.rememberGridColumns
import com.beatwave.music.constants.GridItemSize
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import com.beatwave.music.constants.GridItemsSizeKey
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import com.beatwave.music.constants.GridThumbnailHeight
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import com.beatwave.music.ui.component.ChipsRow
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import com.beatwave.music.ui.component.IconButton
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import com.beatwave.music.ui.component.LocalMenuState
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import com.beatwave.music.ui.component.YouTubeGridItem
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import com.beatwave.music.ui.component.shimmer.GridItemPlaceHolder
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import com.beatwave.music.ui.component.shimmer.ShimmerHost
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import com.beatwave.music.ui.menu.YouTubeAlbumMenu
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import com.beatwave.music.ui.menu.YouTubeArtistMenu
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import com.beatwave.music.ui.menu.YouTubePlaylistMenu
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import com.beatwave.music.ui.utils.backToMain
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import com.beatwave.music.ui.utils.bounceClick
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import com.beatwave.music.ui.utils.combinedBounceClick
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import com.beatwave.music.utils.rememberEnumPreference
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import com.beatwave.music.viewmodels.AccountContentType
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import com.beatwave.music.viewmodels.AccountViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AccountScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current

    val coroutineScope = rememberCoroutineScope()

    val playlists by viewModel.playlists.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val selectedContentType by viewModel.selectedContentType.collectAsState()
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)

    LazyVerticalGrid(
        columns = rememberGridColumns(),
        contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ChipsRow(
                chips = listOf(
                    AccountContentType.PLAYLISTS to stringResource(R.string.filter_playlists),
                    AccountContentType.ALBUMS to stringResource(R.string.filter_albums),
                    AccountContentType.ARTISTS to stringResource(R.string.filter_artists),
                ),
                currentValue = selectedContentType,
                onValueUpdate = { viewModel.setSelectedContentType(it) },
            )
        }

        when (selectedContentType) {
            AccountContentType.PLAYLISTS -> {
                items(
                    items = playlists.orEmpty().distinctBy { it.id },
                    key = { it.id },
                ) { item ->
                    YouTubeGridItem(
                        item = item,
                        fillMaxWidth = true,
                        modifier = Modifier
                            .combinedBounceClick(
                                onClick = {
                                    navController.navigate("online_playlist/${item.id}")
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        YouTubePlaylistMenu(
                                            playlist = item,
                                            coroutineScope = coroutineScope,
                                            onDismiss = menuState::dismiss,
                                        )
                                    }
                                },
                            ),
                    )
                }

                if (playlists == null) {
                    items(8) {
                        ShimmerHost {
                            GridItemPlaceHolder(fillMaxWidth = true)
                        }
                    }
                }
            }

            AccountContentType.ALBUMS -> {
                items(
                    items = albums.orEmpty().distinctBy { it.id },
                    key = { it.id }
                ) { item ->
                    YouTubeGridItem(
                        item = item,
                        fillMaxWidth = true,
                        modifier = Modifier
                            .combinedBounceClick(
                                onClick = {
                                    navController.navigate("album/${item.id}")
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        YouTubeAlbumMenu(
                                            albumItem = item,
                                            navController = navController,
                                            onDismiss = menuState::dismiss
                                        )
                                    }
                                }
                            )
                    )
                }

                if (albums == null) {
                    items(8) {
                        ShimmerHost {
                            GridItemPlaceHolder(fillMaxWidth = true)
                        }
                    }
                }
            }

            AccountContentType.ARTISTS -> {
                items(
                    items = artists.orEmpty().distinctBy { it.id },
                    key = { it.id }
                ) { item ->
                    YouTubeGridItem(
                        item = item,
                        fillMaxWidth = true,
                        modifier = Modifier
                            .combinedBounceClick(
                                onClick = {
                                    navController.navigate("artist/${item.id}")
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        YouTubeArtistMenu(
                                            artist = item,
                                            onDismiss = menuState::dismiss
                                        )
                                    }
                                }
                            )
                    )
                }

                if (artists == null) {
                    items(8) {
                        ShimmerHost {
                            GridItemPlaceHolder(fillMaxWidth = true)
                        }
                    }
                }
            }
        }
    }

    TopAppBar(
            windowInsets = appTopBarWindowInsets(),
        title = { Text(stringResource(R.string.account)) },
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
}
