/**
 * BeatWave Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.convx.music.ui.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.convx.music.R
import com.convx.music.constants.SearchSource
import com.convx.music.ui.component.backdrop.catalog.utils.InteractiveHighlight
import com.convx.music.ui.player.FloatingMiniPlayer
import com.convx.music.ui.screens.Screens
import com.convx.music.ui.screens.search.DynamicSearchPlaceholder
import com.convx.music.ui.component.floatingtabbar.FloatingTabBar
import com.convx.music.ui.component.floatingtabbar.LocalTabBarBackdropFrozen
import com.convx.music.ui.component.floatingtabbar.FloatingTabBarDefaults
import com.convx.music.ui.component.floatingtabbar.FloatingTabBarScrollConnection
import com.convx.music.ui.component.shapes.ContinuousRoundedRectangle
import com.convx.music.ui.utils.bounceClick
import kotlinx.coroutines.delay

// Kyant0/Capsule's continuous (superellipse) capsule instead of a circular-arc
// RoundedCornerShape — smoother corners, and lerp-able for the puck's
// drag-to-search morph (see FloatingTabBar's ExpandedTabs).
private val NavBarShape = ContinuousRoundedRectangle(percent = 50)

// Matches FloatingTabBar's own SearchBarRowHeight (the search-expanded row's
// height, itself matched to the inline mini player's shrunk height) so the
// bar doesn't visibly resize the moment the keyboard opens.
private val NavBarSearchBarHeight = 48.dp

// How long the mini player/icon hide transition (AnimatedContent in
// AppFloatingNavBar) takes to clear the screen before the keyboard pulls up.
private const val KeyboardOpenDelayMs = 260L

/**
 * The iOS 26 style floating navigation bar, an alternative to [AppNavigationBar].
 *
 * Collapses to an inline pill while scrolling down (driven by [scrollConnection]) and
 * expands back on scroll up. The search destination is rendered as the standalone
 * circular tab. When the liquid glass effect is enabled for the navigation bar, the tab
 * bar surfaces sample the app backdrop through [Modifier.liquidGlass].
 *
 * When [showPlayerAccessory] is true the now playing controls dock into the bar as an
 * accessory (a pill above the tabs when expanded, inline between the tab pill and the
 * search tab when collapsed) and [onAccessoryClick] opens the full player.
 *
 * Search mode (whenever [currentRoute] is `search_input` or `search/{query}`) is driven
 * by [LocalNavSearchState]: the tab group shrinks to the current screen's own icon and
 * the search circle expands into a search bar (see [FloatingTabBar]'s searchMode). A
 * second tap on that bar flips [NavSearchState.keyboardActive], swapping the whole bar
 * for [NavBarSearchInputBar] — a real text field docked above the keyboard.
 */
