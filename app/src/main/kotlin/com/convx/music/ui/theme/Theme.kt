/**
 * BeatWave Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convx.music.ui.theme

import android.graphics.Bitmap
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.palette.graphics.Palette
import com.convx.music.constants.AppFont
import com.convx.music.constants.SelectedFontKey
import com.convx.music.utils.rememberPreference
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.materialkolor.score.Score

val DefaultThemeColor = AppleTokens.AccentRed

/**
 * The user's chosen accent color, provided app-wide. ONLY the nav bar uses this
 * as an accent; the rest of the UI is contrast-based (white/black by luminance).
 * Kept separate from [ColorScheme.primary], which is neutralized to white in the
 * dark Apple theme so generic M3 controls don't render red.
 */
val LocalAccentColor = androidx.compose.runtime.compositionLocalOf { DefaultThemeColor }

/**
 * The accent tuned for readable body text: same hue, lightened on dark
 * backgrounds and darkened on light ones, but never pushed to near-white or
 * near-black — so it reads as "coloured text," legible on either theme.
 * Consume via [LocalAccentTextColor].
 */
fun accentTextColor(accent: Color, dark: Boolean): Color {
    val blended = if (dark) {
        androidx.compose.ui.graphics.lerp(accent, Color.White, 0.35f)
    } else {
        androidx.compose.ui.graphics.lerp(accent, Color.Black, 0.45f)
    }
    // A very dark or very pale accent can survive the blend and still be
    // unreadable against the surface, so clamp lightness rather than trust it.
    val hsl = FloatArray(3)
    androidx.core.graphics.ColorUtils.colorToHSL(blended.toArgb(), hsl)
    hsl[2] = if (dark) hsl[2].coerceAtLeast(0.72f) else hsl[2].coerceAtMost(0.32f)
    return Color(androidx.core.graphics.ColorUtils.HSLToColor(hsl))
}

/** Accent-contrast text color, provided app-wide from the current accent + theme. */
val LocalAccentTextColor = androidx.compose.runtime.compositionLocalOf { DefaultThemeColor }

@Composable
fun beatwaveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = true,
    themeColor: Color = DefaultThemeColor,
    content: @Composable () -> Unit,
) {
    val baseColorScheme = rememberDynamicColorScheme(
        seedColor = themeColor,
        isDark = darkTheme,
        specVersion = ColorSpec.SpecVersion.SPEC_2025,
        style = PaletteStyle.TonalSpot,
    )

    val colorScheme = remember(baseColorScheme, pureBlack, darkTheme, themeColor) {
        val withApple = if (darkTheme) baseColorScheme.appleSurfaces() else baseColorScheme
        val withBlack = if (darkTheme && pureBlack) withApple.pureBlack(true) else withApple
        withBlack.accentText(themeColor, darkTheme)
    }

    val (selectedFont) = rememberPreference(SelectedFontKey, defaultValue = AppFont.SYSTEM.value)
    val customFont = rememberCustomFontFamily()

    val appFont = remember(selectedFont, customFont) {
        when (AppFont.fromValue(selectedFont)) {
            AppFont.SYSTEM -> customFont ?: FontFamily.Default
            AppFont.GOOGLE_SANS -> GoogleSansFontFamily
            AppFont.SANS_FLEX -> SansFlexFontFamily
            AppFont.OUTFIT -> OutfitFontFamily
            AppFont.PLUS_JAKARTA_SANS -> PlusJakartaSansFontFamily
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalAccentColor provides themeColor,
        LocalAccentTextColor provides accentTextColor(themeColor, darkTheme),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = remember(appFont) { AppTypography(appFont) },
            content = content
        )
    }
}

fun Bitmap.extractThemeColor(): Color {
    val colorsToPopulation = Palette.from(this)
        .maximumColorCount(8)
        .generate()
        .swatches
        .associate { it.rgb to it.population }
    val rankedColors = Score.score(colorsToPopulation)
    return Color(rankedColors.first())
}

