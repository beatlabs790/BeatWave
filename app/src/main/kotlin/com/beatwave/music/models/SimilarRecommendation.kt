/**
 * BeatWave Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.beatwave.music.models

import androidx.compose.runtime.Immutable
import com.music.innertube.models.YTItem
import com.beatwave.music.db.entities.LocalItem

@Immutable
data class SimilarRecommendation(
    val title: LocalItem,
    val items: List<YTItem>,
)
