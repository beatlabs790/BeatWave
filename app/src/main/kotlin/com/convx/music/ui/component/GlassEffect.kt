/**
 * BeatWave Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convx.music.ui.component

import android.app.ActivityManager
import android.os.Build
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.convx.music.ui.component.backdrop.Backdrop
import com.convx.music.ui.component.backdrop.drawBackdrop
import com.convx.music.ui.component.backdrop.effects.blur
import com.convx.music.ui.component.backdrop.effects.colorControls
import com.convx.music.ui.component.backdrop.effects.lens
import com.convx.music.ui.component.backdrop.highlight.Highlight
import com.convx.music.ui.component.backdrop.highlight.HighlightStyle
import com.convx.music.ui.component.backdrop.shadow.Shadow

/**
 * User-configurable parameters of the liquid glass effect, sourced from DataStore
 * preferences in [com.BeatWave.music.MainActivity] and distributed through
 * [LocalGlassEffectConfig].
 */
@Stable
data class GlassEffectConfig(
    val globalEnabled: Boolean = true,
    val vibrancy: Float = 1.2f,
    /** Blur in dp applied to glass pills. User requested 2dp for landscape fix. */
    val blurRadius: Float = 2f,
    /** 0..1, mapped to 0..[LENS_MAX_DP] dp of lens refraction height. 0.4 = 40%. */
    val lensHeight: Float = 0.4f,
    /** 0..1, mapped to 0..[LENS_MAX_DP] dp of lens refraction amount. 0.6 = 60%. */
    val lensAmount: Float = 0.6f,
    val chromaticAberration: Boolean = false,
    val depthEffect: Boolean = false,
    /** [Color.Unspecified] means adaptive: dark grey on dark. */
    val surfaceTintColor: Color = Color(0xFF1A1A1A),
    val surfaceOpacity: Float = 0.5f,
    val textColor: Color = Color.White,
    val playerEnabled: Boolean = true,
    val miniPlayerEnabled: Boolean = true,
    val navBarEnabled: Boolean = true,
    /** Tablet side panel — split from [navBarEnabled] so it can differ from the
     *  phone bottom bar's glass setting instead of always mirroring it. */
    val sidePanelEnabled: Boolean = true,
    /** Side panel gets its own effect tuning (unlike the other components,
     *  which all share [vibrancy]/[blurRadius]/[lensHeight]/[lensAmount]) —
     *  defaults match those shared values so it looks identical until
     *  explicitly customized. */
    val sidePanelVibrancy: Float = 1.2f,
    val sidePanelBlurRadius: Float = 2f,
    val sidePanelLensHeight: Float = 0.4f,
    val sidePanelLensAmount: Float = 0.6f,
    val sidePanelColor: Color = Color.Unspecified,
    val sidePanelSurfaceOpacity: Float = 0.5f,
    val sidePanelTextColor: Color = Color.White,
) {
    /** A copy of this config with the shared effect fields swapped for the
     *  side panel's own — pass this to Modifier.liquidGlass for the side
     *  panel instead of the raw config, since liquidGlass always reads the
     *  shared fields generically. */
    fun forSidePanel(): GlassEffectConfig = copy(
        vibrancy = sidePanelVibrancy,
        blurRadius = sidePanelBlurRadius,
        lensHeight = sidePanelLensHeight,
        lensAmount = sidePanelLensAmount,
        surfaceTintColor = sidePanelColor,
        surfaceOpacity = sidePanelSurfaceOpacity,
        textColor = sidePanelTextColor,
    )
    /**
     * Whether the glass effect should be rendered for [component], taking the master
     * switch and the per-component switch into account.
     */
    fun isEnabledFor(component: GlassComponent): Boolean =
        globalEnabled && when (component) {
            GlassComponent.PLAYER -> playerEnabled
            GlassComponent.MINI_PLAYER -> miniPlayerEnabled
            GlassComponent.NAV_BAR -> navBarEnabled
            GlassComponent.SIDE_PANEL -> sidePanelEnabled
        }
}

/** UI surfaces that can individually opt in or out of the liquid glass effect. */
enum class GlassComponent {
    PLAYER,
    MINI_PLAYER,
    NAV_BAR,
    SIDE_PANEL,
}

/**
 * Maximum lens refraction in dp when the 0..1 preference sliders are at 1. The
 * defaults (0.5) land on the 24dp height/amount used by the library author's
 * Apple-matched LiquidBottomTabs recipe.
 */
internal const val LENS_MAX_DP = 48f

/**
 * The full screen player uses a much heavier blur than the glass pills, matching
 * Apple Music where the now playing background is a deep-blurred material while
 * only the small controls are clear liquid glass.
 */
internal const val PLAYER_BLUR_MULTIPLIER = 4f

/** Lowest resolution fraction glass surfaces are rendered at (heavy blur hides it). */
internal const val MIN_GLASS_RESOLUTION_SCALE = 0.33f

