/**
 * BeatWave Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.beatwave.music.ui.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.beatwave.music.R

/**
 * A bottom-bar / side-bar destination.
 *
 * One icon per destination per UI style — deliberately not a selected/unselected
 * pair. iOS ships outline/filled pairs because its tab bar has no selection
 * indicator; this bar has both a glass puck and an accent content colour, so a
 * filled variant would be a third signal saying the same thing. Two artworks per
 * tab also have to be optically aligned against each other or the icon visibly
 * shifts on selection, which is exactly the cheap-looking wobble the pair would
 * have bought us.
 */
@Immutable
sealed class Screens(
    @StringRes val titleId: Int,
    val route: String,
    @DrawableRes val icon: Int,
    // SF Symbols-style lookalike used when the Apple Music UI style is active.
    // Defaults to the classic icon, so a destination only names this when it
    // actually has a distinct iOS drawing.
    @DrawableRes val iosIcon: Int = icon,
) {
    /** The icon to draw for the current UI style. */
    fun icon(appleMusicUi: Boolean): Int = if (appleMusicUi) iosIcon else icon

    object Home : Screens(
        titleId = R.string.home,
        route = "home",
        icon = R.drawable.accord_home,
    )

    object Search : Screens(
        titleId = R.string.search,
        route = "search_input",
        icon = R.drawable.search,
        iosIcon = R.drawable.cosmos_search,
    )

    object ListenTogether : Screens(
        titleId = R.string.together,
        route = "listen_together",
        icon = R.drawable.accord_groups,
    )

    object Library : Screens(
        titleId = R.string.filter_library,
        route = "library",
        icon = R.drawable.accord_library,
    )

    object Settings : Screens(
        titleId = R.string.settings,
        route = "settings",
        icon = R.drawable.settings,
    )

    companion object {
        val MainScreens = listOf(Home, Search, ListenTogether, Library)
    }
}
