/**
 * BeatWave Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convx.music

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import com.convx.music.ui.component.backdrop.backdrops.layerBackdrop
import com.convx.music.ui.component.backdrop.backdrops.rememberLayerBackdrop
import com.convx.music.ui.component.backdrop.drawPlainBackdrop
import com.convx.music.ui.component.backdrop.effects.blur
import com.convx.music.ui.component.backdrop.isRenderEffectSupported
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import com.convx.music.constants.AppBarHeight
import com.convx.music.constants.AppLanguageKey
import com.convx.music.constants.DarkModeKey
import com.convx.music.constants.DefaultOpenTabKey
import com.convx.music.constants.DisableScreenshotKey
import com.convx.music.constants.DynamicThemeKey
import com.convx.music.constants.SearchSource
import com.convx.music.constants.SearchSourceKey
import com.convx.music.constants.EnableHighRefreshRateKey
import com.convx.music.constants.EnableSettingsPopupKey
import com.convx.music.constants.ListenTogetherInTopBarKey
import com.convx.music.constants.ListenTogetherUsernameKey
import com.convx.music.constants.MiniPlayerBottomSpacing
import com.convx.music.constants.MiniPlayerHeight
import com.convx.music.constants.DockedAccessoryHeight
import com.convx.music.constants.NavigationBarAnimationSpec
import com.convx.music.constants.NavigationBarHeight
import com.convx.music.constants.PauseListenHistoryKey
import com.convx.music.constants.LiquidGlassGlobalEnabledKey
import com.convx.music.constants.LiquidGlassPlayerEnabledKey
import com.convx.music.constants.LiquidGlassMiniPlayerEnabledKey
import com.convx.music.constants.LiquidGlassNavBarEnabledKey
import com.convx.music.constants.LiquidGlassSidePanelEnabledKey
import com.convx.music.constants.LiquidGlassSidePanelVibrancyKey
import com.convx.music.constants.LiquidGlassSidePanelBlurRadiusKey
import com.convx.music.constants.LiquidGlassSidePanelLensHeightKey
import com.convx.music.constants.LiquidGlassSidePanelLensAmountKey
import com.convx.music.constants.LiquidGlassSidePanelColorKey
import com.convx.music.constants.LiquidGlassSidePanelSurfaceOpacityKey
import com.convx.music.constants.LiquidGlassSidePanelTextColorKey
import com.convx.music.constants.SideBarCollapsedKey
import com.convx.music.constants.LiquidGlassVibrancyKey
import com.convx.music.constants.LiquidGlassBlurRadiusKey
import com.convx.music.constants.LiquidGlassLensHeightKey
import com.convx.music.constants.LiquidGlassLensAmountKey
import com.convx.music.constants.LiquidGlassChromaticAberrationKey
import com.convx.music.constants.LiquidGlassDepthEffectKey
import com.convx.music.constants.LiquidGlassSurfaceTintColorKey
import com.convx.music.constants.LiquidGlassSurfaceOpacityKey
import com.convx.music.constants.LiquidGlassTextColorKey
import com.convx.music.constants.AppleMusicUiKey
import com.convx.music.constants.PauseSearchHistoryKey
import com.convx.music.constants.PureBlackKey
import com.convx.music.constants.SYSTEM_DEFAULT
import com.convx.music.constants.SelectedThemeColorKey
import com.convx.music.constants.SlimNavBarHeight
import com.convx.music.constants.ForceTabletLayoutKey
import com.convx.music.constants.PlaylistSortType
import com.convx.music.constants.SlimNavBarKey
import com.convx.music.constants.StopMusicOnTaskClearKey
import com.convx.music.constants.UseNewMiniPlayerDesignKey
import com.convx.music.db.MusicDatabase
import com.convx.music.db.entities.SearchHistory
import com.convx.music.extensions.toEnum
import com.convx.music.models.toMediaMetadata
import com.convx.music.playback.DownloadUtil
import com.convx.music.playback.MusicService
import com.convx.music.playback.MusicService.MusicBinder
import com.convx.music.playback.PlayerConnection
import com.convx.music.playback.queues.YouTubeQueue
import com.convx.music.ui.component.AppFloatingNavBar
import com.convx.music.ui.component.LocalNavSearchState
import com.convx.music.ui.component.NavSearchState
import com.convx.music.ui.component.AppNavigationBar
import com.convx.music.ui.component.GlassEffectConfig
import com.convx.music.ui.component.LocalGlassEffectConfig
import com.convx.music.ui.component.LocalAppBackdrop
import com.convx.music.ui.component.LocalAppleMusicUi
import com.convx.music.ui.component.isGlassAllowed
import com.convx.music.ui.component.AppFloatingNowPlayingPill
import com.convx.music.ui.component.FloatingMiniPlayerWidthFraction
import com.convx.music.ui.component.NavBarSearchInputBar
import com.convx.music.ui.component.GlassCircleButton
import com.convx.music.ui.component.AppFloatingSideBar
import com.convx.music.ui.component.SideBarAccountRow
import com.convx.music.ui.component.SideBarContentInset
import com.convx.music.ui.component.SideBarMargin
import com.convx.music.ui.component.SideBarCollapsedWidth
import com.convx.music.ui.component.SideBarLink
import com.convx.music.ui.component.SideBarSection
import com.convx.music.ui.component.TabletWidthThreshold
import com.convx.music.ui.component.AppNavigationRail
import com.convx.music.ui.component.BottomSheetMenu
import com.convx.music.ui.component.BottomSheetPage
import com.convx.music.ui.component.LocalBottomSheetPageState
import com.convx.music.ui.component.LocalMenuState
import com.convx.music.ui.component.rememberBottomSheetState
import com.convx.music.ui.component.shimmer.ShimmerTheme
import com.convx.music.ui.menu.YouTubeSongMenu
import com.convx.music.ui.player.BottomSheetPlayer
import com.convx.music.ui.screens.Screens
import com.convx.music.ui.screens.SettingDialoge
import com.convx.music.ui.screens.navigationBuilder
import com.convx.music.ui.screens.settings.DarkMode
import com.convx.music.ui.screens.settings.NavigationTab
import com.convx.music.ui.theme.ColorSaver
import com.convx.music.ui.theme.DefaultThemeColor
import com.convx.music.ui.theme.BrandName
import com.convx.music.ui.theme.LocalAccentTextColor
import com.convx.music.ui.theme.rememberBrandFontFamily
import com.convx.music.ui.theme.extractThemeColor
import com.convx.music.ui.theme.extractThemeColorFromVideoFrame
import com.convx.music.ui.theme.beatwaveTheme
import com.convx.music.ui.utils.appBarScrollBehavior
import com.convx.music.ui.utils.resetHeightOffset
import com.convx.music.utils.SyncUtils
import com.convx.music.utils.dataStore
import com.convx.music.utils.get
import com.convx.music.utils.rememberEnumPreference
import com.convx.music.constants.CanvasSource
import com.convx.music.constants.CanvasSourceKey
import com.convx.music.ui.player.CanvasArtworkPlaybackCache
import com.convx.music.utils.rememberPreference
import com.convx.music.constants.IosOverscrollKey
import com.convx.music.ui.utils.rememberIosOverscrollFactory
import com.convx.music.utils.reportException
import com.convx.music.utils.setAppLocale
import com.convx.music.viewmodels.HistoryViewModel
import com.convx.music.ui.component.floatingtabbar.rememberFloatingTabBarScrollConnection
import com.convx.music.viewmodels.HomeViewModel
import com.convx.music.vivimusic.UpdateNotificationHelper
import com.convx.music.vivimusic.updater.checkForUpdate
import com.convx.music.vivimusic.updater.getAutoUpdateCheckSetting
import com.convx.music.vivimusic.updater.getUpdateNotificationsSetting
import com.convx.music.vivimusic.updater.saveUpdateAvailableState
import com.valentinilk.shimmer.LocalShimmerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Locale
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds


@Suppress("DEPRECATION", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
/**
 * Dynamic-theme colors keyed by song id: [extractThemeColor] decodes + runs
 * Palette on the full artwork for every song, so a bounded cache skips the
 * repeat work when a song recurs in a session.
 */
private val themeColorCache = android.util.LruCache<String, androidx.compose.ui.graphics.Color>(64)

/**
 * Fraction of the surface resolution the top bar's blur strip records its backdrop
 * at. Higher than the nav bar glass's floor: this strip sits over *moving* content,
 * and at 0.33 the upscaled capture crawled visibly as rows scrolled beneath it.
 * Affordable now that the strip is one capture per frame rather than two.
 */
private const val TopBarBackdropScale = 0.45f

/**
 * Blur radius for that strip. Deliberately moderate — a heavy radius over scrolling
 * content smears each row into the next and reads as a glitch rather than as frost.
 * Legibility is topped up by the scrim gradient below the blur, not by the radius.
 */
private val TopBarBlurRadius = 26.dp

/**
 * Routes whose whole screen is an `AndroidView` WebView. They opt out of the app
 * backdrop capture — see the NavHost modifier for why.
 */
private val WebViewRoutes = setOf(
    "login",
    "switch_channel",
    "settings/spotify",
    "settings/discord/login",
)

/**
 * Drives whether the top bar's blur/scrim strip is drawn at all: shown while the
 * list is at the top and while the user is scrolling back up, hidden once they
 * scroll down into the content. Without this the darkened strip sits over every
 * screen permanently.
 *
 * MainActivity doesn't own any screen's list state — the nested scroll stream is
 * the only app-level scroll signal available here, so direction is tracked from
 * the deltas rather than read off a LazyListState.
 */