/**
 * Grabs one frame from a canvas video URL and runs the same extraction as
 * [Bitmap.extractThemeColor] on it — canvas videos are often more colorful/
 * representative than the still cover art. Best-effort: HLS (.m3u8) frame
 * extraction isn't reliable on every device/Android version, so callers
 * should fall back to the static artwork on a null result. Blocking — call
 * from a background dispatcher.
 */
fun extractThemeColorFromVideoFrame(videoUrl: String): Color? {
    val retriever = android.media.MediaMetadataRetriever()
    return try {
        retriever.setDataSource(videoUrl, emptyMap())
        // A second or so in tends to avoid a black/fade-in first frame.
        val frame = retriever.getFrameAtTime(1_000_000L, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        frame?.extractThemeColor()
    } catch (e: Exception) {
        null
    } finally {
        retriever.release()
    }
}

fun Bitmap.extractGradientColors(): List<Color> {
    val extractedColors = Palette.from(this)
        .maximumColorCount(64)
        .generate()
        .swatches
        .associate { it.rgb to it.population }

    val orderedColors = Score.score(extractedColors, 2, 0xff4285f4.toInt(), true)
        .sortedByDescending { Color(it).luminance() }

    return if (orderedColors.size >= 2)
        listOf(Color(orderedColors[0]), Color(orderedColors[1]))
    else
        listOf(Color(0xFF595959), Color(0xFF0D0D0D))
}

/**
 * Apple-dark surface overrides applied unconditionally in dark mode.
 * pureBlack is applied AFTER this and wins for surface/background when enabled.
 */
fun ColorScheme.appleSurfaces() = copy(
    background = AppleTokens.Bg,
    surface = AppleTokens.Bg,
    surfaceContainerLowest = AppleTokens.Bg,
    surfaceContainerLow = AppleTokens.BgElevated,
    surfaceContainer = AppleTokens.Card,
    surfaceContainerHigh = AppleTokens.Card,
    surfaceContainerHighest = AppleTokens.CardSecondary,
    surfaceVariant = AppleTokens.CardSecondary,
    // primary/onPrimary set here are only a base — accentText runs afterwards and
    // gives them the contrast-clamped accent, so controls and link text carry the
    // theme colour rather than rendering flat white.
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = AppleTokens.CardSecondary,
    onPrimaryContainer = Color.White,
)

/**
 * Swaps Material's neutral text roles for accent-contrast ones, so every screen
 * picks it up from `MaterialTheme.colorScheme` without being edited.
 *
 * Body text keeps only a whisper of the accent — it has to stay readable over
 * long lists — while secondary text carries it plainly, which is what replaces
 * M3's washed-out grey. Tune the two lerp amounts to taste; they are the whole
 * calibration.
 */
fun ColorScheme.accentText(accent: Color, dark: Boolean): ColorScheme {
    val body = if (dark) {
        androidx.compose.ui.graphics.lerp(accent, Color.White, 0.86f)
    } else {
        androidx.compose.ui.graphics.lerp(accent, Color.Black, 0.86f)
    }
    val secondary = accentTextColor(accent, dark)
    return copy(
        onSurface = body,
        onBackground = body,
        onSurfaceVariant = secondary,
        onSecondaryContainer = secondary,
        onTertiaryContainer = secondary,
        onPrimaryContainer = secondary,
        onSecondary = secondary,
        // The subtitle role: the artist name under a song or album title reads
        // through this everywhere in the app.
        secondary = secondary,
        // `primary` is the app's second-largest text/icon role after
        // onSurfaceVariant — links, selected states, section actions. appleSurfaces
        // had pinned it to flat white in dark mode, which is most of what still
        // read as untinted Material. It doubles as a fill (buttons, sliders), so
        // onPrimary flips to the opposite extreme to keep those legible.
        primary = secondary,
        onPrimary = if (dark) Color.Black else Color.White,
    )
}

fun ColorScheme.pureBlack(apply: Boolean) =
    if (apply) copy(
        surface = Color.Black,
        background = Color.Black
    ) else this

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}
