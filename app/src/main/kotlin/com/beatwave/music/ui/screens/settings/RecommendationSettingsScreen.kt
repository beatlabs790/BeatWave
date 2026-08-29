package com.beatwave.music.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import com.beatwave.music.LocalPlayerAwareWindowInsets
import com.beatwave.music.R
import com.beatwave.music.constants.RecommendationEngineStyle
import com.beatwave.music.constants.RecommendationEngineStyleKey
import com.beatwave.music.ui.component.EnumListPreference
import com.beatwave.music.ui.component.PreferenceEntry
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import com.beatwave.music.utils.rememberEnumPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationSettingsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (recommendationEngineStyle, setRecommendationEngineStyle) = rememberEnumPreference(
        RecommendationEngineStyleKey,
        RecommendationEngineStyle.HYBRID
    )

    LazyColumn(
        Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal))
    ) {
        item {
            Column {
                EnumListPreference(
                    title = { Text("Recommendation Engine / Style") },
                    icon = { Icon(painterResource(R.drawable.sparks), null) },
                    selectedValue = recommendationEngineStyle,
                    onValueSelected = { setRecommendationEngineStyle(it) },
                    valueText = {
                        when (it) {
                            RecommendationEngineStyle.SPOTIFY -> "Spotify Style"
                            RecommendationEngineStyle.YOUTUBE_MUSIC -> "YouTube Music Style"
                            RecommendationEngineStyle.HYBRID -> "Beatwave Hybrid"
                        }
                    }
                )
                PreferenceEntry(
                    title = { Text("About Recommendation Styles") },
                    description = "Spotify Style: Acoustic similarity, deep genre matching.\nYouTube Music Style: Trending music, extended radio.\nBeatwave Hybrid: A balanced blend of both.",
                    onClick = {}
                )
            }
        }
    }

    TopAppBar(
        title = { Text("Recommendations") },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null
                )
            }
        },
        windowInsets = appTopBarWindowInsets(),
        scrollBehavior = scrollBehavior
    )
}