/** Blur radius (dp) at or above which the minimum resolution scale is safe to use. */
internal const val FULL_QUALITY_BLUR_DP = 8f

/**
 * Resolution fraction at which a glass surface records and processes its backdrop.
 * Blur masks the upscaling, so the more blur, the lower the resolution can go: at
 * [FULL_QUALITY_BLUR_DP]+ dp of blur the surface renders at
 * [MIN_GLASS_RESOLUTION_SCALE]; with no blur it stays at full resolution so the
 * clear glass center remains crisp.
 */
fun glassResolutionScale(blurRadiusDp: Float): Float {
    val t = (blurRadiusDp / FULL_QUALITY_BLUR_DP).coerceIn(0f, 1f)
    return 1f - t * (1f - MIN_GLASS_RESOLUTION_SCALE)
}

/**
 * The backdrop blur pipeline requires [android.graphics.RenderEffect] on a
 * [android.graphics.RenderNode], which is available from Android 12 (API 31).
 */
fun isGlassSupported(sdkInt: Int = Build.VERSION.SDK_INT): Boolean = sdkInt >= Build.VERSION_CODES.S

/**
 * Devices Android flags as low-RAM ([ActivityManager.isLowRamDevice]) can't push the
 * RenderEffect/RuntimeShader chain (blur + lens + vibrancy, several offscreen layers
 * per glass surface) without dropped frames, so glass is skipped there in favor of the
 * flat fallback color every glass call site already has for [isGlassSupported] == false.
 */
@Composable
fun isLowRamDevice(): Boolean {
    val context = LocalContext.current
    return remember {
        context.getSystemService(ActivityManager::class.java)?.isLowRamDevice ?: false
    }
}

/** Combines the API-level check with the low-RAM device check; use this to gate glass. */
@Composable
fun isGlassAllowed(): Boolean = isGlassSupported() && !isLowRamDevice()

/**
 * Maps the user-facing vibrancy preference (0..2, default 1) to a saturation multiplier.
 * A value of 1 matches the library's built-in vibrancy effect (saturation x1.5), 0 leaves
 * colors untouched and 2 doubles the saturation.
 */
fun glassSaturation(vibrancy: Float): Float = 1f + 0.5f * vibrancy.coerceIn(0f, 2f)

/**
 * Apple's floating pills have a thin, subtle specular line along the top edge — a
 * hint of light, not a bold ring. The library's [Highlight.Default] (0.5dp, white @
 * 0.5 alpha) reads as near-invisible at typical density, but a wide bright rim
 * overshoots in the other direction, so this lands narrower and dimmer than that.
 */
private val EdgeHighlightWidth = 0.8f.dp
private const val EdgeHighlightAlpha = 0.55f

/**
 * Real Liquid Glass isn't a highlight painted on at a fixed angle: Apple describes
 * the material as dynamically bending and shaping light "in real time, every single
 * frame," and community shader recreations implement this as one or more soft light
 * sources drifting across the surface over time (e.g. animated via sin/cos of the
 * frame clock). A perfectly static 45° rim reads as flat and synthetic by
 * comparison, so the highlight's light angle slowly drifts instead of sitting still.
 */
private const val HighlightAngleMin = 25f
private const val HighlightAngleMax = 65f
private const val HighlightDriftMillis = 7000

/** Midpoint of the [HighlightAngleMin]..[HighlightAngleMax] sweep — see the use site. */
private const val HighlightAngleFrozen = (HighlightAngleMin + HighlightAngleMax) / 2f

val LocalGlassEffectConfig = staticCompositionLocalOf { GlassEffectConfig() }

/** The backdrop content (app UI) that glass surfaces sample from. */
val LocalAppBackdrop = staticCompositionLocalOf<Backdrop> { error("No AppBackdrop provided") }

/**
 * Whether the Apple Music-styled UI (iOS 26/27 liquid glass look, SF-style tab icons,
 * denser glass) is active. Read by [com.BeatWave.music.ui.screens.Screens] consumers to pick
 * between the classic and iOS icon sets.
 */
val LocalAppleMusicUi = staticCompositionLocalOf { false }

/**
 * Renders this composable as a liquid glass surface sampling [LocalAppBackdrop].
 *
 * Applies the configured vibrancy, blur and lens refraction effects, then draws the
 * surface tint (theme-adaptive unless the user picked a color). Effects whose
 * parameters make them a no-op are skipped entirely to keep the RenderEffect chain
 * as short as possible, and the backdrop is processed at [glassResolutionScale] of
 * the surface resolution. Returns the receiver unchanged on devices without
 * RenderEffect support.
 *
 * [applyEdgeEffects] controls the edge treatment that makes small pills read as
 * physical glass: lens refraction, the specular highlight rim and the drop shadow.
 * It should be false for large surfaces such as the full screen player, where the
 * rim renders as a stray band of light.
 *
 * [blurRadiusDp] overrides the configured blur; the full screen player passes a
 * heavier value ([PLAYER_BLUR_MULTIPLIER]x) than the clear glass pills.
 *
 * [shape] is restricted to [CornerBasedShape] because the backdrop lens effect throws
 * [UnsupportedOperationException] for any other shape type.
 */
