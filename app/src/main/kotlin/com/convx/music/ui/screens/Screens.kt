/**
 * BeatWave Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convx.music.ui.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.convx.music.R

@Immutable
sealed class Screens(
    @StringRes val titleId: Int,
    @DrawableRes val iconIdInactive: Int,
    @DrawableRes val iconIdActive: Int,
    val route: String,
    // SF Symbols-style lookalikes used when the Apple Music UI style is active.
    // Default to the classic icon so screens that don't define an iOS pair still work.
    @DrawableRes val iconIdInactiveIos: Int = iconIdInactive,
    @DrawableRes val iconIdActiveIos: Int = iconIdActive,
) {
    /** Resolves the icon pair to draw for the current UI style. */
    fun iconInactive(appleMusicUi: Boolean): Int = if (appleMusicUi) iconIdInactiveIos else iconIdInactive
    fun iconActive(appleMusicUi: Boolean): Int = if (appleMusicUi) iconIdActiveIos else iconIdActive

    object Home : Screens(
        titleId = R.string.home,
        iconIdInactive = R.drawable.accord_home,
        iconIdActive = R.drawable.accord_home,
        route = "home",
        iconIdInactiveIos = R.drawable.accord_home,
        iconIdActiveIos = R.drawable.accord_home,
    )

    object Search : Screens(
        titleId = R.string.search,
        iconIdInactive = R.drawable.search,
        iconIdActive = R.drawable.search,
        route = "search_input",
        iconIdInactiveIos = R.drawable.cosmos_search,
        iconIdActiveIos = R.drawable.cosmos_search,
    )

    object ListenTogether : Screens(
        titleId = R.string.together,
        iconIdInactive = R.drawable.accord_groups,
        iconIdActive = R.drawable.accord_groups,
        route = "listen_together",
        iconIdInactiveIos = R.drawable.accord_groups,
        iconIdActiveIos = R.drawable.accord_groups,
    )

    object Library : Screens(
        titleId = R.string.filter_library,
        iconIdInactive = R.drawable.accord_library,
        iconIdActive = R.drawable.accord_library,
        route = "library",
        iconIdInactiveIos = R.drawable.accord_library,
        iconIdActiveIos = R.drawable.accord_library,
    )

    object Settings : Screens(
        titleId = R.string.settings,
        iconIdInactive = R.drawable.settings,
        iconIdActive = R.drawable.settings,
        route = "settings",
    )

    companion object {
        val MainScreens = listOf(Home, Search, ListenTogether, Library)
    }
}
