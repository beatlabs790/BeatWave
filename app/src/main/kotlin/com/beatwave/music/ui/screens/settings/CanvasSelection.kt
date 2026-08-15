/**
 * BeatWave Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.beatwave.music.ui.screens.settings

import com.beatwave.music.ui.utils.appTopBarWindowInsets
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.animation.animateColorAsState
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import com.beatwave.music.ui.utils.bounceClick
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import com.beatwave.music.ui.utils.combinedBounceClick
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.Arrangement
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.Box
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.Column
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.Row
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.Spacer
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.height
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.only
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.padding
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.rememberScrollState
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.shape.RoundedCornerShape
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.verticalScroll
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.Card
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.CardDefaults
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.Icon
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.MaterialTheme
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.RadioButton
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.Scaffold
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import com.beatwave.music.ui.component.GlassSwitchCompat as Switch
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.Text
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.TopAppBar
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.TopAppBarDefaults
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.TopAppBarScrollBehavior
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.graphics.Color
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.runtime.Composable
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.runtime.getValue
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.Alignment
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.Modifier
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.res.painterResource
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.res.stringResource
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.unit.dp
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import androidx.navigation.NavController
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import com.beatwave.music.LocalPlayerAwareWindowInsets
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import com.beatwave.music.R
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import com.beatwave.music.constants.CanvasSource
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import com.beatwave.music.constants.CanvasSourceKey
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import com.beatwave.music.constants.CanvasThumbnailAnimationKey
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import com.beatwave.music.ui.component.ExpressiveIconButton
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import com.beatwave.music.ui.component.IconButton
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import com.beatwave.music.ui.component.Material3SettingsGroup
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import com.beatwave.music.ui.component.Material3SettingsItem
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import com.beatwave.music.ui.component.ModernSwitch
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import com.beatwave.music.ui.utils.backToMain
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import com.beatwave.music.utils.rememberEnumPreference
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import com.beatwave.music.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasSelection(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (canvasThumbnailAnimation, onCanvasThumbnailAnimationChange) = rememberPreference(
        CanvasThumbnailAnimationKey,
        defaultValue = true
    )
    val (canvasSource, onCanvasSourceChange) = rememberEnumPreference(
        CanvasSourceKey,
        defaultValue = CanvasSource.AUTO
    )

    Column(
        Modifier
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Top
                )
            )
        )

        // Description text
        Text(
            text = stringResource(R.string.vivimusic_canvas_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp, top = 16.dp)
        )

        // Large capsule banner for main toggle
        val containerColor by animateColorAsState(
            targetValue = if (canvasThumbnailAnimation) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            },
            label = "containerColor"
        )

        val contentColor = if (canvasThumbnailAnimation) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

        Card(
            onClick = { onCanvasThumbnailAnimationChange(!canvasThumbnailAnimation) },
            shape = RoundedCornerShape(50),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.use_canvas),
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor
                )
                ModernSwitch(
                    checked = canvasThumbnailAnimation,
                    onCheckedChange = onCanvasThumbnailAnimationChange
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Options settings group
        Material3SettingsGroup(
            title = stringResource(R.string.canvas_source),
            items = listOf(
                Material3SettingsItem(
                    leadingContent = {
                        RadioButton(
                            selected = canvasSource == CanvasSource.AUTO,
                            onClick = null,
                            enabled = canvasThumbnailAnimation
                        )
                    },
                    title = { Text(stringResource(R.string.canvas_source_auto)) },
                    description = { Text(stringResource(R.string.canvas_source_auto_desc)) },
                    enabled = canvasThumbnailAnimation,
                    onClick = { onCanvasSourceChange(CanvasSource.AUTO) }
                ),
                Material3SettingsItem(
                    leadingContent = {
                        RadioButton(
                            selected = canvasSource == CanvasSource.ECHO_MUSIC,
                            onClick = null,
                            enabled = canvasThumbnailAnimation
                        )
                    },
                    title = { Text(stringResource(R.string.canvas_source_echo_music)) },
                    description = { Text(stringResource(R.string.canvas_source_echo_music_desc)) },
                    enabled = canvasThumbnailAnimation,
                    onClick = { onCanvasSourceChange(CanvasSource.ECHO_MUSIC) }
                ),
                Material3SettingsItem(
                    leadingContent = {
                        RadioButton(
                            selected = canvasSource == CanvasSource.APPLE_MUSIC,
                            onClick = null,
                            enabled = canvasThumbnailAnimation
                        )
                    },
                    title = { Text(stringResource(R.string.canvas_source_apple_music)) },
                    description = { Text(stringResource(R.string.canvas_source_apple_music_desc)) },
                    enabled = canvasThumbnailAnimation,
                    onClick = { onCanvasSourceChange(CanvasSource.APPLE_MUSIC) }
                ),
                Material3SettingsItem(
                    leadingContent = {
                        RadioButton(
                            selected = canvasSource == CanvasSource.VIVIMUSIC,
                            onClick = null,
                            enabled = canvasThumbnailAnimation
                        )
                    },
                    title = { Text(stringResource(R.string.canvas_source_vivimusic)) },
                    description = { Text(stringResource(R.string.canvas_source_vivimusic_desc)) },
                    enabled = canvasThumbnailAnimation,
                    onClick = { onCanvasSourceChange(CanvasSource.VIVIMUSIC) }
                ),
                Material3SettingsItem(
                    leadingContent = {
                        RadioButton(
                            selected = canvasSource == CanvasSource.TIDAL,
                            onClick = null,
                            enabled = canvasThumbnailAnimation
                        )
                    },
                    title = { Text(stringResource(R.string.canvas_source_tidal)) },
                    description = { Text(stringResource(R.string.canvas_source_tidal_desc)) },
                    enabled = canvasThumbnailAnimation,
                    onClick = { onCanvasSourceChange(CanvasSource.TIDAL) }
                )
            )
        )
        Spacer(modifier = Modifier.height(36.dp))
    }

    TopAppBar(
            windowInsets = appTopBarWindowInsets(),
        title = { Text(stringResource(R.string.vivimusic_canvas)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        }
    )
}
