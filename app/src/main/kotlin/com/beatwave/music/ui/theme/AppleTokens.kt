package com.beatwave.music.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import com.beatwave.music.ui.component.shapes.ContinuousRoundedRectangle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Apple Music design tokens — the single source of truth for colors, shapes, and
 * adaptive contrast helpers. Every screen references these instead of hardcoding values.
 */
object AppleTokens {
    // Accent
    val AccentRed = Color(0xFFFA2D48)

    // Surfaces — Apple-style soft dark greys, not pure black (pure black stays an
    // opt-in via the PureBlack setting for OLED). Monotonic elevation ladder.
    val Bg = Color(0xFF121212)
    val BgElevated = Color(0xFF1A1A1A)
    val Card = Color(0xFF1C1C1E)
    val CardSecondary = Color(0xFF2C2C2E)

    /**
     * Hairline rule on whatever surface the caller sits on. Derived from the
     * content colour rather than fixed to white: a constant white rule is
     * invisible on the light theme, and on an artwork-tinted screen it reads as
     * a foreign line instead of one belonging to the surface.
     */
    val divider: Color
        @Composable get() = LocalContentColor.current.copy(alpha = 0.12f)

    /**
     * Metadata text — artist names, subtitles, durations. One step down from
     * on-surface, never full white: the hierarchy on Home is carried by this
     * contrast step rather than by size, since title and subtitle share a size.
     */
    val Metadata = Color(0xFF8E8E93)

    // Spacing — the whole scale. Screens reference these, never a dp literal.

    /** Horizontal screen gutter. The only gutter. */
    val Gutter = 20.dp
    /** Gap between siblings in a grid or list. */
    val ItemGap = 16.dp
    /** Gap between a section and the next section's header. */
    val SectionGap = 24.dp
    /** Gap between stacked text lines inside one item. */
    val TextGap = 2.dp

    // Type scale — Home's headers and tile text. Sizes are fixed rather than
    // taken from the Material scale: the design pins them to specific values and
    // the tile grid's alignment depends on title and subtitle matching exactly.

    // Each size carries its own line height. Compose otherwise derives leading from
    // the font's own metrics, which lands taller than the design at every step and
    // shows up as sections that drift out of alignment the further down you scroll.

    /** Screen title ("Listen Now"). */
    val TitleLarge = 34.sp
    val TitleLargeLineHeight = 41.sp
    /** Section header ("Recently Played"). */
    val SectionHeader = 22.sp
    val SectionHeaderLineHeight = 28.sp
    /** Tile title and list-row primary text. */
    val ItemTitle = 15.sp
    val ItemTitleLineHeight = 20.sp
    /** Tile subtitle, row metadata. */
    val ItemSubtitle = 13.sp
    val ItemSubtitleLineHeight = 18.sp
    /** Speed dial captions. */
    val Caption = 12.sp
    val CaptionLineHeight = 16.sp

    // Shapes — four corners, no more. Artwork is the only rounded thing on a
    // content tile; a card corner means the object is genuinely a card.
    // Every one of them is drawn with continuous (squircle) curvature — see
    // [AppShapes] — so nothing in the app mixes circular and continuous corners.
    val Artwork = 12.dp
    val Control = 12.dp
    val CardCorner = 22.dp
    val CardCornerLarge = 28.dp

    // Adaptive contrast helpers

    /**
     * Primary text/icon color for content sitting on [bg] — typically a screen's
     * own hero tint, extracted from its artwork.
     *
     * Keeps [bg]'s hue and pushes lightness to a readable extreme, rather than
     * dropping to flat white/near-black. The text then belongs to the screen's
     * color instead of being pasted on top of it. Saturation is capped low: this
     * is body copy, so it should read as white/black that happens to be warm or
     * cool, not as colored text.
     */
    fun onColor(bg: Color): Color = bg.shiftedForContrast(saturation = 0.16f, lightness = 0.96f to 0.10f)

    /**
     * Secondary text on [bg]: same hue, but the tint is allowed to show. Use for
     * subtitles, counts and metadata — the role that reads as washed-out grey
     * when it comes from Material's neutral palette.
     */
    fun onColorSecondary(bg: Color): Color =
        bg.shiftedForContrast(saturation = 0.42f, lightness = 0.76f to 0.34f)

    /**
     * Headings on [bg] — artist names, section titles, the top-bar title. Carries
     * the most tint of the three: a heading is short and large, so it can be
     * clearly the screen's colour without costing legibility the way body copy
     * would.
     */
    fun onColorHeading(bg: Color): Color =
        bg.shiftedForContrast(saturation = 0.55f, lightness = 0.88f to 0.18f)

    /**
     * @param lightness (on-dark, on-light) target lightness.
     */
    private fun Color.shiftedForContrast(saturation: Float, lightness: Pair<Float, Float>): Color {
        val hsl = FloatArray(3)
        androidx.core.graphics.ColorUtils.colorToHSL(toArgb(), hsl)
        val onDark = luminance() <= 0.5f
        val target = if (onDark) lightness.first else lightness.second
        // No artwork (the tint is still black) or a monochrome cover means there
        // is no hue to carry. For primary text, nudging lightness off the extreme
        // would only mute it to a grey, so hand back plain white/near-black.
        // Secondary text is meant to sit back, so it keeps its target.
        if (hsl[1] < 0.08f) {
            hsl[1] = 0f
            hsl[2] = if (target > 0.9f) 1f else if (target < 0.12f) 0.04f else target
        } else {
            hsl[1] = minOf(hsl[1], saturation)
            hsl[2] = target
        }
        return Color(androidx.core.graphics.ColorUtils.HSLToColor(hsl))
    }
}

/**
 * The M3 shape scale, bound to [AppleTokens]' four corners so that anything
 * reaching for `MaterialTheme.shapes` lands on the same vocabulary as code that
 * names the token directly.
 */
val AppShapes = Shapes(
    extraSmall = ContinuousRoundedRectangle(4.dp),
    small = ContinuousRoundedRectangle(AppleTokens.Artwork),
    medium = ContinuousRoundedRectangle(AppleTokens.Control),
    large = ContinuousRoundedRectangle(AppleTokens.CardCorner),
    extraLarge = ContinuousRoundedRectangle(AppleTokens.CardCornerLarge),
)