@Composable
fun Modifier.liquidGlass(
    config: GlassEffectConfig,
    shape: CornerBasedShape = RoundedCornerShape(0.dp),
    applyEdgeEffects: Boolean = true,
    blurRadiusDp: Float = config.blurRadius,
    // The nav bar and mini player want a dimmer specular rim than the default.
    highlightAlpha: Float = EdgeHighlightAlpha,
    // Fraction of the surface resolution the backdrop is recorded at. Defaults to
    // [glassResolutionScale] for the blur radius (cheap, and the blur masks the
    // upscaling) — pass 1f for a crisp full-resolution backdrop, e.g. the small
    // glass buttons whose whole look is clear content, not heavy frost.
    backdropScale: Float = glassResolutionScale(blurRadiusDp),
    // Forwarded to drawBackdrop: while this returns true the surface holds its
    // last capture instead of re-recording the screen. Used by the floating tab
    // bar for the frames of its inline/expanded/search transition, where the
    // bar's bounds animate and would otherwise force a full-screen re-capture
    // plus effect chain on every frame.
    frozen: () -> Boolean = { false },
): Modifier {
    if (!isGlassAllowed()) return this
    val backdrop = LocalAppBackdrop.current
    val density = LocalDensity.current
    val resolutionScale = backdropScale.coerceIn(0.05f, 1f)
    // Pixel-sized effect parameters operate on the backdrop layer at its recorded
    // resolution, so they are pre-multiplied by the resolution scale to keep the
    // same visual size.
    val blurPx = with(density) { blurRadiusDp.dp.toPx() } * resolutionScale
    val saturation = glassSaturation(config.vibrancy)
    val lensHeightPx = with(density) { (config.lensHeight * LENS_MAX_DP).dp.toPx() } * resolutionScale
    val lensAmountPx = with(density) { (config.lensAmount * LENS_MAX_DP).dp.toPx() } * resolutionScale
    // Apple's Liquid Glass is a bright, reflective material: even in dark mode it
    // reads as a distinctly lighter "frosted" surface, not a near-black rectangle
    // that blends into an OLED-black background. Honor an explicit user color,
    // otherwise use a proper adaptive glass gray rather than matching the theme
    // surface color 1:1 (which made the bar invisible over pure-black content).
    val surfaceTintColor = if (config.surfaceTintColor.isSpecified) {
        config.surfaceTintColor
    } else if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) {
        Color(0xFFFAFAFA)
    } else {
        Color(0xFF4A4A4E)
    }

    // ponytail: frozen mid-sweep. The drift described at [HighlightAngleMin] was a
    // rememberInfiniteTransition, and because glass surfaces (the nav bar above all)
    // are on screen permanently, it re-emitted every frame forever. Each emission
    // invalidated the backdrop draw, and LayerBackdropModifier re-records the whole
    // screen unconditionally per draw — so a 0.8dp rim highlight cost a measured
    // ~45fps of full-screen capture plus three blur passes and ~400mA, at idle,
    // with nothing on screen moving. Slowing the tween does NOT help: an infinite
    // transition emits per frame regardless of duration.
    // Upgrade path: gate LayerBackdropModifier's recordLayer() on the source content
    // actually changing, then restore the transition — the drift becomes ~free.
    val animatedHighlightAngle = HighlightAngleFrozen

    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            if (saturation != 1f) {
                colorControls(saturation = saturation)
            }
            if (blurPx > 0f) {
                blur(blurPx)
            }
            if (applyEdgeEffects &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                (lensHeightPx > 0f || lensAmountPx > 0f)
            ) {
                lens(
                    refractionHeight = lensHeightPx,
                    refractionAmount = lensAmountPx,
                    depthEffect = config.depthEffect,
                    chromaticAberration = config.chromaticAberration,
                )
            }
        },
        highlight = if (applyEdgeEffects) {
            {
                Highlight(
                    width = EdgeHighlightWidth,
                    style = HighlightStyle.Default(
                        color = Color.White.copy(alpha = highlightAlpha),
                        angle = animatedHighlightAngle,
                    ),
                )
            }
        } else {
            null
        },
        shadow = if (applyEdgeEffects) ({ Shadow.Default }) else null,
        onDrawSurface = {
            if (config.surfaceOpacity > 0f) {
                drawRect(
                    color = surfaceTintColor.copy(alpha = config.surfaceOpacity),
                    size = size,
                )
            }
        },
        backdropScale = resolutionScale,
        frozen = frozen,
    )
}
