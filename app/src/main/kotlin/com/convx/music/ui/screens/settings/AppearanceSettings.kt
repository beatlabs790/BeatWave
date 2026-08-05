/**
 * BeatWave Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convx.music.ui.screens.settings

import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.ui.utils.appTopBarWindowInsets
import android.app.Activity
import com.convx.music.ui.utils.appTopBarWindowInsets
import android.content.Context
import com.convx.music.ui.utils.appTopBarWindowInsets
import android.content.Intent
import com.convx.music.ui.utils.appTopBarWindowInsets
import android.os.Build
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.core.content.edit
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.ui.utils.bounceClick
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.Arrangement
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.Column
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.Row
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.Spacer
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.height
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.padding
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.size
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.rememberScrollState
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.verticalScroll
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.Icon
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.MaterialTheme
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.SnackbarHostState
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.ui.component.GlassSwitchCompat as Switch
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.SwitchDefaults
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.Text
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.TextButton
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.TopAppBar
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.TopAppBarScrollBehavior
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.runtime.Composable
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.runtime.getValue
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.runtime.mutableFloatStateOf
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.runtime.mutableStateOf
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.runtime.remember
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.runtime.saveable.rememberSaveable
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.runtime.setValue
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.Alignment
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.Modifier
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.res.painterResource
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.res.stringResource
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.unit.dp
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.navigation.NavController
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.LocalPlayerAwareWindowInsets
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.R
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.CanvasSource
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.CanvasSourceKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.CanvasThumbnailAnimationKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.ChipSortTypeKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.CropAlbumArtKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.DefaultOpenTabKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.DensityScale
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.DensityScaleKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.DynamicThemeKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.EnableDynamicIconKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.EnableSettingsPopupKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.EnableHighRefreshRateKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.EnableLyricsThumbnailPlayPauseKey
import com.convx.music.constants.OneTapFullscreenLyricsKey
import com.convx.music.constants.FullscreenLyricsCollapseTopKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.IosOverscrollKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.GridItemSize
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.GridItemsSizeKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.HidePlayerThumbnailKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.LibraryFilter
import com.convx.music.constants.LibraryIconsOnlyKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.ListenTogetherInTopBarKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.LyricsAnimationStyle
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.LyricsAnimationStyleKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.LyricsStandardBlurKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.LyricsTextPositionKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.LyricsTextSizeKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.ShowCachedPlaylistKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.ShowDownloadedPlaylistKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.ShowLikedPlaylistKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.ShowTopPlaylistKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.ShowUploadedPlaylistKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.SlimNavBarKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.SquigglySliderKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.SwipeSensitivityKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.SwipeThumbnailKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.SwipeLyricsKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.SwipeToRemoveSongKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.SwipeToSongKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.ThumbnailCornerRadiusKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.UseNewMiniPlayerDesignKey
import com.convx.music.constants.MiniBarTabStyleKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.UseNewPlayerDesignKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.ui.component.ThumbnailCornerRadiusModal
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.ui.component.DefaultDialog
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.ui.component.EnumDialog
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.ui.component.IconButton
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.ui.component.Material3SettingsGroup
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.ui.component.Material3SettingsItem
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.ui.utils.backToMain
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.utils.rememberEnumPreference
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.utils.rememberPreference
import com.convx.music.ui.utils.appTopBarWindowInsets
import kotlin.math.roundToInt
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.LyricsClickKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.AppleMusicLyricsBlurKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.LyricsGlowEffectKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.LyricsLineSpacingKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.LyricsScrollKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.ShowAudioQualityBadgeKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.MiniPlayerWaveformKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.ShowCommentButtonKey
import com.convx.music.constants.AppFont
import com.convx.music.constants.SelectedFontKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    activity: Activity,
    snackbarHostState: SnackbarHostState,
) {
    val (_, _) = rememberPreference(UseNewMiniPlayerDesignKey, defaultValue = true)
    val (miniBarTabStyle, onMiniBarTabStyleChange) = rememberPreference(MiniBarTabStyleKey, defaultValue = false)
    val (_, _) = rememberPreference(DynamicThemeKey, defaultValue = true)
    val (_, _) = rememberPreference(EnableDynamicIconKey, defaultValue = true)
    val (useNewPlayerDesign, _) = rememberPreference(UseNewPlayerDesignKey, defaultValue = false)
    val (showAudioQualityBadge, onShowAudioQualityBadgeChange) = rememberPreference(
        ShowAudioQualityBadgeKey,
        defaultValue = true
    )
    val (miniPlayerWaveform, onMiniPlayerWaveformChange) = rememberPreference(
        MiniPlayerWaveformKey,
        defaultValue = true
    )
    val (selectedFontValue) = rememberPreference(
        SelectedFontKey,
        defaultValue = AppFont.SYSTEM.value
    )

    val (libraryIconsOnly, onLibraryIconsOnlyChange) = rememberPreference(
        LibraryIconsOnlyKey,
        defaultValue = true
    )

    val (enableHighRefreshRate, onEnableHighRefreshRateChange) = rememberPreference(
        EnableHighRefreshRateKey,
        defaultValue = true
    )
    val (iosOverscroll, onIosOverscrollChange) = rememberPreference(
        IosOverscrollKey,
        defaultValue = false
    )
    val (enableSettingsPopup, onEnableSettingsPopupChange) = rememberPreference(
        EnableSettingsPopupKey,
        defaultValue = false
    )

    val (hidePlayerThumbnail, onHidePlayerThumbnailChange) = rememberPreference(
        HidePlayerThumbnailKey,
        defaultValue = false
    )
    val (cropAlbumArt, onCropAlbumArtChange) = rememberPreference(
        CropAlbumArtKey,
        defaultValue = false
    )
    val (defaultOpenTab, onDefaultOpenTabChange) = rememberEnumPreference(
        DefaultOpenTabKey,
        defaultValue = NavigationTab.HOME
    )
    val (lyricsPosition, onLyricsPositionChange) = rememberEnumPreference(
        LyricsTextPositionKey,
        defaultValue = LyricsPosition.LEFT
    )
    val (lyricsClick, onLyricsClickChange) = rememberPreference(LyricsClickKey, defaultValue = true)
    val (lyricsScroll, onLyricsScrollChange) = rememberPreference(
        LyricsScrollKey,
        defaultValue = true
    )
    val (lyricsAnimationStyle, onLyricsAnimationStyleChange) = rememberEnumPreference(
        LyricsAnimationStyleKey,
        defaultValue = LyricsAnimationStyle.beatwave_1
    )
    val (lyricsTextSize, onLyricsTextSizeChange) = rememberPreference(LyricsTextSizeKey, defaultValue = 30f)
    val (lyricsLineSpacing, onLyricsLineSpacingChange) = rememberPreference(LyricsLineSpacingKey, defaultValue = 1.3f)
    val (lyricsGlowEffect, onLyricsGlowEffectChange) = rememberPreference(LyricsGlowEffectKey, defaultValue = true)
    val (appleMusicLyricsBlur, onAppleMusicLyricsBlurChange) = rememberPreference(AppleMusicLyricsBlurKey, defaultValue = true)
    val (lyricsStandardBlur, onLyricsStandardBlurChange) = rememberPreference(LyricsStandardBlurKey, defaultValue = false)
    val (swipeLyrics, onSwipeLyricsChange) = rememberPreference(SwipeLyricsKey, defaultValue = false)
    val (enableLyricsThumbnailPlayPause, onEnableLyricsThumbnailPlayPauseChange) = rememberPreference(EnableLyricsThumbnailPlayPauseKey, defaultValue = false)
    val (oneTapFullscreenLyrics, onOneTapFullscreenLyricsChange) = rememberPreference(OneTapFullscreenLyricsKey, defaultValue = false)
    val (fullscreenLyricsCollapseTop, onFullscreenLyricsCollapseTopChange) = rememberPreference(FullscreenLyricsCollapseTopKey, defaultValue = true)

    val (squigglySlider, onSquigglySliderChange) = rememberPreference(
        SquigglySliderKey,
        defaultValue = false
    )
    val (swipeThumbnail, onSwipeThumbnailChange) = rememberPreference(
        SwipeThumbnailKey,
        defaultValue = true
    )
    val (swipeSensitivity, onSwipeSensitivityChange) = rememberPreference(
        SwipeSensitivityKey,
        defaultValue = 0.73f
    )
    val (canvasThumbnailAnimation, onCanvasThumbnailAnimationChange) = rememberPreference(
        CanvasThumbnailAnimationKey,
        defaultValue = true
    )
    val (canvasSource) = rememberEnumPreference(
        CanvasSourceKey,
        defaultValue = CanvasSource.AUTO
    )
    val (gridItemSize, onGridItemSizeChange) = rememberEnumPreference(
        GridItemsSizeKey,
        defaultValue = GridItemSize.SMALL
    )

    // Density scale preferences
    val context = activity as Context
    val sharedPreferences = remember { context.getSharedPreferences("beatwave_settings", Context.MODE_PRIVATE) }
    val prefDensityScale = remember(sharedPreferences) {
        sharedPreferences.getFloat("density_scale_factor", 1.0f)
    }
    val (densityScale, setDensityScale) = rememberPreference(DensityScaleKey, defaultValue = prefDensityScale)
    var showRestartDialog by rememberSaveable { mutableStateOf(false) }
    var showDensityScaleDialog by rememberSaveable { mutableStateOf(false) }

    val onDensityScaleChange: (Float) -> Unit = { newScale ->
        setDensityScale(newScale)
        sharedPreferences.edit {
            putFloat("density_scale_factor", newScale)
        }
        showRestartDialog = true
    }

    val (listenTogetherInTopBar, onListenTogetherInTopBarChange) = rememberPreference(
        ListenTogetherInTopBarKey,
        defaultValue = true
    )

    val (swipeToSong, onSwipeToSongChange) = rememberPreference(
        SwipeToSongKey,
        defaultValue = false
    )

    val (swipeToRemoveSong, onSwipeToRemoveSongChange) = rememberPreference(
        SwipeToRemoveSongKey,
        defaultValue = false
    )

    val (showLikedPlaylist, onShowLikedPlaylistChange) = rememberPreference(
        ShowLikedPlaylistKey,
        defaultValue = true
    )
    val (showDownloadedPlaylist, onShowDownloadedPlaylistChange) = rememberPreference(
        ShowDownloadedPlaylistKey,
        defaultValue = true
    )
    val (showTopPlaylist, onShowTopPlaylistChange) = rememberPreference(
        ShowTopPlaylistKey,
        defaultValue = true
    )
    val (showCachedPlaylist, onShowCachedPlaylistChange) = rememberPreference(
        ShowCachedPlaylistKey,
        defaultValue = true
    )
    val (showUploadedPlaylist, onShowUploadedPlaylistChange) = rememberPreference(
        ShowUploadedPlaylistKey,
        defaultValue = true
    )
    val (showCommentButton, onShowCommentButtonChange) = rememberPreference(
        ShowCommentButtonKey,
        defaultValue = true
    )

    val (defaultChip, onDefaultChipChange) = rememberEnumPreference(
        key = ChipSortTypeKey,
        defaultValue = LibraryFilter.LIBRARY
    )

    var showLyricsPositionDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showLyricsAnimationStyleDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showLyricsTextSizeDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showLyricsLineSpacingDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showLyricsPositionDialog) {
        EnumDialog(
            onDismiss = { showLyricsPositionDialog = false },
            onSelect = {
                onLyricsPositionChange(it)
                showLyricsPositionDialog = false
            },
            title = stringResource(R.string.lyrics_text_position),
            current = lyricsPosition,
            values = LyricsPosition.entries,
            valueText = {
                when (it) {
                    LyricsPosition.LEFT -> stringResource(R.string.left)
                    LyricsPosition.CENTER -> stringResource(R.string.center)
                    LyricsPosition.RIGHT -> stringResource(R.string.right)
                }
            }
        )
    }

    if (showLyricsAnimationStyleDialog) {
        EnumDialog(
            onDismiss = { showLyricsAnimationStyleDialog = false },
            onSelect = {
                onLyricsAnimationStyleChange(it)
                showLyricsAnimationStyleDialog = false
            },
            title = stringResource(R.string.lyrics_animation_style),
            current = lyricsAnimationStyle,
            values = LyricsAnimationStyle.entries,
            valueText = {
                when (it) {
                    LyricsAnimationStyle.NONE -> stringResource(R.string.none)
                    LyricsAnimationStyle.FADE -> stringResource(R.string.fade)
                    LyricsAnimationStyle.GLOW -> stringResource(R.string.glow)
                    LyricsAnimationStyle.SLIDE -> stringResource(R.string.slide)
                    LyricsAnimationStyle.KARAOKE -> stringResource(R.string.karaoke)
                    LyricsAnimationStyle.APPLE -> stringResource(R.string.apple_music_style)
                    LyricsAnimationStyle.APPLE_V2 -> stringResource(R.string.apple_music_style_letter)
                    LyricsAnimationStyle.beatwave_1 -> stringResource(R.string.beatwave_1)
                    LyricsAnimationStyle.LYRICS_V2 -> stringResource(R.string.lyrics_v2_fluid)
                    LyricsAnimationStyle.METRO_LYRICS -> stringResource(R.string.lyrics_animation_metro)
                }
            }
        )
    }

    var showDefaultOpenTabDialog by rememberSaveable { mutableStateOf(false) }
    if (showDefaultOpenTabDialog) {
        EnumDialog(
            onDismiss = { showDefaultOpenTabDialog = false },
            onSelect = {
                onDefaultOpenTabChange(it)
                showDefaultOpenTabDialog = false
            },
            title = stringResource(R.string.default_open_tab),
            current = defaultOpenTab,
            values = NavigationTab.entries,
            valueText = {
                when (it) {
                    NavigationTab.HOME -> stringResource(R.string.home)
                    NavigationTab.SEARCH -> stringResource(R.string.search)
                    NavigationTab.LIBRARY -> stringResource(R.string.filter_library)
                }
            }
        )
    }

    var showDefaultChipDialog by rememberSaveable { mutableStateOf(false) }
    if (showDefaultChipDialog) {
        EnumDialog(
            onDismiss = { showDefaultChipDialog = false },
            onSelect = {
                onDefaultChipChange(it)
                showDefaultChipDialog = false
            },
            title = stringResource(R.string.default_lib_chips),
            current = defaultChip,
            values = LibraryFilter.entries,
            valueText = {
                when (it) {
                    LibraryFilter.SONGS -> stringResource(R.string.songs)
                    LibraryFilter.ARTISTS -> stringResource(R.string.artists)
                    LibraryFilter.ALBUMS -> stringResource(R.string.albums)
                    LibraryFilter.PLAYLISTS -> stringResource(R.string.playlists)
                    LibraryFilter.LIBRARY -> stringResource(R.string.filter_library)
                }
            }
        )
    }

    var showGridSizeDialog by rememberSaveable { mutableStateOf(false) }
    if (showGridSizeDialog) {
        EnumDialog(
            onDismiss = { showGridSizeDialog = false },
            onSelect = {
                onGridItemSizeChange(it)
                showGridSizeDialog = false
            },
            title = stringResource(R.string.grid_cell_size),
            current = gridItemSize,
            values = GridItemSize.entries,
            valueText = {
                when (it) {
                    GridItemSize.BIG -> stringResource(R.string.big)
                    GridItemSize.SMALL -> stringResource(R.string.small)
                }
            }
        )
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Material3SettingsGroup(
            title = stringResource(R.string.theme_colors),
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.palette),
                    title = { Text(stringResource(R.string.theme_colors)) },
                    onClick = { navController.navigate("settings/appearance/theme") }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.alphabet_cyrillic),
                    title = { Text(stringResource(R.string.app_font)) },
                    description = {
                        val fontLabel = when (AppFont.fromValue(selectedFontValue)) {
                            AppFont.SYSTEM -> stringResource(R.string.font_system)
                            AppFont.GOOGLE_SANS -> stringResource(R.string.font_google_sans)
                            AppFont.SANS_FLEX -> stringResource(R.string.font_sans_flex)
                            AppFont.OUTFIT -> stringResource(R.string.font_outfit)
                            AppFont.PLUS_JAKARTA_SANS -> stringResource(R.string.font_plus_jakarta_sans)
                        }
                        Text(fontLabel)
                    },
                    onClick = { navController.navigate("settings/appearance/font") }
                )
            )
        )

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.liquid_glass),
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.tune),
                    title = { Text(stringResource(R.string.liquid_glass)) },
                    onClick = { navController.navigate("settings/appearance/liquidglass") }
                )
            )
        )

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.appearance),
            items = buildList {
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.speed),
                        title = { Text(stringResource(R.string.enable_high_refresh_rate)) },
                        description = { Text(stringResource(R.string.enable_high_refresh_rate_desc)) },
                        trailingContent = {
                            Switch(
                                checked = enableHighRefreshRate,
                                onCheckedChange = onEnableHighRefreshRateChange,
                                thumbContent = {
                                    Icon(
                                        painter = painterResource(
                                            id = if (enableHighRefreshRate) R.drawable.check else R.drawable.close
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        },
                        onClick = { onEnableHighRefreshRateChange(!enableHighRefreshRate) }
                    )
                )
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.tune),
                        title = { Text(stringResource(R.string.ios_overscroll)) },
                        description = { Text(stringResource(R.string.ios_overscroll_desc)) },
                        trailingContent = {
                            Switch(
                                checked = iosOverscroll,
                                onCheckedChange = onIosOverscrollChange,
                                thumbContent = {
                                    Icon(
                                        painter = painterResource(
                                            id = if (iosOverscroll) R.drawable.check else R.drawable.close
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        },
                        onClick = { onIosOverscrollChange(!iosOverscroll) }
                    )
                )
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.settings),
                        title = { Text(stringResource(R.string.enable_settings_popup)) },
                        description = { Text(stringResource(R.string.enable_settings_popup_desc)) },
                        trailingContent = {
                            Switch(
                                checked = enableSettingsPopup,
                                onCheckedChange = onEnableSettingsPopupChange,
                                thumbContent = {
                                    Icon(
                                        painter = painterResource(
                                            id = if (enableSettingsPopup) R.drawable.check else R.drawable.close
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        },
                        onClick = { onEnableSettingsPopupChange(!enableSettingsPopup) }
                    )
                )
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.grid_view),
                        title = { Text(stringResource(R.string.library_icons_only)) },
                        description = { Text(stringResource(R.string.library_icons_only_desc)) },
                        trailingContent = {
                            Switch(
                                checked = libraryIconsOnly,
                                onCheckedChange = onLibraryIconsOnlyChange,
                                thumbContent = {
                                    Icon(
                                        painter = painterResource(
                                            id = if (libraryIconsOnly) R.drawable.check else R.drawable.close
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        },
                        onClick = { onLibraryIconsOnlyChange(!libraryIconsOnly) }
                    )
                )
            }
        )

        Spacer(modifier = Modifier.height(27.dp))

        val (thumbnailCornerRadius, onThumbnailCornerRadiusChange) = rememberPreference(
            ThumbnailCornerRadiusKey,
            defaultValue = 3f
        )
        
        var showThumbnailCornerRadiusDialog by rememberSaveable { mutableStateOf(false) }

        Material3SettingsGroup(
            title = stringResource(R.string.player),
            items = listOfNotNull(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.gradient),
                    title = { Text(stringResource(R.string.player_theme)) },
                    description = { Text(stringResource(R.string.player_theme_desc)) },
                    onClick = { navController.navigate("settings/appearance/playertheme") }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.hide_image),
                    title = { Text(stringResource(R.string.hide_player_thumbnail)) },
                    description = { Text(stringResource(R.string.hide_player_thumbnail_desc)) },
                    trailingContent = {
                        Switch(
                            checked = hidePlayerThumbnail,
                            onCheckedChange = onHidePlayerThumbnailChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (hidePlayerThumbnail) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onHidePlayerThumbnailChange(!hidePlayerThumbnail) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.image),
                    title = { Text(stringResource(R.string.thumbnail_corner_radius)) },
                    description = { Text(stringResource(R.string.thumbnail_corner_radius_desc)) },
                    trailingContent = {
                        Text(
                            text = "${thumbnailCornerRadius.roundToInt()}dp",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = { showThumbnailCornerRadiusDialog = true }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.crop),
                    title = { Text(stringResource(R.string.crop_album_art)) },
                    description = { Text(stringResource(R.string.crop_album_art_desc)) },
                    trailingContent = {
                        Switch(
                            checked = cropAlbumArt,
                            onCheckedChange = onCropAlbumArtChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (cropAlbumArt) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onCropAlbumArtChange(!cropAlbumArt) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.tune),
                    title = { Text(stringResource(R.string.show_audio_quality_badge)) },
                    trailingContent = {
                        Switch(
                            checked = showAudioQualityBadge,
                            onCheckedChange = onShowAudioQualityBadgeChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (showAudioQualityBadge) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onShowAudioQualityBadgeChange(!showAudioQualityBadge) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.tune),
                    title = { Text(stringResource(R.string.mini_player_waveform)) },
                    trailingContent = {
                        Switch(
                            checked = miniPlayerWaveform,
                            onCheckedChange = onMiniPlayerWaveformChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (miniPlayerWaveform) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onMiniPlayerWaveformChange(!miniPlayerWaveform) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.nav_bar),
                    title = { Text(stringResource(R.string.mini_bar_tab_style)) },
                    description = { Text(stringResource(R.string.mini_bar_tab_style_desc)) },
                    trailingContent = {
                        Switch(
                            checked = miniBarTabStyle,
                            onCheckedChange = onMiniBarTabStyleChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (miniBarTabStyle) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onMiniBarTabStyleChange(!miniBarTabStyle) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.tune),
                    title = { Text(stringResource(R.string.enable_wavy_slider)) },
                    trailingContent = {
                        Switch(
                            checked = squigglySlider,
                            onCheckedChange = onSquigglySliderChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (squigglySlider) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onSquigglySliderChange(!squigglySlider) }
                ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.canvas_art),
                        title = { Text(stringResource(R.string.beatwave_canvas)) },
                    description = {
                        val summary = if (!canvasThumbnailAnimation) {
                            stringResource(R.string.disable)
                        } else {
                            when (canvasSource) {
                                CanvasSource.AUTO -> stringResource(R.string.canvas_source_auto)
                                CanvasSource.ECHO_MUSIC -> stringResource(R.string.canvas_source_echo_music)
                                CanvasSource.APPLE_MUSIC -> stringResource(R.string.canvas_source_apple_music)
                                CanvasSource.beatwave -> stringResource(R.string.canvas_source_beatwave)
                                CanvasSource.TIDAL -> stringResource(R.string.canvas_source_tidal)
                            }
                        }
                        Text(summary)
                    },
                    onClick = { navController.navigate("settings/appearance/canvas") }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.chat_msg),
                    title = { Text(stringResource(R.string.show_comment_button)) },
                    description = { Text(stringResource(R.string.show_comment_button_description)) },
                    trailingContent = {
                        Switch(
                            checked = showCommentButton,
                            onCheckedChange = onShowCommentButtonChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (showCommentButton) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onShowCommentButtonChange(!showCommentButton) }
                )
            )
        )

        if (showThumbnailCornerRadiusDialog) {
            ThumbnailCornerRadiusModal(
                initialRadius = thumbnailCornerRadius,
                onDismiss = { showThumbnailCornerRadiusDialog = false },
                onRadiusSelected = { radius ->
                    onThumbnailCornerRadiusChange(radius)
                    showThumbnailCornerRadiusDialog = false
                }
            )
        }

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.lyrics),
            items = listOfNotNull(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.lyrics),
                    title = { Text(stringResource(R.string.lyrics_text_position)) },
                    description = {
                        Text(
                            when (lyricsPosition) {
                                LyricsPosition.LEFT -> stringResource(R.string.left)
                                LyricsPosition.CENTER -> stringResource(R.string.center)
                                LyricsPosition.RIGHT -> stringResource(R.string.right)
                            }
                        )
                    },
                    onClick = { showLyricsPositionDialog = true }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.lyrics),
                    title = { Text(stringResource(R.string.lyrics_animation_style)) },
                    description = {
                        Text(
                            when (lyricsAnimationStyle) {
                                LyricsAnimationStyle.NONE -> stringResource(R.string.none)
                                LyricsAnimationStyle.FADE -> stringResource(R.string.fade)
                                LyricsAnimationStyle.GLOW -> stringResource(R.string.glow)
                                LyricsAnimationStyle.SLIDE -> stringResource(R.string.slide)
                                LyricsAnimationStyle.KARAOKE -> stringResource(R.string.karaoke)
                                LyricsAnimationStyle.beatwave_1 -> stringResource(R.string.beatwave_1)
                                LyricsAnimationStyle.APPLE -> stringResource(R.string.apple_music_style)
                                LyricsAnimationStyle.APPLE_V2 -> stringResource(R.string.apple_music_style_letter)
                                LyricsAnimationStyle.LYRICS_V2 -> stringResource(R.string.lyrics_v2_fluid)
                                LyricsAnimationStyle.METRO_LYRICS -> stringResource(R.string.lyrics_animation_metro)
                            }
                        )
                    },
                    onClick = { showLyricsAnimationStyleDialog = true }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.lyrics),
                    title = { Text(stringResource(R.string.lyrics_glow_effect)) },
                    description = { Text(stringResource(R.string.lyrics_glow_effect_desc)) },
                    trailingContent = {
                        Switch(
                            checked = lyricsGlowEffect,
                            onCheckedChange = onLyricsGlowEffectChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (lyricsGlowEffect) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onLyricsGlowEffectChange(!lyricsGlowEffect) }
                ),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && lyricsAnimationStyle == LyricsAnimationStyle.beatwave_1) {
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.lyrics),
                        title = { Text(stringResource(R.string.apple_music_lyrics_blur)) },
                        description = { Text(stringResource(R.string.apple_music_lyrics_blur_desc)) },
                        trailingContent = {
                            Switch(
                                checked = appleMusicLyricsBlur,
                                onCheckedChange = onAppleMusicLyricsBlurChange,
                                thumbContent = {
                                    Icon(
                                        painter = painterResource(
                                            id = if (appleMusicLyricsBlur) R.drawable.check else R.drawable.close
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        },
                        onClick = { onAppleMusicLyricsBlurChange(!appleMusicLyricsBlur) }
                    )
                } else null,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.lyrics),
                        title = { Text(stringResource(R.string.standard_lyrics_blur)) },
                        description = { Text(stringResource(R.string.apple_music_lyrics_blur_desc)) },
                        trailingContent = {
                            Switch(
                                checked = lyricsStandardBlur,
                                onCheckedChange = onLyricsStandardBlurChange,
                                thumbContent = {
                                    Icon(
                                        painter = painterResource(
                                            id = if (lyricsStandardBlur) R.drawable.check else R.drawable.close
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        },
                        onClick = { onLyricsStandardBlurChange(!lyricsStandardBlur) }
                    )
                } else null,
                Material3SettingsItem(
                    icon = painterResource(R.drawable.lyrics),
                    title = { Text(stringResource(R.string.lyrics_auto_scroll)) },
                    trailingContent = {
                        Switch(
                            checked = lyricsScroll,
                            onCheckedChange = onLyricsScrollChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (lyricsScroll) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onLyricsScrollChange(!lyricsScroll) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.swipe),
                    title = { Text(stringResource(R.string.lyrics_swipe_to_change_song)) },
                    description = { Text(stringResource(R.string.lyrics_swipe_to_change_song_desc)) },
                    trailingContent = {
                        Switch(
                            checked = swipeLyrics,
                            onCheckedChange = onSwipeLyricsChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (swipeLyrics) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onSwipeLyricsChange(!swipeLyrics) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.play),
                    title = { Text(stringResource(R.string.lyrics_thumbnail_play_pause)) },
                    description = { Text(stringResource(R.string.lyrics_thumbnail_play_pause_desc)) },
                    trailingContent = {
                        Switch(
                            checked = enableLyricsThumbnailPlayPause,
                            onCheckedChange = onEnableLyricsThumbnailPlayPauseChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (enableLyricsThumbnailPlayPause) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onEnableLyricsThumbnailPlayPauseChange(!enableLyricsThumbnailPlayPause) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.fullscreen),
                    title = { Text(stringResource(R.string.one_tap_fullscreen_lyrics)) },
                    description = { Text(stringResource(R.string.one_tap_fullscreen_lyrics_desc)) },
                    trailingContent = {
                        Switch(
                            checked = oneTapFullscreenLyrics,
                            onCheckedChange = onOneTapFullscreenLyricsChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (oneTapFullscreenLyrics) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onOneTapFullscreenLyricsChange(!oneTapFullscreenLyrics) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.expand_more),
                    title = { Text(stringResource(R.string.fullscreen_lyrics_collapse_top)) },
                    description = { Text(stringResource(R.string.fullscreen_lyrics_collapse_top_desc)) },
                    trailingContent = {
                        Switch(
                            checked = fullscreenLyricsCollapseTop,
                            onCheckedChange = onFullscreenLyricsCollapseTopChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (fullscreenLyricsCollapseTop) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onFullscreenLyricsCollapseTopChange(!fullscreenLyricsCollapseTop) }
                )
            )
        )

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.misc),
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.nav_bar),
                    title = { Text(stringResource(R.string.default_open_tab)) },
                    description = {
                        Text(
                            when (defaultOpenTab) {
                                NavigationTab.HOME -> stringResource(R.string.home)
                                NavigationTab.SEARCH -> stringResource(R.string.search)
                                NavigationTab.LIBRARY -> stringResource(R.string.filter_library)
                            }
                        )
                    },
                    onClick = { showDefaultOpenTabDialog = true }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.tab),
                    title = { Text(stringResource(R.string.default_lib_chips)) },
                    description = {
                        Text(
                            when (defaultChip) {
                                LibraryFilter.SONGS -> stringResource(R.string.songs)
                                LibraryFilter.ARTISTS -> stringResource(R.string.artists)
                                LibraryFilter.ALBUMS -> stringResource(R.string.albums)
                                LibraryFilter.PLAYLISTS -> stringResource(R.string.playlists)
                                LibraryFilter.LIBRARY -> stringResource(R.string.filter_library)
                            }
                        )
                    },
                    onClick = { showDefaultChipDialog = true }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.swipe),
                    title = { Text(stringResource(R.string.swipe_song_to_add)) },
                    trailingContent = {
                        Switch(
                            checked = swipeToSong,
                            onCheckedChange = onSwipeToSongChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (swipeToSong) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onSwipeToSongChange(!swipeToSong) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.swipe),
                    title = { Text(stringResource(R.string.swipe_song_to_remove)) },
                    trailingContent = {
                        Switch(
                            checked = swipeToRemoveSong,
                            onCheckedChange = onSwipeToRemoveSongChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (swipeToRemoveSong) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onSwipeToRemoveSongChange(!swipeToRemoveSong) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.grid_view),
                    title = { Text(stringResource(R.string.grid_cell_size)) },
                    description = {
                        Text(
                            when (gridItemSize) {
                                GridItemSize.BIG -> stringResource(R.string.big)
                                GridItemSize.SMALL -> stringResource(R.string.small)
                            }
                        )
                    },
                    onClick = { showGridSizeDialog = true }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.grid_view),
                    title = { Text(stringResource(R.string.display_density)) },
                    description = {
                        Text(DensityScale.fromValue(densityScale).label)
                    },
                    onClick = { showDensityScaleDialog = true }
                )
            )
        )

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.auto_playlists),
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.favorite),
                    title = { Text(stringResource(R.string.show_liked_playlist)) },
                    trailingContent = {
                        Switch(
                            checked = showLikedPlaylist,
                            onCheckedChange = onShowLikedPlaylistChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (showLikedPlaylist) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onShowLikedPlaylistChange(!showLikedPlaylist) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.offline),
                    title = { Text(stringResource(R.string.show_downloaded_playlist)) },
                    trailingContent = {
                        Switch(
                            checked = showDownloadedPlaylist,
                            onCheckedChange = onShowDownloadedPlaylistChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (showDownloadedPlaylist) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onShowDownloadedPlaylistChange(!showDownloadedPlaylist) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.trending_up),
                    title = { Text(stringResource(R.string.show_top_playlist)) },
                    trailingContent = {
                        Switch(
                            checked = showTopPlaylist,
                            onCheckedChange = onShowTopPlaylistChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (showTopPlaylist) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onShowTopPlaylistChange(!showTopPlaylist) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.cached),
                    title = { Text(stringResource(R.string.show_cached_playlist)) },
                    trailingContent = {
                        Switch(
                            checked = showCachedPlaylist,
                            onCheckedChange = onShowCachedPlaylistChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (showCachedPlaylist) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onShowCachedPlaylistChange(!showCachedPlaylist) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.backup),
                    title = { Text(stringResource(R.string.show_uploaded_playlist)) },
                    trailingContent = {
                        Switch(
                            checked = showUploadedPlaylist,
                            onCheckedChange = onShowUploadedPlaylistChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (showUploadedPlaylist) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onShowUploadedPlaylistChange(!showUploadedPlaylist) }
                )
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
    }

    TopAppBar(
            windowInsets = appTopBarWindowInsets(),
        title = { Text(stringResource(R.string.appearance)) },
        navigationIcon = {
            IconButton(
                onClick = { navController.navigateUp() },
                onLongClick = { navController.backToMain() },
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        }
    )
}

enum class DarkMode {
    ON,
    OFF,
    AUTO,
}

enum class NavigationTab {
    HOME,
    SEARCH,
    LIBRARY,
}

enum class LyricsPosition {
    LEFT,
    CENTER,
    RIGHT,
}

enum class PlayerTextAlignment {
    SIDED,
    CENTER,
}
