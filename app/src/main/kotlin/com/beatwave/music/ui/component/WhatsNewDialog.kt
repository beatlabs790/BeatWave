/**
 * BeatWave Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.beatwave.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.beatwave.music.BuildConfig
import com.beatwave.music.R
import com.beatwave.music.constants.LastSeenChangelogVersionKey
import com.beatwave.music.ui.theme.AppleTokens
import com.beatwave.music.utils.rememberPreference

/**
 * Automatically prompts the user with a "What's New" changelog summary
 * when they update to a new version of BeatWave.
 */
@Composable
fun WhatsNewPromptHost(
    navController: NavController? = null
) {
    var lastSeenVersion by rememberPreference(LastSeenChangelogVersionKey, "")
    val currentVersion = BuildConfig.VERSION_NAME

    var visible by remember(lastSeenVersion, currentVersion) {
        mutableStateOf(lastSeenVersion.isNotBlank() && lastSeenVersion != currentVersion)
    }

    // If it's a brand new install (empty string), stamp it so we don't spam on initial setup
    if (lastSeenVersion.isBlank()) {
        lastSeenVersion = currentVersion
        return
    }

    if (!visible) return

    fun dismiss() {
        visible = false
        lastSeenVersion = currentVersion
    }

    AlertDialog(
        onDismissRequest = ::dismiss,
        shape = RoundedCornerShape(AppleTokens.CardCornerLarge),
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.update),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "What's New in BeatWave",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Version $currentVersion",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ChangelogHighlightItem(
                    emoji = "🎵",
                    title = "Spotify Recommendation Engine",
                    description = "Live algorithmic discovery feeds bringing fresh, personalized music without requiring any logins or API keys."
                )

                ChangelogHighlightItem(
                    emoji = "⚡",
                    title = "Instant Song Cards",
                    description = "1-tap direct playback for all Daily Discover cards and Quick Picks right from your home screen."
                )

                ChangelogHighlightItem(
                    emoji = "🎨",
                    title = "Vibrant Moods & Genres",
                    description = "Unified dynamic gradient palette with rich contrast across Home and Explore sections."
                )

                ChangelogHighlightItem(
                    emoji = "💎",
                    title = "Liquid Glass UI Polish",
                    description = "Enhanced frosted glass cards, subtle refraction highlights, and smooth spring physics."
                )

                ChangelogHighlightItem(
                    emoji = "🚀",
                    title = "Performance Improvements",
                    description = "Smoother scrolling, optimized database queries, and faster app startup."
                )
            }
        },
        confirmButton = {
            Button(
                onClick = ::dismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Got it!")
            }
        },
        dismissButton = {
            if (navController != null) {
                TextButton(
                    onClick = {
                        dismiss()
                        navController.navigate("settings/changelog")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View Full Changelog")
                }
            }
        }
    )
}

@Composable
private fun ChangelogHighlightItem(
    emoji: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = emoji,
            fontSize = 20.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}
