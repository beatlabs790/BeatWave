/**
 * BeatWave Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.beatwave.music.ui.screens

import android.app.Activity
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import com.beatwave.music.constants.DarkModeKey
import com.beatwave.music.constants.PureBlackKey
import com.beatwave.music.ui.screens.artist.ArtistAlbumsScreen
import com.beatwave.music.ui.screens.artist.ArtistItemsScreen
import com.beatwave.music.ui.screens.artist.ArtistScreen
import com.beatwave.music.ui.screens.artist.ArtistSongsScreen
import com.beatwave.music.ui.screens.equalizer.EqScreen
import com.beatwave.music.ui.screens.library.LibraryScreen
import com.beatwave.music.ui.screens.library.LocalFolderScreen
import com.beatwave.music.ui.screens.library.LocalMusicScreen
import com.beatwave.music.ui.screens.playlist.AutoPlaylistScreen
import com.beatwave.music.ui.screens.playlist.CachePlaylistScreen
import com.beatwave.music.ui.screens.playlist.LocalPlaylistScreen
import com.beatwave.music.ui.screens.playlist.OnlinePlaylistScreen
import com.beatwave.music.ui.screens.playlist.TopPlaylistScreen
import com.beatwave.music.ui.screens.search.OnlineSearchResult
import com.beatwave.music.ui.screens.search.SearchScreen
import com.beatwave.music.ui.screens.settings.AboutScreen
import com.beatwave.music.ui.screens.settings.AppearanceSettings
import com.beatwave.music.ui.screens.settings.CanvasSelection
import com.beatwave.music.ui.screens.settings.AppIconScreen
import com.beatwave.music.ui.screens.settings.diy.DiyEditorScreen
import com.beatwave.music.ui.screens.settings.diy.PlayerIconsScreen
import com.beatwave.music.ui.screens.settings.diy.PresetsScreen
import com.beatwave.music.ui.screens.settings.FontSelectionScreen
import com.beatwave.music.ui.screens.settings.GlassEffectSettings
import com.beatwave.music.ui.screens.settings.PlayerThemeScreen
import com.beatwave.music.ui.screens.settings.BackupAndRestore
import com.beatwave.music.ui.screens.settings.SpotifyScreen
import com.beatwave.music.viewmodels.SpotifyImportViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.beatwave.music.ui.screens.settings.ContentSettings
import com.beatwave.music.ui.screens.settings.DarkMode
import com.beatwave.music.ui.screens.settings.DiscordLoginScreen
import com.beatwave.music.ui.screens.settings.PlayerSettings
import com.beatwave.music.ui.screens.settings.JioSettings
import com.beatwave.music.ui.screens.settings.LocalFoldersSettingsScreen
import com.beatwave.music.ui.screens.settings.PrivacySettings
import com.beatwave.music.ui.screens.settings.RomanizationSettings
import com.beatwave.music.ui.screens.settings.SettingsScreen
import com.beatwave.music.ui.screens.settings.AccountSettingsScreen
import com.beatwave.music.ui.screens.settings.StorageSettings
import com.beatwave.music.ui.screens.settings.ThemeScreen
import com.beatwave.music.ui.screens.settings.AiSettings
import com.beatwave.music.ui.screens.settings.integrations.DiscordSettings
import com.beatwave.music.ui.screens.settings.integrations.IntegrationScreen
import com.beatwave.music.ui.screens.settings.integrations.LastFMSettings
import com.beatwave.music.ui.screens.settings.integrations.ListenTogetherSettings
import com.beatwave.music.ui.screens.ambient.AmbientModeScreen
import com.beatwave.music.ui.screens.recognition.RecognitionScreen
import com.beatwave.music.ui.screens.recognition.RecognitionHistoryScreen
import com.beatwave.music.ui.screens.settings.ModuleSourceScreen
import com.beatwave.music.ui.screens.settings.ModuleDetailScreen
import com.beatwave.music.ui.screens.settings.UpdateSettings
import com.beatwave.music.ui.screens.wrapped.WrappedScreen
import com.beatwave.music.vivimusic.updater.UpdateScreen
import com.beatwave.music.utils.rememberEnumPreference
import com.beatwave.music.utils.rememberPreference
import com.beatwave.music.vivimusic.changelog.ChangelogScreen
import com.beatwave.music.vivimusic.commitscreen.CommitScreen
import com.beatwave.music.ui.screens.equalizer.axion.AxionEqScreen

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.navigationBuilder(
    navController: NavHostController,
    scrollBehavior: TopAppBarScrollBehavior,
    activity: Activity,
    snackbarHostState: SnackbarHostState
) {
    composable(Screens.Home.route) {
        HomeScreen(navController = navController, snackbarHostState = snackbarHostState)
    }

    composable(Screens.Search.route) {
        val pureBlackEnabled by rememberPreference(PureBlackKey, defaultValue = false)
        val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
        val isSystemInDarkTheme = isSystemInDarkTheme()
        val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
            if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
        }
        val pureBlack = remember(pureBlackEnabled, useDarkTheme) {
            pureBlackEnabled && useDarkTheme
        }
        SearchScreen(
            navController = navController,
            pureBlack = pureBlack
        )
    }

    composable(Screens.Library.route) {
        LibraryScreen(navController)
    }

    composable(Screens.ListenTogether.route) {
        ListenTogetherScreen(navController, showTopBar = false)
    }

    composable(
        route = "listen_together_from_topbar",
    ) {
        ListenTogetherScreen(navController, showTopBar = true)
    }

    composable("listen_together/chat") {
        CommentTogetherScreen(navController)
    }

    composable("history") {
        HistoryScreen(navController)
    }

    composable("stats") {
        StatsScreen(navController)
    }

    composable("mood_and_genres") {
        MoodAndGenresScreen(navController, scrollBehavior)
    }

    composable("account") {
        AccountScreen(navController, scrollBehavior)
    }

    composable("new_release") {
        NewReleaseScreen(navController, scrollBehavior)
    }

    composable("charts_screen") {
        ChartsScreen(navController)
    }

    composable(
        route = "browse/{browseId}",
        arguments = listOf(
            navArgument("browseId") {
                type = NavType.StringType
            }
        )
    ) {
        BrowseScreen(
            navController,
            scrollBehavior,
            it.arguments?.getString("browseId")
        )
    }

    composable(
        route = "search/{query}",
        arguments = listOf(
            navArgument("query") {
                type = NavType.StringType
            },
        ),
        enterTransition = {
            fadeIn(tween(250))
        },
        exitTransition = {
            if (targetState.destination.route?.startsWith("search/") == true) {
                fadeOut(tween(200))
            } else {
                fadeOut(tween(200)) + slideOutHorizontally { -it / 2 }
            }
        },
        popEnterTransition = {
            if (initialState.destination.route?.startsWith("search/") == true) {
                fadeIn(tween(250))
            } else {
                fadeIn(tween(250)) + slideInHorizontally { -it / 2 }
            }
        },
        popExitTransition = {
            fadeOut(tween(200))
        },
    ) {
        OnlineSearchResult(navController)
    }

    composable(
        route = "album/{albumId}",
        arguments = listOf(
            navArgument("albumId") {
                type = NavType.StringType
            },
        ),
    ) {
        AlbumScreen(navController, scrollBehavior)
    }

    composable(
        route = "artist/{artistId}",
        arguments = listOf(
            navArgument("artistId") {
                type = NavType.StringType
            },
        ),
    ) {
        ArtistScreen(navController, scrollBehavior)
    }

    composable(
        route = "artist/{artistId}/songs",
        arguments = listOf(
            navArgument("artistId") {
                type = NavType.StringType
            },
        ),
    ) {
        ArtistSongsScreen(navController, scrollBehavior)
    }

    composable(
        route = "artist/{artistId}/albums",
        arguments = listOf(
            navArgument("artistId") {
                type = NavType.StringType
            }
        )
    ) {
        ArtistAlbumsScreen(navController, scrollBehavior)
    }

    composable(
        route = "artist/{artistId}/items?browseId={browseId}?params={params}",
        arguments = listOf(
            navArgument("artistId") {
                type = NavType.StringType
            },
            navArgument("browseId") {
                type = NavType.StringType
                nullable = true
            },
            navArgument("params") {
                type = NavType.StringType
                nullable = true
            },
        ),
    ) {
        ArtistItemsScreen(navController, scrollBehavior)
    }

    composable(
        route = "online_playlist/{playlistId}",
        arguments = listOf(
            navArgument("playlistId") {
                type = NavType.StringType
            },
        ),
    ) {
        OnlinePlaylistScreen(navController, scrollBehavior)
    }

    composable(
        route = "local_playlist/{playlistId}",
        arguments = listOf(
            navArgument("playlistId") {
                type = NavType.StringType
            },
        ),
    ) {
        LocalPlaylistScreen(navController, scrollBehavior)
    }

    composable(
        route = "local_music",
    ) {
        LocalMusicScreen(navController, scrollBehavior)
    }

    composable(
        route = "local_folder/{path}",
        arguments = listOf(
            navArgument("path") {
                type = NavType.StringType
            },
        ),
    ) {
        LocalFolderScreen(navController, it.arguments?.getString("path").orEmpty())
    }

    composable(
        route = "auto_playlist/{playlist}",
        arguments = listOf(
            navArgument("playlist") {
                type = NavType.StringType
            },
        ),
    ) {
        AutoPlaylistScreen(navController, scrollBehavior)
    }

    composable(
        route = "cache_playlist/{playlist}",
        arguments = listOf(
            navArgument("playlist") {
                type = NavType.StringType
            },
        ),
    ) {
        CachePlaylistScreen(navController, scrollBehavior)
    }

    composable(
        route = "top_playlist/{top}",
        arguments = listOf(
            navArgument("top") {
                type = NavType.StringType
            },
        ),
    ) {
        TopPlaylistScreen(navController, scrollBehavior)
    }

    composable(
        route = "youtube_browse/{browseId}?params={params}",
        arguments = listOf(
            navArgument("browseId") {
                type = NavType.StringType
                nullable = true
            },
            navArgument("params") {
                type = NavType.StringType
                nullable = true
            },
        ),
    ) {
        YouTubeBrowseScreen(navController)
    }

    composable("settings") {
        SettingsScreen(navController, scrollBehavior)
    }

    composable("settings/update") {
       UpdateSettings(navController, scrollBehavior)
    }

    composable("settings/account") {
        AccountSettingsScreen(navController, scrollBehavior)
    }

    composable("settings/appearance") {
        AppearanceSettings(navController, scrollBehavior, activity, snackbarHostState)
    }

    composable("settings/appearance/theme") {
        ThemeScreen(navController)
    }

    composable("settings/appearance/font") {
        FontSelectionScreen(navController, scrollBehavior)
    }

    composable("settings/appearance/appicon") {
        AppIconScreen(navController, scrollBehavior)
    }

    composable("settings/appearance/presets") {
        PresetsScreen(navController, scrollBehavior)
    }

    composable("settings/appearance/playericons") {
        PlayerIconsScreen(navController, scrollBehavior)
    }

    composable("settings/appearance/diy") {
        DiyEditorScreen(navController)
    }

    composable("settings/appearance/canvas") {
        CanvasSelection(navController, scrollBehavior)
    }

    composable("settings/appearance/playertheme") {
        PlayerThemeScreen(navController, scrollBehavior)
    }

    composable("settings/appearance/liquidglass") {
        GlassEffectSettings(navController, scrollBehavior)
    }

    composable("settings/content") {
        ContentSettings(navController, scrollBehavior)
    }

    composable("settings/content/romanization") {
        RomanizationSettings(navController, scrollBehavior)
    }

    composable("settings/ai") {
        AiSettings(navController, scrollBehavior)
    }
    
    composable("settings/player") {
        PlayerSettings(navController, scrollBehavior)
    }

    composable("settings/player/jio") {
        JioSettings(navController, scrollBehavior)
    }

    composable("settings/player/local_folders") {
        LocalFoldersSettingsScreen(navController, scrollBehavior)
    }

    composable("settings/storage") {
        StorageSettings(navController, scrollBehavior)
    }

    composable("settings/equalizer") {
        AxionEqScreen(onBackClick = { navController.navigateUp() })
    }

    composable("settings/privacy") {
        PrivacySettings(navController, scrollBehavior)
    }

    composable("settings/backup_restore") {
        BackupAndRestore(navController, scrollBehavior)
    }

    composable("settings/spotify") {
        SpotifyScreen(navController, scrollBehavior)
    }



    composable("settings/integrations") {
        IntegrationScreen(navController, scrollBehavior)
    }

    composable("settings/modules") {
        ModuleSourceScreen(navController, scrollBehavior)
    }

    composable(
        route = "settings/modules/{moduleId}",
        arguments = listOf(
            navArgument("moduleId") {
                type = NavType.StringType
            },
        ),
    ) {
        ModuleDetailScreen(navController, scrollBehavior, it.arguments?.getString("moduleId") ?: "")
    }

    composable("settings/integrations/discord") {
        DiscordSettings(navController, scrollBehavior, snackbarHostState)
    }

    composable("settings/integrations/lastfm") {
        LastFMSettings(navController, scrollBehavior)
    }

    composable(route = "settings/integrations/listen_together") {
        ListenTogetherSettings(navController, scrollBehavior)
    }

    composable("settings/discord/login") {
        DiscordLoginScreen(navController)
    }

    composable("settings/about") {
        AboutScreen(navController, scrollBehavior)
    }

    composable("update") {
        UpdateScreen(navController)
    }

    composable("login") {
        LoginScreen(navController)
    }

    composable("channel_picker") {
        ChannelPickerScreen(navController)
    }

    composable("switch_channel") {
        SwitchChannelScreen(navController)
    }

    composable("wrapped") {
        WrappedScreen(navController)
    }

    composable("ambient_mode") {
        AmbientModeScreen(navController)
    }

    dialog("equalizer") {
        EqScreen(navController = navController)
    }

    composable("recognition") {
        RecognitionScreen(navController)
    }

    composable("recognition_history") {
        RecognitionHistoryScreen(navController)
    }
    composable("settings/changelog") {
        ChangelogScreen(navController,scrollBehavior)
    }
    composable("settings/commits") {
        CommitScreen(navController, scrollBehavior)
    }
}