@Stable
private class TopBarChromeVisibility {
    var visible by mutableStateOf(true)
        private set

    /** True while the content is against its top edge. */
    private var atTop = true

    val connection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            // Finger moving down (positive y) scrolls the content back up.
            val dy = available.y
            if (dy > ScrollSlopPx) {
                visible = true
            } else if (dy < -ScrollSlopPx && !atTop) {
                visible = false
            }
            return Offset.Zero
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            // Leftover downward delta the list could not consume means it is
            // already pinned at its top edge.
            if (available.y > 0f) {
                atTop = true
                visible = true
            } else if (consumed.y != 0f) {
                atTop = false
            }
            return Offset.Zero
        }
    }

    private companion object {
        /** Ignore sub-pixel jitter so the strip doesn't flicker while settling. */
        const val ScrollSlopPx = 1.5f
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    companion object {
        private const val ACTION_SEARCH = "com.BeatWave.music.action.SEARCH"
        private const val ACTION_LIBRARY = "com.BeatWave.music.action.LIBRARY"

        // Ignore a repeat bottom-nav navigate() to the same route within this
        // window, so the floating tab bar's predictive (press/drag) fire can't
        // double-navigate against its own release-fire.
        private const val NavDebounceMs = 250L

        // How long the nav bar's shrink/expand-to-pill animation takes before the
        // actual navigate()/navigateUp() call for entering/exiting search — keeps
        // the animation and the route's own screen transition from overlapping.
        // Comfortably past the crossfade's own ~300ms so it always finishes first.
        private const val SearchNavTransitionDelayMs = 360L
    }

    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var downloadUtil: DownloadUtil

    @Inject
    lateinit var syncUtils: SyncUtils

    @Inject
    lateinit var listenTogetherManager: com.BeatWave.music.listentogether.ListenTogetherManager

    private lateinit var navController: NavHostController
    private var pendingIntent: Intent? = null

    private var playerConnection by mutableStateOf<PlayerConnection?>(null)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is MusicBinder) {
                try {
                    playerConnection = PlayerConnection(this@MainActivity, service, database, lifecycleScope)
                    Timber.tag("MainActivity").d("PlayerConnection created successfully")
                    // Connect Listen Together manager to player
                    listenTogetherManager.setPlayerConnection(playerConnection)
                } catch (e: Exception) {
                    Timber.tag("MainActivity").e(e, "Failed to create PlayerConnection")
                    // Retry after a delay of 500ms
                    lifecycleScope.launch {
                        delay(500.milliseconds)
                        try {
                            playerConnection = PlayerConnection(this@MainActivity, service, database, lifecycleScope)
                            listenTogetherManager.setPlayerConnection(playerConnection)
                        } catch (e2: Exception) {
                            Timber.tag("MainActivity").e(e2, "Failed to create PlayerConnection on retry")
                        }
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            // Disconnect Listen Together manager
            listenTogetherManager.setPlayerConnection(null)
            playerConnection?.dispose()
            playerConnection = null
        }
    }

    override fun onStart() {
        super.onStart()
        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1000)
            }
        }

        // On Android 12+, we can't start foreground services from background
        // Use BIND_AUTO_CREATE which will create the service if needed
        // The service will call startForeground() in onCreate() when bound
        bindService(
            Intent(this, MusicService::class.java),
            serviceConnection,
            BIND_AUTO_CREATE
        )
    }

    override fun onStop() {
        unbindService(serviceConnection)
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (dataStore.get(StopMusicOnTaskClearKey, false) &&
            playerConnection?.isPlaying?.value == true &&
            isFinishing
        ) {
            stopService(Intent(this, MusicService::class.java))
            unbindService(serviceConnection)
            playerConnection = null
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (::navController.isInitialized) {
            handleDeepLinkIntent(intent, navController)
        } else {
            pendingIntent = intent
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_LTR
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Initialize Listen Together manager
        listenTogetherManager.initialize()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val locale = dataStore[AppLanguageKey]
                ?.takeUnless { it == SYSTEM_DEFAULT }
                ?.let { Locale.forLanguageTag(it) }
                ?: Locale.getDefault()
            setAppLocale(this, locale)
        }

        lifecycleScope.launch {
            dataStore.data
                .map { it[DisableScreenshotKey] ?: false }
                .distinctUntilChanged()
                .collectLatest {
                    if (it) {
                        window.setFlags(
                            WindowManager.LayoutParams.FLAG_SECURE,
                            WindowManager.LayoutParams.FLAG_SECURE,
                        )
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
        }

        setContent {
            beatwaveApp(
                playerConnection = playerConnection,
                database = database,
                downloadUtil = downloadUtil,
                syncUtils = syncUtils,
            )
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable
    private fun beatwaveApp(
        playerConnection: PlayerConnection?,
        database: MusicDatabase,
        downloadUtil: DownloadUtil,
        syncUtils: SyncUtils,
    ) {
        val enableDynamicTheme by rememberPreference(DynamicThemeKey, defaultValue = true)
        val enableHighRefreshRate by rememberPreference(EnableHighRefreshRateKey, defaultValue = true)
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            if (getAutoUpdateCheckSetting(context)) {
                // Delay to not block app startup
                delay(2000L.milliseconds)
                checkForUpdate(
                    context = context,
                    onSuccess = { latestVersion, isAvailable, _, _, _, _, _, _ ->
                        val currentVersion = BuildConfig.VERSION_NAME
                        Log.d("UpdateCheck", "Startup check success. Latest: $latestVersion, Current: $currentVersion, isAvailable: $isAvailable")
                        saveUpdateAvailableState(context, isAvailable)
                        
                        if (isAvailable && getUpdateNotificationsSetting(context)) {
                            Log.d("UpdateCheck", "Posting update notification for $latestVersion")
                            UpdateNotificationHelper.showUpdateNotification(context, latestVersion)
                        }
                    },
                    onError = {
                        Log.e("UpdateCheck", "Startup check failed")
                        // Do not clear the state on error, in case of offline launch
                    }
                )
            }
        }

        LaunchedEffect(enableHighRefreshRate) {
            val window = this@MainActivity.window
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val layoutParams = window.attributes
                if (enableHighRefreshRate) {
                    layoutParams.preferredDisplayModeId = 0
                } else {
                    val modes = window.windowManager.defaultDisplay.supportedModes
                    val mode60 = modes.firstOrNull { kotlin.math.abs(it.refreshRate - 60f) < 1f }
                        ?: modes.minByOrNull { kotlin.math.abs(it.refreshRate - 60f) }

                    if (mode60 != null) {
                        layoutParams.preferredDisplayModeId = mode60.modeId
                    }
                }
                window.attributes = layoutParams
            } else {
                val params = window.attributes
                if (enableHighRefreshRate) {
                    params.preferredRefreshRate = 0f
                } else {
                    params.preferredRefreshRate = 60f
                }
                window.attributes = params
            }
        }

        val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
        val isSystemInDarkTheme = isSystemInDarkTheme()
        val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
            if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
        }

        LaunchedEffect(useDarkTheme) {
            setSystemBarAppearance(useDarkTheme)
        }

        val pureBlackEnabled by rememberPreference(PureBlackKey, defaultValue = false)
        val pureBlack = remember(pureBlackEnabled, useDarkTheme) {
            pureBlackEnabled && useDarkTheme
        }

        val (selectedThemeColorInt) = rememberPreference(SelectedThemeColorKey, defaultValue = DefaultThemeColor.toArgb())
        val selectedThemeColor = Color(selectedThemeColorInt)
        val (canvasSource) = rememberEnumPreference(CanvasSourceKey, defaultValue = CanvasSource.AUTO)

        var themeColor by rememberSaveable(stateSaver = ColorSaver) {
            mutableStateOf(selectedThemeColor)
        }

        LaunchedEffect(selectedThemeColor) {
            if (!enableDynamicTheme) {
                themeColor = selectedThemeColor
            }
        }

        LaunchedEffect(playerConnection, enableDynamicTheme, selectedThemeColor) {
            val playerConnection = playerConnection
            if (!enableDynamicTheme || playerConnection == null) {
                themeColor = selectedThemeColor
                return@LaunchedEffect
            }

            playerConnection.service.currentMediaMetadata.collectLatest { song ->
                if (song?.thumbnailUrl != null) {
                    val cached = themeColorCache.get(song.id)
                    if (cached != null) {
                        themeColor = cached
                    } else withContext(Dispatchers.IO) {
                        try {
                            // Prefer a frame from the song's canvas video over the static
                            // cover art when one's already been fetched/confirmed playing
                            // (see Player.kt) — canvas videos are often more colorful/
                            // representative than the plain album art.
                            val canvasVideoUrl = CanvasArtworkPlaybackCache
                                .get("${song.id}:${canvasSource.name}")
                                ?.preferredAnimationUrl

                            val extracted = canvasVideoUrl?.let { extractThemeColorFromVideoFrame(it) }
                                ?: imageLoader.execute(
                                    ImageRequest.Builder(this@MainActivity)
                                        .data(song.thumbnailUrl)
                                        .allowHardware(false)
                                        .memoryCachePolicy(CachePolicy.ENABLED)
                                        .diskCachePolicy(CachePolicy.ENABLED)
                                        .networkCachePolicy(CachePolicy.ENABLED)
                                        .crossfade(false)
                                        .build()
                                ).image?.toBitmap()?.extractThemeColor()

                            if (extracted != null) {
                                themeColorCache.put(song.id, extracted)
                                themeColor = extracted
                            } else {
                                themeColor = selectedThemeColor
                            }
                        } catch (e: Exception) {
                            // Fallback to default on error
                            themeColor = selectedThemeColor
                        }
                    }
                } else {
                    themeColor = selectedThemeColor
                }
            }
        }

        beatwaveTheme(
            darkTheme = useDarkTheme,
            pureBlack = pureBlack,
            themeColor = themeColor,
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface)
            ) {
                val focusManager = LocalFocusManager.current
                val density = LocalDensity.current
                val configuration = LocalWindowInfo.current
                val cutoutInsets = WindowInsets.displayCutout
                val windowsInsets = WindowInsets.systemBars
                val bottomInset = with(density) { windowsInsets.getBottom(density).toDp() }
                val bottomInsetDp = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()

                val navController = rememberNavController()
                val homeViewModel: HomeViewModel = hiltViewModel()
                // Pre-warm HistoryViewModel at Activity scope so history data loads
                // in background immediately — zero lag when user taps the history icon
                hiltViewModel<HistoryViewModel>()
                val accountImageUrl by homeViewModel.accountImageUrl.collectAsStateWithLifecycle()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val (previousTab, setPreviousTab) = rememberSaveable { mutableStateOf("home") }

                val (listenTogetherInTopBar) = rememberPreference(ListenTogetherInTopBarKey, defaultValue = true)
                val navigationItems = remember(listenTogetherInTopBar) {
                    if (listenTogetherInTopBar) {
                        Screens.MainScreens.filter { it != Screens.ListenTogether }
                    } else {
                        Screens.MainScreens
                    }
                }
                val (slimNav) = rememberPreference(SlimNavBarKey, defaultValue = false)
                // Legacy classic nav bar/mini player removed — floating pill system always on.
                val useFloatingNavBar = true
                val (appleMusicUi) = rememberPreference(AppleMusicUiKey, defaultValue = true)
                // The Settings tab is exclusive to the floating (iOS-style) tab bar —
                // the classic nav bar keeps settings behind the top bar icon.
                val floatingNavigationItems = remember(navigationItems) {
                    navigationItems + Screens.Settings
                }
                val floatingNavBarScrollConnection = rememberFloatingTabBarScrollConnection()
                val (liquidGlassGlobalEnabled) = rememberPreference(LiquidGlassGlobalEnabledKey, defaultValue = true)
                val (liquidGlassVibrancy) = rememberPreference(LiquidGlassVibrancyKey, defaultValue = 1.2f)
                val (liquidGlassBlurRadius) = rememberPreference(LiquidGlassBlurRadiusKey, defaultValue = 2f)
                val (liquidGlassLensHeight) = rememberPreference(LiquidGlassLensHeightKey, defaultValue = 0.4f)
                val (liquidGlassLensAmount) = rememberPreference(LiquidGlassLensAmountKey, defaultValue = 0.6f)
                val (liquidGlassChromaticAberration) = rememberPreference(LiquidGlassChromaticAberrationKey, defaultValue = false)
                val (liquidGlassDepthEffect) = rememberPreference(LiquidGlassDepthEffectKey, defaultValue = false)
                // 0 (fully transparent, unreachable from the color picker) marks the
                // theme-adaptive default tint.
                // 0 = theme-adaptive tint (a lighter frosted grey) rather than a
                // fixed dark chip — the dark default read as a near-black bar (and
                // an invisible button surface over a dark hero, e.g. the playlist
                // action buttons).
                val (liquidGlassSurfaceTintColorInt) = rememberPreference(LiquidGlassSurfaceTintColorKey, defaultValue = 0)
                val (liquidGlassSurfaceOpacity) = rememberPreference(LiquidGlassSurfaceOpacityKey, defaultValue = 0.5f)
                val (liquidGlassTextColorInt) = rememberPreference(LiquidGlassTextColorKey, defaultValue = Color.White.toArgb())
                val (liquidGlassPlayerEnabled) = rememberPreference(LiquidGlassPlayerEnabledKey, defaultValue = true)
                val (liquidGlassMiniPlayerEnabled) = rememberPreference(LiquidGlassMiniPlayerEnabledKey, defaultValue = true)
                val (liquidGlassNavBarEnabled) = rememberPreference(LiquidGlassNavBarEnabledKey, defaultValue = true)
                val (liquidGlassSidePanelEnabled) = rememberPreference(LiquidGlassSidePanelEnabledKey, defaultValue = true)
                val (liquidGlassSidePanelVibrancy) = rememberPreference(LiquidGlassSidePanelVibrancyKey, defaultValue = 1.2f)
                val (liquidGlassSidePanelBlurRadius) = rememberPreference(LiquidGlassSidePanelBlurRadiusKey, defaultValue = 2f)
                val (liquidGlassSidePanelLensHeight) = rememberPreference(LiquidGlassSidePanelLensHeightKey, defaultValue = 0.4f)
                val (liquidGlassSidePanelLensAmount) = rememberPreference(LiquidGlassSidePanelLensAmountKey, defaultValue = 0.6f)
                val (liquidGlassSidePanelColorInt) = rememberPreference(LiquidGlassSidePanelColorKey, defaultValue = 0)
                val (liquidGlassSidePanelSurfaceOpacity) = rememberPreference(LiquidGlassSidePanelSurfaceOpacityKey, defaultValue = 0.5f)
                val (liquidGlassSidePanelTextColorInt) = rememberPreference(LiquidGlassSidePanelTextColorKey, defaultValue = Color.White.toArgb())
                val glassEffectConfig = remember(
                    liquidGlassGlobalEnabled, useFloatingNavBar, liquidGlassVibrancy, liquidGlassBlurRadius,
                    liquidGlassLensHeight, liquidGlassLensAmount, liquidGlassChromaticAberration,
                    liquidGlassDepthEffect, liquidGlassSurfaceTintColorInt,
                    liquidGlassSurfaceOpacity, liquidGlassTextColorInt, liquidGlassPlayerEnabled,
                    liquidGlassMiniPlayerEnabled, liquidGlassNavBarEnabled, liquidGlassSidePanelEnabled,
                    liquidGlassSidePanelVibrancy, liquidGlassSidePanelBlurRadius,
                    liquidGlassSidePanelLensHeight, liquidGlassSidePanelLensAmount,
                    liquidGlassSidePanelColorInt, liquidGlassSidePanelSurfaceOpacity, liquidGlassSidePanelTextColorInt,
                ) {
                    // The sliders in Glass settings are always the source of truth: the
                    // Apple Music UI toggle just writes a starting preset into them once
                    // when switched on, so the user can keep tuning from there afterward.
                    GlassEffectConfig(
                        // The glass look is part of the floating nav bar experience, so it
                        // only activates when that bar is enabled too.
                        globalEnabled = liquidGlassGlobalEnabled && useFloatingNavBar,
                        vibrancy = liquidGlassVibrancy,
                        blurRadius = liquidGlassBlurRadius,
                        lensHeight = liquidGlassLensHeight,
                        lensAmount = liquidGlassLensAmount,
                        chromaticAberration = liquidGlassChromaticAberration,
                        depthEffect = liquidGlassDepthEffect,
                        surfaceTintColor = if (liquidGlassSurfaceTintColorInt == 0) {
                            Color.Unspecified
                        } else {
                            Color(liquidGlassSurfaceTintColorInt)
                        },
                        surfaceOpacity = liquidGlassSurfaceOpacity,
                        textColor = Color(liquidGlassTextColorInt),
                        playerEnabled = liquidGlassPlayerEnabled,
                        miniPlayerEnabled = liquidGlassMiniPlayerEnabled,
                        navBarEnabled = liquidGlassNavBarEnabled,
                        sidePanelEnabled = liquidGlassSidePanelEnabled,
                        sidePanelVibrancy = liquidGlassSidePanelVibrancy,
                        sidePanelBlurRadius = liquidGlassSidePanelBlurRadius,
                        sidePanelLensHeight = liquidGlassSidePanelLensHeight,
                        sidePanelLensAmount = liquidGlassSidePanelLensAmount,
                        sidePanelColor = if (liquidGlassSidePanelColorInt == 0) Color.Unspecified else Color(liquidGlassSidePanelColorInt),
                        sidePanelSurfaceOpacity = liquidGlassSidePanelSurfaceOpacity,
                        sidePanelTextColor = Color(liquidGlassSidePanelTextColorInt),
                    )
                }
                val (useNewMiniPlayerDesign) = rememberPreference(UseNewMiniPlayerDesignKey, defaultValue = true)
                val defaultOpenTab = remember {
                    dataStore[DefaultOpenTabKey].toEnum(defaultValue = NavigationTab.HOME)
                }
                val tabOpenedFromShortcut = remember {
                    when (intent?.action) {
                        ACTION_SEARCH -> NavigationTab.LIBRARY
                        ACTION_LIBRARY -> NavigationTab.SEARCH
                        else -> null
                    }
                }

                val topLevelScreens = remember {
                    listOf(
                        Screens.Home.route,
                        Screens.Library.route,
                        Screens.ListenTogether.route,
                        "settings",
                    )
                }

                val (query, onQueryChange) = rememberSaveable(stateSaver = TextFieldValue.Saver) {
                    mutableStateOf(TextFieldValue())
                }
                // Whether the nav bar's search field currently owns the keyboard — the
                // one thing that isn't route-derived (search_input/search/{query} don't
                // change across the first-tap/second-tap boundary). Reset below whenever
                // navigation leaves both search routes.
                var searchKeyboardActive by rememberSaveable { mutableStateOf(false) }
                var searchSource by rememberEnumPreference(SearchSourceKey, SearchSource.ONLINE)
                val searchFocusRequester = remember { FocusRequester() }
                // Non-null while entering/exiting search overrides the route-derived
                // visual state, so the shrink/expand animation plays out before the
                // actual navigation call lands (see enterSearch/exitSearch below).
                var searchVisualOverride by remember { mutableStateOf<Boolean?>(null) }

                val onSearch: (String) -> Unit = remember {
                    { searchQuery ->
                        if (searchQuery.isNotEmpty()) {
                            navController.navigate("search/${URLEncoder.encode(searchQuery, "UTF-8")}") {
                                // No launchSingleTop: it compares destination id, not
                                // resolved args, so re-submitting a new query while
                                // already on search/{oldQuery} could get silently
                                // treated as "already there" and dropped. popUpTo
                                // below still prevents stacking a new entry per edit.
                                popUpTo("search/{query}") { inclusive = true }
                            }

                            if (dataStore[PauseSearchHistoryKey] != true) {
                                lifecycleScope.launch(Dispatchers.IO) {
                                    database.query {
                                        insert(SearchHistory(query = searchQuery))
                                    }
                                }
                            }
                        }
                    }
                }

                // Use derivedStateOf to avoid unnecessary recompositions
                val currentRoute by remember {
                    derivedStateOf { navBackStackEntry?.destination?.route }
                }

                val inSearchScreen by remember {
                    derivedStateOf {
                        currentRoute?.startsWith("search/") == true ||
                            currentRoute == Screens.Search.route
                    }
                }
                val inSearchInputScreen by remember {
                    derivedStateOf { currentRoute == Screens.Search.route }
                }
                LaunchedEffect(inSearchScreen) {
                    if (!inSearchScreen) searchKeyboardActive = false
                }
                // The floating nav bar keeps Settings as one of its own tabs (see
                // floatingNavigationItems above), so it should stay visible there too —
                // only the classic nav bar treats Settings as a top-bar-only destination.
                val navigationItemRoutes = remember(navigationItems, floatingNavigationItems, useFloatingNavBar) {
                    (if (useFloatingNavBar) floatingNavigationItems else navigationItems)
                        .map { it.route }.toSet()
                }

                val isKeyboardOpen = WindowInsets.isImeVisible
                val shouldShowNavigationBar = remember(currentRoute, inSearchInputScreen, useFloatingNavBar) {
                    when {
                        // The floating nav bar renders its own search-mode chrome on
                        // search_input instead of hiding — only the classic bar still
                        // hides there.
                        inSearchInputScreen && !useFloatingNavBar -> false
                        currentRoute?.startsWith("settings/") == true -> false
                        currentRoute in setOf("login", "equalizer", "wrapped", "update", "listen_together/chat") -> false
                        else -> true
                    }
                }

                val isLandscape = configuration.containerDpSize.width > configuration.containerDpSize.height

                // The floating liquid-glass nav bar is a centered bottom pill that
                // works the same in landscape, so don't swap it for the side rail
                // there — only the classic nav bar falls back to the rail.
                //
                // "Tab view" forces the same side layout on any device, including a
                // phone in portrait, so the tablet sidebar can be tried out without
                // a tablet.
                val (forceTabletLayout) = rememberPreference(ForceTabletLayoutKey, defaultValue = false)
                // A real tablet gets the side bar on its own; the toggle only exists
                // to force it onto a device that wouldn't otherwise qualify.
                val isWideScreen = configuration.containerDpSize.width >= TabletWidthThreshold
                val showRail = forceTabletLayout || isWideScreen ||
                    (isLandscape && !useFloatingNavBar && !inSearchInputScreen)
                val (sideBarCollapsed, onSideBarCollapsedChange) = rememberPreference(SideBarCollapsedKey, defaultValue = false)
                val sideBarContentInset by animateDpAsState(
                    targetValue = if (sideBarCollapsed) SideBarCollapsedWidth + SideBarMargin * 2 else SideBarContentInset,
                    animationSpec = spring(0.9f, 400f),
                    label = "sideBarContentInset",
                )

                val navPadding = if (shouldShowNavigationBar && !showRail) {
                    if (slimNav) SlimNavBarHeight else NavigationBarHeight
                } else {
                    0.dp
                }

                val navigationBarHeight by animateDpAsState(
                    targetValue = if (shouldShowNavigationBar && !showRail) NavigationBarHeight else 0.dp,
                    animationSpec = NavigationBarAnimationSpec,
                    label = "navBarHeight",
                )

                val playerMediaMetadata by playerConnection?.mediaMetadata?.collectAsStateWithLifecycle()
                    ?: remember { mutableStateOf(null) }
                // With the floating nav bar the mini player docks into the tab bar as an
                // accessory, and in tab view it docks into the sidebar footer, so in both
                // cases the sheet's collapsed state coincides with dismissed and the
                // standalone collapsed mini player never shows. Without this, turning tab
                // view on swapped the docked accessory for the sheet's own mini player,
                // which is a different component and reads as a different app.
                val playerBottomSheetState = rememberBottomSheetState(
                    dismissedBound = 0.dp,
                    collapsedBound = if (useFloatingNavBar || showRail || inSearchScreen) {
                        0.dp
                    } else {
                        bottomInset +
                            (if (!showRail && shouldShowNavigationBar) navPadding else 0.dp) +
                            (if (useNewMiniPlayerDesign) MiniPlayerBottomSpacing else 0.dp) +
                            MiniPlayerHeight
                    },
                    expandedBound = maxHeight,
                )

                // Only reserve space for the docked player accessory on screens where the
                // floating tab bar is actually visible; other screens (e.g. settings) get
                // the full height. In search mode the mini player still docks (now part of
                // the search-expanded/search-inline chrome) — it only drops out once the
                // keyboard takes over the bar entirely.
                val hasDockedPlayerAccessory =
                    useFloatingNavBar && playerMediaMetadata != null && !showRail && shouldShowNavigationBar &&
                        (!inSearchScreen || !searchKeyboardActive)
                val playerAwareWindowInsets = remember(
                    bottomInset,
                    shouldShowNavigationBar,
                    playerBottomSheetState.isDismissed,
                    showRail,
                    hasDockedPlayerAccessory,
                    sideBarContentInset,
                    inSearchInputScreen,
                ) {
                    var bottom = bottomInset
                    if (shouldShowNavigationBar && !showRail) {
                        bottom += NavigationBarHeight
                    }
                    if (!playerBottomSheetState.isDismissed && !inSearchInputScreen) bottom += MiniPlayerHeight
                    if (hasDockedPlayerAccessory) bottom += DockedAccessoryHeight
                    windowsInsets
                        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                        .add(WindowInsets(top = AppBarHeight, bottom = bottom))
                        // Space for the floating side bar, carried the same way the
                        // bottom bar's space is: screens spend this as list content
                        // padding, so rows start clear of the panel but the list
                        // still spans the full width and scrolls under its glass.
                        .add(
                            WindowInsets(left = if (showRail) sideBarContentInset else 0.dp)
                        )
                }
                appBarScrollBehavior(
                    canScroll = {
                        !inSearchScreen &&
                            (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                    }
                )

                val topAppBarScrollBehavior = appBarScrollBehavior(
                    canScroll = {
                        !inSearchScreen &&
                            (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                    },
                )

                // Shows the top bar's blur/scrim strip only at the top of a list
                // or while scrolling back up — see TopBarChromeVisibility.
                val topBarChrome = remember { TopBarChromeVisibility() }

                // Navigation tracking
                LaunchedEffect(navBackStackEntry) {
                    // Only the results route (search/{query}) carries a query arg;
                    // the search_input landing does not — guard against a null arg
                    // so tapping search never NPEs.
                    val rawQuery = navBackStackEntry?.arguments?.getString("query")
                    if (inSearchScreen && rawQuery != null) {
                        val searchQuery = withContext(Dispatchers.IO) {
                            try {
                                URLDecoder.decode(rawQuery, "UTF-8")
                            } catch (e: IllegalArgumentException) {
                                rawQuery
                            }
                        }
                        onQueryChange(
                            TextFieldValue(
                                searchQuery,
                                TextRange(searchQuery.length)
                            )
                        )
                    } else if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                        onQueryChange(TextFieldValue())
                    }

                    // Reset scroll behavior for main navigation items
                    if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                        if (navigationItems.fastAny { it.route == previousTab }) {
                            topAppBarScrollBehavior.state.resetHeightOffset()
                        }
                    }

                    topAppBarScrollBehavior.state.resetHeightOffset()

                    // Track previous tab for animations
                    navController.currentBackStackEntry?.destination?.route?.let {
                        setPreviousTab(it)
                    }
                }

                LaunchedEffect(playerConnection) {
                    val player = playerConnection?.player ?: return@LaunchedEffect
                    if (player.currentMediaItem == null) {
                        if (!playerBottomSheetState.isDismissed) {
                            playerBottomSheetState.dismiss()
                        }
                    } else {
                        if (playerBottomSheetState.isDismissed) {
                            playerBottomSheetState.collapseSoft()
                        }
                    }
                }

                DisposableEffect(playerConnection, playerBottomSheetState) {
                    val player = playerConnection?.player ?: return@DisposableEffect onDispose { }
                    val listener = object : Player.Listener {
                        override fun onMediaItemTransition(
                            mediaItem: MediaItem?,
                            reason: Int,
                        ) {
                            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED &&
                                mediaItem != null &&
                                playerBottomSheetState.isDismissed
                            ) {
                                playerBottomSheetState.collapseSoft()
                            }
                        }
                    }
                    player.addListener(listener)
                    onDispose {
                        player.removeListener(listener)
                    }
                }

                var shouldShowTopBar by rememberSaveable { mutableStateOf(false) }

                LaunchedEffect(navBackStackEntry, listenTogetherInTopBar, showRail) {
                    val currentRoute = navBackStackEntry?.destination?.route
                    val isListenTogetherScreen = currentRoute == Screens.ListenTogether.route || 
                        currentRoute == "listen_together_from_topbar"
                    shouldShowTopBar = !showRail &&
                        currentRoute in topLevelScreens &&
                        currentRoute != "settings" &&
                        !(isListenTogetherScreen && listenTogetherInTopBar)
                }

                val coroutineScope = rememberCoroutineScope()
                var sharedSong: SongItem? by remember {
                    mutableStateOf(null)
                }
                val snackbarHostState = remember { SnackbarHostState() }
                var showSettingDialoge by remember { mutableStateOf(false) }
                val (enableSettingsPopup) = rememberPreference(EnableSettingsPopupKey, defaultValue = false)

                LaunchedEffect(Unit) {
                    if (pendingIntent != null) {
                        handleDeepLinkIntent(pendingIntent!!, navController)
                        pendingIntent = null
                    } else {
                        handleDeepLinkIntent(intent, navController)
                    }
                }

                DisposableEffect(Unit) {
                    val listener = Consumer<Intent> { intent ->
                        handleDeepLinkIntent(intent, navController)
                    }

                    addOnNewIntentListener(listener)
                    onDispose { removeOnNewIntentListener(listener) }
                }

                val currentTitleRes = remember(navBackStackEntry) {
                    when (navBackStackEntry?.destination?.route) {
                        Screens.Home.route -> R.string.music
                        Screens.Search.route -> R.string.search
                        Screens.Library.route -> R.string.filter_library
                        Screens.ListenTogether.route -> R.string.together
                        else -> null
                    }
                }



                val pauseListenHistory by rememberPreference(PauseListenHistoryKey, defaultValue = false)
                val eventCount by database.eventCount().collectAsStateWithLifecycle(initialValue = 0)
                val showHistoryButton = remember(pauseListenHistory, eventCount) {
                    !(pauseListenHistory && eventCount == 0)
                }

                val baseBg = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer

                // Remember the draw lambda so rememberLayerBackdrop (which keys on it)
                // isn't recreated on every app recomposition, forcing a full backdrop
                // re-record.
                val appBackdrop = rememberLayerBackdrop(
                    onDraw = remember(baseBg) {
                        val bg = baseBg
                        {
                            drawRect(bg)
                            drawContent()
                        }
                    }
                )

                // One provider replaces Android's stretch/glow edge effect with the
                // iOS rubber-band for every scroll container in the app.
                val iosOverscroll by rememberPreference(IosOverscrollKey, defaultValue = false)
                val iosOverscrollFactory = rememberIosOverscrollFactory()

                // Entering/exiting search: hold the nav bar's visual state at the
                // target (search-expanded / normal) for one animation beat before the
                // actual navigation call, so the shrink/expand-to-pill animation plays
                // out first instead of racing the route's own screen transition.
                val enterSearch: () -> Unit = remember(navController, coroutineScope) {
                    {
                        searchVisualOverride = true
                        coroutineScope.launch {
                            delay(SearchNavTransitionDelayMs)
                            navController.navigate(Screens.Search.route) { launchSingleTop = true }
                            searchVisualOverride = null
                        }
                    }
                }
                val exitSearch: () -> Unit = remember(navController, coroutineScope) {
                    {
                        searchKeyboardActive = false
                        searchVisualOverride = false
                        coroutineScope.launch {
                            delay(SearchNavTransitionDelayMs)
                            // Pop the WHOLE search flow, not a single level. Searching
                            // pushes search_input and then search/{query} on top of it,
                            // so navigateUp() from a results screen landed on the hint
                            // screen — which renders nothing once the keyboard is closed,
                            // and read as "the screen I came from lost all its content".
                            // Cancelling search must return to whatever preceded it.
                            if (!navController.popBackStack(Screens.Search.route, inclusive = true)) {
                                navController.navigateUp()
                            }
                            searchVisualOverride = null
                        }
                    }
                }

                // The nav bar (rendered outside/above search_input and search/{query} in
                // the tree below) owns the actual search text field now — both screens
                // just read this to filter/display results.
                val navSearchState = NavSearchState(
                    visualActive = searchVisualOverride ?: inSearchScreen,
                    keyboardActive = searchKeyboardActive,
                    query = query,
                    onQueryChange = onQueryChange,
                    onSubmit = onSearch,
                    searchSource = searchSource,
                    onToggleSource = {
                        searchSource = if (searchSource == SearchSource.ONLINE) SearchSource.LOCAL else SearchSource.ONLINE
                    },
                    onTapSearchIcon = enterSearch,
                    onTapBar = {
                        if (inSearchScreen && !inSearchInputScreen) {
                            // Tapping the bar again from a results screen (search/{query})
                            // pops back to the hint screen (search_input) and opens the
                            // keyboard there instead — a normal, working search, rather
                            // than trying to resubmit in place.
                            navController.popBackStack(Screens.Search.route, inclusive = false)
                        }
                        searchKeyboardActive = true
                    },
                    onExit = exitSearch,
                    onCloseKeyboard = { searchKeyboardActive = false },
                    focusRequester = searchFocusRequester,
                )

                CompositionLocalProvider(
                    LocalOverscrollFactory provides
                        if (iosOverscroll) iosOverscrollFactory else LocalOverscrollFactory.current,
                    LocalDatabase provides database,
                    LocalNavSearchState provides navSearchState,
                    // onSurface already carries the accent-contrast treatment (see
                    // ColorScheme.accentText), so pure black must not force plain white
                    // back over it.
                    LocalContentColor provides MaterialTheme.colorScheme.onSurface,
                    LocalPlayerConnection provides playerConnection,
                    LocalPlayerAwareWindowInsets provides playerAwareWindowInsets,
                    LocalDownloadUtil provides downloadUtil,
                    LocalShimmerTheme provides ShimmerTheme,
                    LocalSyncUtils provides syncUtils,
                    LocalListenTogetherManager provides listenTogetherManager,
                    LocalGlassEffectConfig provides glassEffectConfig,
                    LocalTabView provides showRail,
                    LocalAppBackdrop provides appBackdrop,
                    LocalAppleMusicUi provides appleMusicUi,
                ) {

                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        topBar = {
                            AnimatedVisibility(
                                visible = shouldShowTopBar,
                                enter = fadeIn(animationSpec = tween(durationMillis = 300)),
                                exit = fadeOut(animationSpec = tween(durationMillis = 200))
                            ) {
                                Row {
                                    Box(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // ONE capture layer, masked (DstIn) to fade out over its
                                        // own height, at a moderate radius. It used to be two
                                        // stacked captures (light 18dp over heavy 64dp) chasing a
                                        // true iOS radius ramp; in practice the two low-res
                                        // captures disagreed frame to frame as content scrolled
                                        // under them and the seam between their masks crawled —
                                        // the "glitchy" edge. One capture at a higher record
                                        // scale is steadier, costs about the same (0.45² for one
                                        // vs 2 × 0.33² for two), and the vertical mask still
                                        // gives the sharp-at-the-bottom ramp. Offscreen
                                        // compositing is required for DstIn to mask just this
                                        // layer instead of the whole screen underneath.
                                        //
                                        // Plain alpha rather than AnimatedVisibility: this sits in
                                        // a Box nested in a Row, so AnimatedVisibility resolves
                                        // against RowScope and does not compile here. Animating
                                        // alpha in graphicsLayer keeps the fade off the
                                        // recomposition path anyway, and the `if` drops the
                                        // backdrop capture entirely once it is fully hidden.
                                        //
                                        // The fade is asymmetric on purpose: coming back has
                                        // to feel immediate, because it trails the scroll
                                        // that asked for it. Going away can be lazy.
                                        val chromeAlpha by animateFloatAsState(
                                            targetValue = if (topBarChrome.visible) 1f else 0f,
                                            animationSpec = tween(
                                                durationMillis = if (topBarChrome.visible) 90 else 200
                                            ),
                                            label = "topBarChromeAlpha",
                                        )
                                        if (chromeAlpha > 0.01f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(AppBarHeight * 1.6f)
                                                .align(Alignment.TopCenter)
                                                .graphicsLayer { alpha = chromeAlpha }
                                                .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                                        ) {
                                            // The mask holds its value through the title band and
                                            // only ramps out in the lower part of the strip — a
                                            // plain two-stop Black->Transparent gradient starts
                                            // fading from the first pixel, leaving the title over
                                            // an already half-faded blur.
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                                    .drawWithContent {
                                                        drawContent()
                                                        drawRect(
                                                            brush = Brush.verticalGradient(
                                                                0f to Color.Black,
                                                                0.60f to Color.Black,
                                                                1f to Color.Transparent,
                                                            ),
                                                            blendMode = BlendMode.DstIn,
                                                        )
                                                    }
                                                    .drawPlainBackdrop(
                                                        backdrop = LocalAppBackdrop.current,
                                                        shape = { RectangleShape },
                                                        effects = {
                                                            // blur() takes px, not dp — the raw-float
                                                            // calls here before were off by the device
                                                            // density factor (~3x), so bumping the float
                                                            // barely moved the real radius. Pre-multiplied
                                                            // by the record scale below, same as
                                                            // Modifier.liquidGlass does.
                                                            if (isRenderEffectSupported()) {
                                                                blur(TopBarBlurRadius.toPx() * TopBarBackdropScale)
                                                            }
                                                        },
                                                        backdropScale = TopBarBackdropScale,
                                                    )
                                            )

                                            // Darkening scrim over the blur layer, behind the
                                            // sharp title/actions drawn by the TopAppBar below.
                                            // Still light, but doing slightly more of the work
                                            // than it used to: it now covers the legibility the
                                            // dropped heavy-blur layer was providing, without
                                            // painting the strip near-black.
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        Brush.verticalGradient(
                                                            0f to Color.Black.copy(alpha = 0.46f),
                                                            0.50f to Color.Black.copy(alpha = 0.30f),
                                                            0.78f to Color.Black.copy(alpha = 0.10f),
                                                            1f to Color.Transparent,
                                                        )
                                                    )
                                            )
                                        }
                                        }
                                        TopAppBar(
                                        title = {
                                            // Home shows the wordmark; every other tab keeps
                                            // its own title.
                                            val isHome =
                                                navBackStackEntry?.destination?.route == Screens.Home.route
                                            Text(
                                                text = if (isHome) {
                                                    BrandName
                                                } else {
                                                    currentTitleRes?.let { stringResource(it) } ?: ""
                                                },
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    // Display face is for the wordmark only;
                                                    // other tab titles keep the app typeface.
                                                    fontFamily = if (isHome) rememberBrandFontFamily() else null,
                                                    fontWeight = if (isHome) FontWeight.SemiBold else FontWeight.Bold,
                                                    fontSize = 24.sp,
                                                    letterSpacing = if (isHome) 1.5.sp else 0.sp,
                                                ),
                                                // The bar is a Scaffold slot outside the screen
                                                // content, so this is the app-wide accent text
                                                // colour, not any screen's hero tint.
                                                color = LocalAccentTextColor.current,
                                                modifier = Modifier.padding(start = 4.dp),
                                            )
                                        },
                                        actions = {
                                            if (showHistoryButton) {
                                                IconButton(onClick = { navController.navigate("history") }) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.music_history),
                                                        contentDescription = stringResource(R.string.history)
                                                    )
                                                }
                                            }
                                            IconButton(onClick = { navController.navigate("stats") }) {
                                                Icon(
                                                    painter = painterResource(R.drawable.stats),
                                                    contentDescription = stringResource(R.string.stats)
                                                )
                                            }
                                            if (listenTogetherInTopBar) {
                                                IconButton(onClick = { navController.navigate("listen_together_from_topbar") }) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.group_outlined),
                                                        contentDescription = stringResource(R.string.together)
                                                    )
                                                }
                                            }
                                             IconButton(onClick = {
                                                  if (enableSettingsPopup) {
                                                      showSettingDialoge = true
                                                  } else {
                                                      navController.navigate("settings")
                                                  }
                                              }) {
                                                BadgedBox(badge = {}) {
                                                    if (accountImageUrl != null) {
                                                        AsyncImage(
                                                            model = accountImageUrl,
                                                            contentDescription = stringResource(R.string.account),
                                                            modifier = Modifier
                                                                .size(24.dp)
                                                                .clip(CircleShape)
                                                        )
                                                    } else {
                                                        val composition by rememberLottieComposition(
                                                            LottieCompositionSpec.RawRes(R.raw.setting)
                                                        )
                                                        val progress by animateLottieCompositionAsState(
                                                            composition = composition,
                                                            isPlaying = true,
                                                            iterations = 1,
                                                            speed = 1.5f
                                                        )

                                                        LottieAnimation(
                                                            composition = composition,
                                                            progress = { progress },
                                                            modifier = Modifier.size(50.dp),
                                                            contentScale = ContentScale.Fit
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        scrollBehavior = topAppBarScrollBehavior,
                                        colors = TopAppBarDefaults.topAppBarColors(
                                            // Floating: transparent bar over the content/hero background.
                                            containerColor = Color.Transparent,
                                            scrolledContainerColor = Color.Transparent,
                                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                                            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.windowInsetsPadding(
                                            // The side bar is a compact floating capsule
                                            // centred vertically, not a full-height
                                            // panel, so it never reaches the top bar and
                                            // the top bar needs no inset for it.
                                            cutoutInsets.only(WindowInsetsSides.Start + WindowInsetsSides.End)
                                        )
                                    )
                                    }
                                }
                            }
                        },
                        bottomBar = {
                            val onNavItemClick: (Screens, Boolean) -> Unit = remember(navController, coroutineScope, topAppBarScrollBehavior, playerBottomSheetState) {
                                var lastNavRoute: String? = null
                                var lastNavTimeMs = 0L
                                { screen: Screens, isSelected: Boolean ->
                                    if (playerBottomSheetState.isExpanded) {
                                        playerBottomSheetState.collapseSoft()
                                    }

                                    if (isSelected) {
                                        navController.currentBackStackEntry?.savedStateHandle?.set("scrollToTop", true)
                                        coroutineScope.launch {
                                            topAppBarScrollBehavior.state.resetHeightOffset()
                                        }
                                    } else {
                                        val now = SystemClock.elapsedRealtime()
                                        // The debounce below only suppresses a genuine duplicate
                                        // fire for a tab we are still sitting on. Once the user
                                        // has moved anywhere else (tapped a song, opened an
                                        // artist), the remembered route is stale — leaving it set
                                        // meant a tab tap within the debounce window was silently
                                        // swallowed and you stayed on the previous screen, which
                                        // is why Home sometimes did nothing.
                                        if (navController.currentDestination?.route != lastNavRoute) {
                                            lastNavRoute = null
                                        }
                                        // Guards against a double-fire from the floating tab bar's
                                        // predictive (press/drag-threshold) nav landing on top of the
                                        // puck's own release-fire for the same target.
                                        if (screen.route != lastNavRoute || now - lastNavTimeMs >= NavDebounceMs) {
                                            lastNavRoute = screen.route
                                            lastNavTimeMs = now
                                            navController.navigate(screen.route) {
                                                // Preserve each tab's own back stack across tab
                                                // switches (multi-back-stack): drilling into a
                                                // detail on one tab, switching away and back
                                                // restores where you were instead of the tab root.
                                                popUpTo(navController.graph.startDestinationId) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                }
                            }

                            val onSearchLongClick: () -> Unit = remember(navController) {
                                {
                                    navController.navigate("recognition") {
                                        launchSingleTop = true
                                    }
                                }
                            }

                            // Pre-calculate values for graphicsLayer to avoid reading state during composition
                            val navBarTotalHeight = bottomInset + NavigationBarHeight

                            if (!showRail && !showSettingDialoge && currentRoute?.startsWith("settings/") != true && currentRoute !in setOf("wrapped", "update", "listen_together/chat", "login", "equalizer")) {
                                Box {
                                    // Apple Music-style progressive scrim: content fades out under
                                    // the floating glass bar instead of hard-clipping, so the bar
                                    // stays legible over bright artwork.  The horizontal gradient
                                    // on the left/right edges mimics Apple Music's edge blur.
                                    if (appleMusicUi && useFloatingNavBar && isGlassAllowed()) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .fillMaxWidth()
                                                .height(navBarTotalHeight + 56.dp)
                                                .background(
                                                    Brush.verticalGradient(
                                                        0f to Color.Transparent,
                                                        0.35f to baseBg.copy(alpha = 0.15f),
                                                        0.6f to baseBg.copy(alpha = 0.4f),
                                                        0.85f to baseBg.copy(alpha = 0.7f),
                                                        1f to baseBg,
                                                    )
                                                )
                                        )
                                    }

                                    BottomSheetPlayer(
                                        state = playerBottomSheetState,
                                        navController = navController,
                                        pureBlack = pureBlack
                                    )

                                    // Use graphicsLayer instead of offset to avoid recomposition
                                    // graphicsLayer runs during draw phase, not composition phase
                                    val navBarGraphicsLayer: Modifier = Modifier.graphicsLayer {
                                        val navBarHeightPx = navigationBarHeight.toPx()
                                        val totalHeightPx = navBarTotalHeight.toPx()

                                        translationY = if (navBarHeightPx == 0f) {
                                            totalHeightPx
                                        } else {
                                            // Read progress only during draw phase
                                            val progress = playerBottomSheetState.progress.coerceIn(0f, 1f)
                                            val slideOffset = totalHeightPx * progress
                                            val hideOffset = totalHeightPx * (1 - navBarHeightPx / NavigationBarHeight.toPx())
                                            slideOffset + hideOffset
                                        }
                                    }

                                    if (useFloatingNavBar) {
                                        AppFloatingNavBar(
                                            navigationItems = floatingNavigationItems,
                                            currentRoute = currentRoute,
                                            onItemClick = onNavItemClick,
                                            scrollConnection = floatingNavBarScrollConnection,
                                            pureBlack = pureBlack,
                                            showPlayerAccessory = hasDockedPlayerAccessory,
                                            onAccessoryClick = { playerBottomSheetState.expandSoft() },
                                            onAccessoryLyricsClick = {
                                                playerBottomSheetState.expandSoft()
                                                playerConnection?.requestShowLyrics?.value = true
                                            },
                                            onAccessoryQueueClick = {
                                                playerBottomSheetState.expandSoft()
                                                playerConnection?.requestShowQueue?.value = true
                                            },
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                // Keep the bar compact/portrait-width in landscape
                                                // instead of stretching across the wide screen.
                                                .widthIn(max = 500.dp)
                                                .padding(horizontal = 16.dp)
                                                .padding(bottom = bottomInset + 8.dp)
                                                .graphicsLayer {
                                                    // The floating bar (especially with the docked
                                                    // player accessory) is taller than the classic
                                                    // nav bar, so hide it by its own measured height
                                                    // instead of the fixed nav bar height.
                                                    val hiddenOffset =
                                                        size.height + (bottomInset + 8.dp).toPx()
                                                    val navBarHeightPx = navigationBarHeight.toPx()
                                                    translationY = if (navBarHeightPx == 0f) {
                                                        hiddenOffset
                                                    } else {
                                                        val progress = playerBottomSheetState.progress.coerceIn(0f, 1f)
                                                        val slideOffset = hiddenOffset * progress
                                                        val hideOffset = hiddenOffset * (1 - navBarHeightPx / NavigationBarHeight.toPx())
                                                        slideOffset + hideOffset
                                                    }
                                                }
                                        )
                                    } else {
                                        AppNavigationBar(
                                            glassEnabled = true,
                                            navigationItems = navigationItems,
                                            currentRoute = currentRoute,
                                            onItemClick = onNavItemClick,
                                            pureBlack = pureBlack,
                                            slimNav = slimNav,
                                            onSearchLongClick = onSearchLongClick,
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .height(bottomInset + navPadding)
                                                .then(navBarGraphicsLayer)
                                        )
                                    }


                                    // The floating nav bar is edge-to-edge, so skip the opaque
                                    // strip that backs the classic nav bar's system inset area.
                                    if (!useFloatingNavBar) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .align(Alignment.BottomCenter)
                                                .height(bottomInsetDp)
                                                // Use graphicsLayer for background color changes
                                                .graphicsLayer {
                                                    val progress = playerBottomSheetState.progress
                                                    alpha = if (progress > 0f || (useNewMiniPlayerDesign && !shouldShowNavigationBar)) 0f else 1f
                                                }
                                                .background(baseBg)
                                        )
                                    }
                                }
                            } else {
                                if (currentRoute != "wrapped" && currentRoute != "update" && currentRoute != "listen_together/chat") {
                                    BottomSheetPlayer(
                                        state = playerBottomSheetState,
                                        navController = navController,
                                        pureBlack = pureBlack
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .height(bottomInsetDp)
                                        // Use graphicsLayer for background color changes
                                        .graphicsLayer {
                                            val progress = playerBottomSheetState.progress
                                            alpha = if (progress > 0f || (useNewMiniPlayerDesign && !shouldShowNavigationBar)) 0f else 1f
                                        }
                                        .background(baseBg)
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            val onRailItemClick: (Screens, Boolean) -> Unit = remember(navController, coroutineScope, topAppBarScrollBehavior, playerBottomSheetState) {
                                var lastNavRoute: String? = null
                                var lastNavTimeMs = 0L
                                { screen: Screens, isSelected: Boolean ->
                                    if (playerBottomSheetState.isExpanded) {
                                        playerBottomSheetState.collapseSoft()
                                    }

                                    if (isSelected) {
                                        navController.currentBackStackEntry?.savedStateHandle?.set("scrollToTop", true)
                                        coroutineScope.launch {
                                            topAppBarScrollBehavior.state.resetHeightOffset()
                                        }
                                    } else {
                                        val now = SystemClock.elapsedRealtime()
                                        if (screen.route != lastNavRoute || now - lastNavTimeMs >= NavDebounceMs) {
                                            lastNavRoute = screen.route
                                            lastNavTimeMs = now
                                            navController.navigate(screen.route) {
                                                // Preserve each tab's own back stack across tab
                                                // switches (multi-back-stack): drilling into a
                                                // detail on one tab, switching away and back
                                                // restores where you were instead of the tab root.
                                                popUpTo(navController.graph.startDestinationId) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                }
                            }

                            val onRailSearchLongClick: () -> Unit = remember(navController) {
                                {
                                    navController.navigate("recognition") {
                                        launchSingleTop = true
                                    }
                                }
                            }

                            Box(Modifier.fillMaxSize()) {
                                // NavHost with animations (Material 3 Expressive style)
                                NavHost(
                                    navController = navController,
                                    startDestination = when (tabOpenedFromShortcut ?: defaultOpenTab) {
                                        NavigationTab.HOME -> Screens.Home
                                        NavigationTab.LIBRARY -> Screens.Library
                                        else -> Screens.Home
                                    }.route,
                                    // Enter Transition - smoother with smaller offset and longer duration
                                    enterTransition = {
                                        val currentRouteIndex = navigationItems.indexOfFirst {
                                            it.route == targetState.destination.route
                                        }
                                        val previousRouteIndex = navigationItems.indexOfFirst {
                                            it.route == initialState.destination.route
                                        }

                                        if (currentRouteIndex == -1 || currentRouteIndex > previousRouteIndex)
                                            slideInHorizontally { it / 8 } + fadeIn(tween(200))
                                        else
                                            slideInHorizontally { -it / 8 } + fadeIn(tween(200))
                                    },
                                    // Exit Transition - smoother with smaller offset and longer duration
                                    exitTransition = {
                                        val currentRouteIndex = navigationItems.indexOfFirst {
                                            it.route == initialState.destination.route
                                        }
                                        val targetRouteIndex = navigationItems.indexOfFirst {
                                            it.route == targetState.destination.route
                                        }

                                        if (targetRouteIndex == -1 || targetRouteIndex > currentRouteIndex)
                                            slideOutHorizontally { -it / 8 } + fadeOut(tween(200))
                                        else
                                            slideOutHorizontally { it / 8 } + fadeOut(tween(200))
                                    },
                                    // Pop Enter Transition - smoother with smaller offset and longer duration
                                    popEnterTransition = {
                                        val currentRouteIndex = navigationItems.indexOfFirst {
                                            it.route == targetState.destination.route
                                        }
                                        val previousRouteIndex = navigationItems.indexOfFirst {
                                            it.route == initialState.destination.route
                                        }

                                        if (previousRouteIndex != -1 && previousRouteIndex < currentRouteIndex)
                                            slideInHorizontally { it / 8 } + fadeIn(tween(200))
                                        else
                                            slideInHorizontally { -it / 8 } + fadeIn(tween(200))
                                    },
                                    // Pop Exit Transition - smoother with smaller offset and longer duration
                                    popExitTransition = {
                                        val currentRouteIndex = navigationItems.indexOfFirst {
                                            it.route == initialState.destination.route
                                        }
                                        val targetRouteIndex = navigationItems.indexOfFirst {
                                            it.route == targetState.destination.route
                                        }

                                        if (currentRouteIndex != -1 && currentRouteIndex < targetRouteIndex)
                                            slideOutHorizontally { -it / 8 } + fadeOut(tween(200))
                                        else
                                            slideOutHorizontally { it / 8 } + fadeOut(tween(200))
                                    },
                                    modifier = Modifier
                                        // Skipped on WebView routes: layerBackdrop records
                                        // this whole subtree into a GraphicsLayer so glass
                                        // can sample it, which means the subtree is drawn
                                        // twice per frame. A WebView renders on its own
                                        // hardware canvas and does not survive that second
                                        // pass intact — the page tears and flickers while
                                        // still being usable, exactly the reported symptom.
                                        // Those screens are plain full-bleed WebViews with
                                        // their own Material top bar, so they have no glass
                                        // to feed anyway.
                                        .then(
                                            if (currentRoute in WebViewRoutes) {
                                                Modifier
                                            } else {
                                                Modifier.layerBackdrop(appBackdrop)
                                            }
                                        )
                                        .nestedScroll(topBarChrome.connection)
                                        .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                                        .then(
                                            if (useFloatingNavBar) {
                                                Modifier.nestedScroll(floatingNavBarScrollConnection)
                                            } else {
                                                Modifier
                                            }
                                        )
                                ) {
                                    navigationBuilder(
                                        navController = navController,
                                        scrollBehavior = topAppBarScrollBehavior,
                                        activity = this@MainActivity,
                                        snackbarHostState = snackbarHostState
                                    )
                                }
                            }

                            // Both float OVER the full-width NavHost, exactly as the
                            // bottom bar does on a phone: nothing reserves layout
                            // width for them, so content runs underneath instead of
                            // being clipped short of them.
                            if (showRail) {
                                // No global floating Back here anymore: each screen's own
                                // back button (TopAppBar nav icon or floating chrome row)
                                // now insets clear of the side panel, so a second global
                                // one just duplicated it.

                                // Everything the top bar carries on a phone moves in
                                // here, plus the user's pinned playlists — the DAO's
                                // playlists() already filters to bookmarkedAt, so this
                                // is the pinned set, not every local playlist.
                                val sidebarPlaylists by database
                                    .playlists(PlaylistSortType.NAME, descending = false)
                                    .collectAsStateWithLifecycle(initialValue = emptyList())
                                val sidebarSections = remember(
                                    showHistoryButton,
                                    listenTogetherInTopBar,
                                    sidebarPlaylists,
                                ) {
                                    buildList {
                                        val quick = buildList {
                                            if (showHistoryButton) {
                                                add(
                                                    SideBarLink(
                                                        label = getString(R.string.history),
                                                        iconRes = R.drawable.music_history,
                                                        onClick = { navController.navigate("history") },
                                                    )
                                                )
                                            }
                                            add(
                                                SideBarLink(
                                                    label = getString(R.string.stats),
                                                    iconRes = R.drawable.stats,
                                                    onClick = { navController.navigate("stats") },
                                                )
                                            )
                                            if (listenTogetherInTopBar) {
                                                add(
                                                    SideBarLink(
                                                        label = getString(R.string.together),
                                                        iconRes = R.drawable.group_outlined,
                                                        onClick = {
                                                            navController.navigate("listen_together_from_topbar")
                                                        },
                                                    )
                                                )
                                            }
                                        }
                                        if (quick.isNotEmpty()) add(SideBarSection(links = quick))

                                        if (sidebarPlaylists.isNotEmpty()) {
                                            add(
                                                SideBarSection(
                                                    title = getString(R.string.playlists),
                                                    links = sidebarPlaylists.map { playlist ->
                                                        SideBarLink(
                                                            label = playlist.playlist.name,
                                                            thumbnailUrl = playlist.thumbnails.firstOrNull(),
                                                            onClick = {
                                                                navController.navigate(
                                                                    "local_playlist/${playlist.id}"
                                                                )
                                                            },
                                                        )
                                                    },
                                                )
                                            )
                                        }
                                    }
                                }

                                AppFloatingSideBar(
                                    navigationItems = navigationItems,
                                    currentRoute = currentRoute,
                                    onItemClick = onRailItemClick,
                                    sections = sidebarSections,
                                    pureBlack = pureBlack,
                                    collapsed = sideBarCollapsed,
                                    onToggleCollapsed = { onSideBarCollapsedChange(!sideBarCollapsed) },
                                    footer = { footerCollapsed ->
                                        SideBarAccountRow(
                                            accountImageUrl = accountImageUrl,
                                            collapsed = footerCollapsed,
                                            onClick = {
                                                if (enableSettingsPopup) {
                                                    showSettingDialoge = true
                                                } else {
                                                    navController.navigate("settings")
                                                }
                                            },
                                        )
                                    },
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .windowInsetsPadding(
                                            windowsInsets.only(WindowInsetsSides.Vertical)
                                        )
                                        .padding(start = SideBarMargin, top = 8.dp, bottom = 8.dp),
                                )

                                // The phone bar's docked accessory, floating free on
                                // the opposite edge — same pill, same glass, bottom
                                // right rather than centred. Swaps for the real
                                // search input bar while searching, same slide+fade
                                // the phone's own AppFloatingNavBar uses to hide its
                                // mini player for the keyboard.
                                if (playerMediaMetadata != null || inSearchScreen) {
                                    // Centred on the content area, not the screen:
                                    // the side bar's space is padded out first, and
                                    // the pill takes 80% of whatever is left.
                                    BoxWithConstraints(
                                        Modifier
                                            .fillMaxSize()
                                            .padding(start = sideBarContentInset),
                                    ) {
                                        val pillWidth = maxWidth * FloatingMiniPlayerWidthFraction
                                        AnimatedContent(
                                            targetState = inSearchScreen,
                                            transitionSpec = {
                                                if (targetState) {
                                                    (slideInVertically(
                                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                                                    ) { it / 2 } + fadeIn()) togetherWith
                                                        (slideOutVertically(
                                                            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                                                        ) { it } + fadeOut())
                                                } else {
                                                    (slideInVertically(
                                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                                                    ) { it } + fadeIn()) togetherWith
                                                        (slideOutVertically(
                                                            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                                                        ) { it / 2 } + fadeOut())
                                                }
                                            },
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .windowInsetsPadding(
                                                    windowsInsets.only(WindowInsetsSides.Bottom)
                                                )
                                                .padding(bottom = 12.dp),
                                            label = "tabViewSearchPill",
                                        ) { searching ->
                                            if (searching) {
                                                NavBarSearchInputBar(
                                                    state = navSearchState,
                                                    pureBlack = pureBlack,
                                                    modifier = Modifier.width(pillWidth),
                                                )
                                            } else if (playerMediaMetadata != null) {
                                                AppFloatingNowPlayingPill(
                                                    onClick = { playerBottomSheetState.expandSoft() },
                                                    onLyricsClick = {
                                                        playerBottomSheetState.expandSoft()
                                                        playerConnection?.requestShowLyrics?.value = true
                                                    },
                                                    onQueueClick = {
                                                        playerBottomSheetState.expandSoft()
                                                        playerConnection?.requestShowQueue?.value = true
                                                    },
                                                    pureBlack = pureBlack,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    BottomSheetMenu(
                        state = LocalMenuState.current,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )

                    BottomSheetPage(
                        state = LocalBottomSheetPageState.current,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )



                    sharedSong?.let { song ->
                        playerConnection?.let {
                            Dialog(
                                onDismissRequest = { sharedSong = null },
                                properties = DialogProperties(usePlatformDefaultWidth = false),
                            ) {
                                Surface(
                                    modifier = Modifier.padding(24.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    color = AlertDialogDefaults.containerColor,
                                    tonalElevation = AlertDialogDefaults.TonalElevation,
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        YouTubeSongMenu(
                                            song = song,
                                            navController = navController,
                                            onDismiss = { sharedSong = null },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (showSettingDialoge) {
                        SettingDialoge(
                            onDismissRequest = { showSettingDialoge = false },
                            onNavigate = { route ->
                                showSettingDialoge = false
                                navController.navigate(route)
                            },
                            homeViewModel = homeViewModel
                        )
                    }
                }
            }
        }
    }

    private fun handleDeepLinkIntent(intent: Intent, navController: NavHostController) {
        val uri = intent.data ?: intent.extras?.getString(Intent.EXTRA_TEXT)?.toUri() ?: return
        intent.data = null
        intent.removeExtra(Intent.EXTRA_TEXT)
        val coroutineScope = lifecycle.coroutineScope

        val listenCode = uri.getQueryParameter("code")
            ?: uri.getQueryParameter("room")
            ?: uri.pathSegments.getOrNull(1)
        val isListenLink = uri.pathSegments.firstOrNull() == "listen" || uri.host?.equals("listen", ignoreCase = true) == true
        if (!listenCode.isNullOrBlank() && isListenLink) {
            val username = dataStore.get(ListenTogetherUsernameKey, "").ifBlank { "Guest" }
            listenTogetherManager.joinRoom(listenCode, username)
            return
        }

        when (val path = uri.pathSegments.firstOrNull()) {
            "playlist" -> uri.getQueryParameter("list")?.let { playlistId ->
                if (playlistId.startsWith("OLAK5uy_")) {
                    coroutineScope.launch(Dispatchers.IO) {
                        YouTube.albumSongs(playlistId).onSuccess { songs ->
                            songs.firstOrNull()?.album?.id?.let { browseId ->
                                withContext(Dispatchers.Main) {
                                    navController.navigate("album/$browseId")
                                }
                            }
                        }.onFailure { reportException(it) }
                    }
                } else {
                    navController.navigate("online_playlist/$playlistId")
                }
            }

            "browse" -> uri.lastPathSegment?.let { browseId ->
                navController.navigate("album/$browseId")
            }

            "channel", "c" -> uri.lastPathSegment?.let { artistId ->
                navController.navigate("artist/$artistId")
            }

            "search" -> {
                uri.getQueryParameter("q")?.let {
                    navController.navigate("search/${URLEncoder.encode(it, "UTF-8")}")
                }
            }

            else -> {
                val videoId = when {
                    path == "watch" -> uri.getQueryParameter("v")
                    uri.host == "youtu.be" -> uri.pathSegments.firstOrNull()
                    else -> null
                }

                val playlistId = uri.getQueryParameter("list")

                if (videoId != null) {
                    coroutineScope.launch(Dispatchers.IO) {
                        YouTube.queue(listOf(videoId), playlistId).onSuccess { queue ->
                            withContext(Dispatchers.Main) {
                                playerConnection?.playQueue(
                                    YouTubeQueue(
                                        WatchEndpoint(videoId = queue.firstOrNull()?.id, playlistId = playlistId),
                                        queue.firstOrNull()?.toMediaMetadata()
                                    )
                                )
                            }
                        }.onFailure {
                            reportException(it)
                        }
                    }
                } else if (playlistId != null) {
                    coroutineScope.launch(Dispatchers.IO) {
                        YouTube.queue(null, playlistId).onSuccess { queue ->
                            val firstItem = queue.firstOrNull()
                            withContext(Dispatchers.Main) {
                                playerConnection?.playQueue(
                                    YouTubeQueue(
                                        WatchEndpoint(videoId = firstItem?.id, playlistId = playlistId),
                                        firstItem?.toMediaMetadata()
                                    )
                                )
                            }
                        }.onFailure {
                            reportException(it)
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun setSystemBarAppearance(isDark: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView.rootView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            window.statusBarColor = (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            window.navigationBarColor = (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }
    }
}

val LocalDatabase = staticCompositionLocalOf<MusicDatabase> { error("No database provided") }
val LocalPlayerConnection = staticCompositionLocalOf<PlayerConnection?> { error("No PlayerConnection provided") }
val LocalPlayerAwareWindowInsets = compositionLocalOf<WindowInsets> { error("No WindowInsets provided") }
val LocalDownloadUtil = staticCompositionLocalOf<DownloadUtil> { error("No DownloadUtil provided") }
val LocalSyncUtils = staticCompositionLocalOf<SyncUtils> { error("No SyncUtils provided") }
val LocalListenTogetherManager = staticCompositionLocalOf<com.BeatWave.music.listentogether.ListenTogetherManager?> { null }
val LocalIsPlayerExpanded = compositionLocalOf { false }

/**
 * True while the app is laid out for tab view — the vertical floating side bar
 * instead of the bottom bar. Screens read this to switch their header to the
 * wide arrangement (hero as a card with its title beside it) rather than the
 * phone's full-bleed hero.
 */
val LocalTabView = compositionLocalOf { false }