@Composable
fun AppFloatingNavBar(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    scrollConnection: FloatingTabBarScrollConnection,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    showPlayerAccessory: Boolean = false,
    onAccessoryClick: () -> Unit = {},
    onAccessoryLyricsClick: (() -> Unit)? = null,
    onAccessoryQueueClick: (() -> Unit)? = null,
) {
    val navSearch = LocalNavSearchState.current

    // Slide + spring bounce instead of a flat Crossfade: the mini player/current-
    // screen icon side drops down and out, the keyboard pill rises up and in
    // (and reverses on the way back). Bouncier (Medium) coming in, no-bounce
    // going out — matches how the rest of the bar's own spring transitions
    // read (settle in, not settle out).
    AnimatedContent(
        targetState = navSearch.keyboardActive,
        modifier = modifier,
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
        label = "navBarKeyboardActive",
    ) { keyboardActive ->
        if (keyboardActive) {
            NavBarSearchInputBar(
                state = navSearch,
                pureBlack = pureBlack,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            AppFloatingNavBarChrome(
                navigationItems = navigationItems,
                currentRoute = currentRoute,
                onItemClick = onItemClick,
                scrollConnection = scrollConnection,
                pureBlack = pureBlack,
                showPlayerAccessory = showPlayerAccessory,
                onAccessoryClick = onAccessoryClick,
                onAccessoryLyricsClick = onAccessoryLyricsClick,
                onAccessoryQueueClick = onAccessoryQueueClick,
                searchModeActive = navSearch.visualActive,
                navSearch = navSearch,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun AppFloatingNavBarChrome(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    scrollConnection: FloatingTabBarScrollConnection,
    pureBlack: Boolean,
    showPlayerAccessory: Boolean,
    onAccessoryClick: () -> Unit,
    onAccessoryLyricsClick: (() -> Unit)?,
    onAccessoryQueueClick: (() -> Unit)?,
    searchModeActive: Boolean,
    navSearch: NavSearchState,
    modifier: Modifier,
) {
    // System back while search-expanded/search-inline (keyboard not active yet,
    // that state has its own BackHandler in NavBarSearchInputBar) -> same
    // animate-then-navigate exit as the bar's own back arrow / icon tap.
    BackHandler(enabled = searchModeActive, onBack = navSearch.onExit)

    // Last measured width of the normal (all-tabs) expanded row, held across
    // the switch into search mode so the bar doesn't visibly widen — search
    // mode's row targets this same width instead of filling all available space.
    var expandedContentWidthPx by remember { mutableStateOf<Int?>(null) }

    val glassConfig = LocalGlassEffectConfig.current
    val useGlass = glassConfig.isEnabledFor(GlassComponent.NAV_BAR) && isGlassAllowed()
    val appleMusicUi = LocalAppleMusicUi.current

    val backgroundColor = when {
        useGlass -> Color.Transparent
        pureBlack -> Color.Black
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    // Selected nav item = the liquid-glass text color; every other item = white.
    val selectedContentColor = glassConfig.textColor
    val unselectedContentColor = Color.White

    val tabBarContentModifier = if (useGlass) {
        Modifier.liquidGlass(
            config = glassConfig,
            shape = NavBarShape,
            highlightAlpha = 0.3f,
            // The bar's own surface is the largest of its glass layers, and its
            // bounds animate across the whole inline/expanded/search transition —
            // freezing its capture for those frames is most of the win.
            frozen = LocalTabBarBackdropFrozen.current,
        )
    } else {
        Modifier
    }

    val searchScreen = navigationItems.firstOrNull { it == Screens.Search }
    val tabScreens = remember(navigationItems) { navigationItems.filter { it != Screens.Search } }

    // Puck/indicator: sticky, so a non-tab destination (a drilled-in detail,
    // settings sub-page) holds the last tab instead of snapping the puck to Home.
    // Matched against tabScreens (not navigationItems) — Search is one of the
    // navigationItems too, so matching against the full list would let
    // search_input match itself instead of falling back to the last real tab,
    // leaving the search-expanded row's shrunk icon with nothing to show.
    val selectedTabKey = rememberStickySelectedRoute(currentRoute, tabScreens)

    val accessoryContentColor = when {
        useGlass -> glassConfig.textColor
        pureBlack -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }
    val inlineAccessory: (@Composable SharedTransitionScope.(Modifier, AnimatedVisibilityScope) -> Unit)? =
        if (showPlayerAccessory) {
            { accessoryModifier, _ ->
                FloatingMiniPlayer(
                    isInline = true,
                    contentColor = accessoryContentColor,
                    onClick = onAccessoryClick,
                    modifier = accessoryModifier.then(tabBarContentModifier),
                )
            }
        } else {
            null
        }
    val expandedAccessory: (@Composable SharedTransitionScope.(Modifier, AnimatedVisibilityScope) -> Unit)? =
        if (showPlayerAccessory) {
            { accessoryModifier, _ ->
                FloatingMiniPlayer(
                    isInline = false,
                    contentColor = accessoryContentColor,
                    onClick = onAccessoryClick,
                    onLyricsClick = onAccessoryLyricsClick,
                    onQueueClick = onAccessoryQueueClick,
                    modifier = accessoryModifier.fillMaxWidth().then(tabBarContentModifier),
                )
            }
        } else {
            null
        }

    FloatingTabBar(
        selectedTabKey = selectedTabKey,
        scrollConnection = scrollConnection,
        modifier = modifier,
        tabBarContentModifier = tabBarContentModifier,
        inlineAccessory = inlineAccessory,
        expandedAccessory = expandedAccessory,
        colors = FloatingTabBarDefaults.colors(
            backgroundColor = backgroundColor,
            accessoryBackgroundColor = backgroundColor,
        ),
        sizes = FloatingTabBarDefaults.sizes(
            tabBarContentPadding = PaddingValues(vertical = 4.dp, horizontal = 4.dp),
            tabExpandedContentPadding = PaddingValues(vertical = 4.dp, horizontal = 6.dp),
            tabInlineContentPadding = PaddingValues(8.dp),
        ),
        // The selection puck's lens/accent-tint effects only make sense when the
        // bar itself is sampling the app backdrop through liquid glass.
        backdrop = if (useGlass) LocalAppBackdrop.current else null,
        accentColor = selectedContentColor,
        searchMode = searchModeActive,
        searchBarContent = if (searchModeActive) {
            { contentModifier ->
                SearchBarPlaceholder(
                    state = navSearch,
                    contentColor = accessoryContentColor,
                    modifier = contentModifier,
                )
            }
        } else {
            null
        },
        expandedContentWidthPx = expandedContentWidthPx,
        onExpandedWidthChanged = { expandedContentWidthPx = it },
        // The tab content lambdas are captured once per contentKey, so anything they
        // close over (selection, colors) must be part of the key to avoid stale UI.
        contentKey = listOf(
            selectedTabKey,
            currentRoute,
            navigationItems,
            selectedContentColor,
            unselectedContentColor,
            searchModeActive,
            navSearch.searchSource,
        ),
    ) {
        tabScreens.forEach { screen ->
            val isSelected = screen.route == selectedTabKey
            tab(
                key = screen.route,
                title = {
                    Text(
                        text = stringResource(screen.titleId),
                        color = if (isSelected) selectedContentColor else unselectedContentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 10.sp,
                    )
                },
                icon = {
                    Icon(
                        painter = painterResource(
                            if (isSelected) screen.iconActive(appleMusicUi) else screen.iconInactive(appleMusicUi)
                        ),
                        contentDescription = stringResource(screen.titleId),
                        tint = if (isSelected) selectedContentColor else unselectedContentColor,
                        modifier = Modifier.size(30.dp),
                    )
                },
                onClick = {
                    // In search mode this is the shrunk current-screen icon —
                    // tapping it exits (animate back, then navigate) rather than
                    // re-navigating to itself.
                    if (searchModeActive) {
                        navSearch.onExit()
                    } else {
                        onItemClick(screen, isRouteSelected(currentRoute, screen.route, navigationItems))
                    }
                },
            )
        }

        if (searchScreen != null) {
            val screen = searchScreen
            val isSelected = screen.route == selectedTabKey
            standaloneTab(
                key = screen.route,
                title = {
                    Text(
                        text = stringResource(screen.titleId),
                        color = if (isSelected) selectedContentColor else unselectedContentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 10.sp,
                    )
                },
                icon = {
                    Icon(
                        painter = painterResource(
                            if (isSelected) screen.iconActive(appleMusicUi) else screen.iconInactive(appleMusicUi)
                        ),
                        contentDescription = stringResource(screen.titleId),
                        tint = if (isSelected) selectedContentColor else unselectedContentColor,
                        modifier = Modifier.size(30.dp),
                    )
                },
                onClick = {
                    if (searchModeActive) {
                        navSearch.onTapBar()
                    } else {
                        navSearch.onTapSearchIcon()
                    }
                },
            )
        }
    }
}

/**
 * Pre-keyboard search bar content — the wide slot [FloatingTabBar] expands the
 * search circle into once [searchMode] is on. Unfocused, placeholder-only; a tap
 * anywhere but the leading back arrow / trailing source toggle requests focus via
 * [NavSearchState.onTapBar], which flips [NavSearchState.keyboardActive] and swaps
 * the whole nav bar for [NavBarSearchInputBar]. Same back arrow / placeholder /
 * source-toggle layout as that keyboard-active pill, just unfocused — reads as
 * one continuous bar rather than a different-looking intermediate step.
 */
@Composable
private fun SearchBarPlaceholder(
    state: NavSearchState,
    contentColor: Color,
    modifier: Modifier,
) {
    // Same finger-tracking glow as the keyboard-active pill (NavBarSearchInputBar)
    // and the normal search circle (ExpandedStandaloneTab/InlineStandaloneTab).
    val glowScope = rememberCoroutineScope()
    val glow = remember(glowScope) { InteractiveHighlight(animationScope = glowScope) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .then(glow.gestureModifier)
            .then(glow.modifier)
            .bounceClick(onClick = state.onTapBar)
            .padding(start = 4.dp, end = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .bounceClick { state.onExit() }
                .padding(8.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.arrow_back),
                contentDescription = stringResource(R.string.dismiss),
                tint = contentColor,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(Modifier.weight(1f)) {
            DynamicSearchPlaceholder(
                searchSource = state.searchSource,
                style = TextStyle(color = contentColor.copy(alpha = 0.6f), fontSize = 15.sp),
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .bounceClick { state.onToggleSource() }
                .padding(9.dp)
        ) {
            Icon(
                painter = painterResource(
                    when (state.searchSource) {
                        SearchSource.LOCAL -> R.drawable.library_music
                        SearchSource.ONLINE -> R.drawable.globe_search
                    }
                ),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * Keyboard-active search state — replaces the whole nav bar (mini player and
 * current-screen icon included) while focused, since the keyboard covers that
 * screen space anyway. Docked above the keyboard via [Modifier.imePadding].
 * Relocated from the old [com.BeatWave.music.ui.screens.search.SearchScreen]
 * bottomBar pill — same markup, now driven by hoisted [NavSearchState].
 */
@Composable
fun NavBarSearchInputBar(
    state: NavSearchState,
    pureBlack: Boolean,
    modifier: Modifier,
) {
    val glassConfig = LocalGlassEffectConfig.current
    val useGlass = glassConfig.isEnabledFor(GlassComponent.NAV_BAR) && isGlassAllowed()
    val onTint = if (pureBlack || useGlass) Color.White else MaterialTheme.colorScheme.onSurface
    val pillShape = ContinuousRoundedRectangle(percent = 50)
    val coroutineScope = rememberCoroutineScope()
    // Finger-tracking glow, same as the nav bar puck's InteractiveHighlight: a
    // soft radial light follows the touch point across the glass pill.
    val pillGlow = remember(coroutineScope) { InteractiveHighlight(animationScope = coroutineScope) }

    // Keyboard's own back press just closes the keyboard (back to search-expanded)
    // rather than exiting search entirely — the visible back arrow does that.
    BackHandler(onBack = state.onCloseKeyboard)

    // Let the mini player/icon finish sliding down and out (the AnimatedContent
    // transition in AppFloatingNavBar) before pulling the keyboard up — opening
    // it immediately made the two animations fight for the same screen space.
    LaunchedEffect(Unit) {
        delay(KeyboardOpenDelayMs)
        state.focusRequester.requestFocus()
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .imePadding()
            .navigationBarsPadding()
            // Tight above the keyboard (top-only breathing room) rather than
            // padded on both sides — it should read as docked to the keyboard,
            // not floating with a gap above it.
            .padding(horizontal = 16.dp)
            .padding(top = 6.dp, bottom = 2.dp)
            .height(NavBarSearchBarHeight)
            .clip(pillShape)
            .then(
                if (useGlass) {
                    Modifier.liquidGlass(
                        config = glassConfig.copy(surfaceOpacity = 0.12f),
                        shape = pillShape,
                        highlightAlpha = 0.3f,
                    )
                } else {
                    Modifier.background(onTint.copy(alpha = 0.15f))
                }
            )
            .then(pillGlow.gestureModifier)
            .then(pillGlow.modifier)
            .padding(horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .bounceClick { state.onExit() }
                .padding(12.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.arrow_back),
                contentDescription = stringResource(R.string.dismiss),
                tint = onTint,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            if (state.query.text.isEmpty()) {
                DynamicSearchPlaceholder(
                    searchSource = state.searchSource,
                    style = TextStyle(color = onTint.copy(alpha = 0.6f), fontSize = 16.sp),
                )
            }
            BasicTextField(
                value = state.query,
                onValueChange = state.onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = onTint, fontSize = 16.sp),
                cursorBrush = SolidColor(onTint),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                // Some IMEs render the requested Search action as Done/Go instead
                // (or don't reliably report it at all) — handle every plausible
                // submit action so the button always does something.
                keyboardActions = KeyboardActions(
                    onSearch = { state.onSubmit(state.query.text) },
                    onDone = { state.onSubmit(state.query.text) },
                    onGo = { state.onSubmit(state.query.text) },
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(state.focusRequester)
                    // Some OEM keyboards (Samsung's included) don't reliably report
                    // the IME search action through KeyboardActions.onSearch — catch
                    // the raw Enter key too so submit isn't only reachable by tapping
                    // a suggestion.
                    .onKeyEvent {
                        if (it.key == Key.Enter) {
                            state.onSubmit(state.query.text)
                            true
                        } else {
                            false
                        }
                    }
            )
        }
        if (state.query.text.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .bounceClick { state.onQueryChange(TextFieldValue("")) }
                    .padding(12.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = null,
                    tint = onTint,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .bounceClick { state.onToggleSource() }
                .padding(12.dp)
        ) {
            Icon(
                painter = painterResource(
                    when (state.searchSource) {
                        SearchSource.LOCAL -> R.drawable.library_music
                        SearchSource.ONLINE -> R.drawable.globe_search
                    }
                ),
                contentDescription = null,
                tint = onTint,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
