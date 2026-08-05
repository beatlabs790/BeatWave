/**
 * BeatWave Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convx.music.ui.screens.settings

import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.animation.animateColorAsState
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.Arrangement
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.Column
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.Row
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.Spacer
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.height
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.only
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.padding
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.rememberScrollState
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.shape.RoundedCornerShape
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.verticalScroll
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.Card
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.CardDefaults
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.Icon
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.MaterialTheme
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.RadioButton
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.Text
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.TopAppBar
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.TopAppBarScrollBehavior
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.runtime.Composable
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.runtime.getValue
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
import com.convx.music.constants.EnableSaavnStreamingKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.SaavnAudioQuality
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.SaavnAudioQualityKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.ui.component.IconButton
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.ui.component.Material3SettingsGroup
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.ui.component.Material3SettingsItem
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.ui.component.ModernSwitch
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.ui.utils.backToMain
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.utils.rememberEnumPreference
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JioSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (saavnEnabled, onSaavnEnabledChange) = rememberPreference(
        EnableSaavnStreamingKey,
        defaultValue = false
    )
    val (saavnQuality, onSaavnQualityChange) = rememberEnumPreference(
        SaavnAudioQualityKey,
        defaultValue = SaavnAudioQuality.QUALITY_320
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
            text = stringResource(R.string.enable_saavn_streaming_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp, top = 16.dp)
        )

        // Large capsule banner for main toggle
        val containerColor by animateColorAsState(
            targetValue = if (saavnEnabled) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            },
            label = "containerColor"
        )

        val contentColor = if (saavnEnabled) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

        Card(
            onClick = { onSaavnEnabledChange(!saavnEnabled) },
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
                    text = stringResource(R.string.enable_saavn_streaming),
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor
                )
                ModernSwitch(
                    checked = saavnEnabled,
                    onCheckedChange = onSaavnEnabledChange
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Options settings group
        Material3SettingsGroup(
            title = stringResource(R.string.saavn_audio_quality),
            items = listOf(
                Material3SettingsItem(
                    leadingContent = {
                        RadioButton(
                            selected = saavnQuality == SaavnAudioQuality.QUALITY_320,
                            onClick = null,
                            enabled = saavnEnabled
                        )
                    },
                    title = { Text(SaavnAudioQuality.QUALITY_320.toLabel()) },
                    enabled = saavnEnabled,
                    onClick = { onSaavnQualityChange(SaavnAudioQuality.QUALITY_320) }
                ),
                Material3SettingsItem(
                    leadingContent = {
                        RadioButton(
                            selected = saavnQuality == SaavnAudioQuality.QUALITY_160,
                            onClick = null,
                            enabled = saavnEnabled
                        )
                    },
                    title = { Text(SaavnAudioQuality.QUALITY_160.toLabel()) },
                    enabled = saavnEnabled,
                    onClick = { onSaavnQualityChange(SaavnAudioQuality.QUALITY_160) }
                ),
                Material3SettingsItem(
                    leadingContent = {
                        RadioButton(
                            selected = saavnQuality == SaavnAudioQuality.QUALITY_96,
                            onClick = null,
                            enabled = saavnEnabled
                        )
                    },
                    title = { Text(SaavnAudioQuality.QUALITY_96.toLabel()) },
                    enabled = saavnEnabled,
                    onClick = { onSaavnQualityChange(SaavnAudioQuality.QUALITY_96) }
                )
            )
        )

        Row(
            modifier = Modifier.padding(top = 24.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                painter = painterResource(R.drawable.info),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = stringResource(R.string.jiosaavn_beta_info),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(36.dp))
    }

    TopAppBar(
            windowInsets = appTopBarWindowInsets(),
        title = { Text(stringResource(R.string.jiosaavn_settings)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null
                )
            }
        }
    )
}
