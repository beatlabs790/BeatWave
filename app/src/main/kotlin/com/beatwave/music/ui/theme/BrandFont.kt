/**
 * BeatWave Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.beatwave.music.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.beatwave.music.R
import com.beatwave.music.constants.BrandFontEnabledKey
import com.beatwave.music.utils.rememberPreference

/** The product wordmark. */
const val BrandName = "BeatWave"

/** Chillax — display face for artist titles. */
val ChillaxFontFamily = FontFamily(
    Font(R.font.chillax_light, FontWeight.Light),
    Font(R.font.chillax_regular, FontWeight.Normal),
    Font(R.font.chillax_medium, FontWeight.Medium),
    Font(R.font.chillax_semibold, FontWeight.SemiBold),
    Font(R.font.chillax_bold, FontWeight.Bold),
)

/**
 * Outfit font family for the BeatWave brand wordmark and display headers.
 * Falls back to null when brand font preference is disabled.
 */
@Composable
fun rememberBrandFontFamily(): FontFamily? {
    val enabled by rememberPreference(BrandFontEnabledKey, defaultValue = true)
    return if (enabled) OutfitFontFamily else null
}
