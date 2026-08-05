/**
 * BeatWave Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convx.music.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SwitchColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.convx.music.ui.component.backdrop.backdrops.rememberLayerBackdrop
import com.convx.music.ui.component.backdrop.catalog.components.LiquidToggle

/**
 * iOS-style toggle switch with liquid glass track when glass is enabled.
 * Falls back to solid green/grey track on unsupported devices.
 */
@Composable
fun GlassSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val glassConfig = LocalGlassEffectConfig.current
    val useGlass = glassConfig.globalEnabled && isGlassAllowed()

    val thumbProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(200),
        label = "glassSwitchThumb",
    )

    val trackShape = RoundedCornerShape(16.dp)
    val trackWidth = 51.dp
    val trackHeight = 31.dp
    val thumbSize = 27.dp
    val thumbPadding = 2.dp
    val maxTravel = trackWidth - thumbSize - thumbPadding * 2

    // Sample an UNATTACHED, switch-local backdrop rather than whatever
    // LocalAppBackdrop happens to be. These live inside NavHost screens (the
    // settings lists), and the app root wraps the whole NavHost in
    // layerBackdrop(appBackdrop) — so a glass surface in here that samples
    // appBackdrop puts itself inside that capture and native
    // RenderNode::prepareTreeImpl recurses until it segfaults. Unattached means
    // drawBackdrop early-returns, so the track renders as frosted translucent
    // chrome with no self-reference. Same rule as the Artist/Album/Playlist
    // chrome and HideOnScrollFAB.
    val switchBackdrop = rememberLayerBackdrop()
    CompositionLocalProvider(LocalAppBackdrop provides switchBackdrop) {
    Box(
        modifier = modifier
            .width(trackWidth)
            .height(trackHeight)
            .clip(trackShape)
            .then(
                if (useGlass && checked && enabled) {
                    // Green wash ON TOP of the glass. The backdrop above is
                    // unattached, so the track has no live content to refract and
                    // glass alone rendered as a faint outline — "on" was all but
                    // indistinguishable from "off" at a glance. The wash restores
                    // the iOS on-state signal while the rim highlight and lens
                    // still read underneath it.
                    Modifier
                        .liquidGlass(
                            config = glassConfig,
                            shape = trackShape,
                            applyEdgeEffects = true,
                            blurRadiusDp = 4f,
                        )
                        .background(Color(0xFF34C759).copy(alpha = 0.55f))
                } else {
                    Modifier.background(
                        when {
                            !enabled -> Color(0xFF39393D).copy(alpha = 0.4f)
                            checked -> Color(0xFF34C759)
                            else -> Color(0xFF39393D)
                        }
                    )
                }
            )
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset((thumbPadding + maxTravel * thumbProgress).roundToPx(), 0) }
                .size(thumbSize)
                .shadow(3.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
    }
}

/**
 * Signature-compatible stand-in for Material3's `Switch`, so a screen can adopt
 * [GlassSwitch] by aliasing its import rather than rewriting every call site:
 *
 * ```
 * import com.BeatWave.music.ui.component.GlassSwitchCompat as Switch
 * ```
 *
 * [thumbContent] and [colors] are accepted and deliberately ignored — the glass
 * switch draws its own thumb and takes its track from the glass config, so the
 * check/close icons and Material color roles the call sites pass have nothing to
 * apply to.
 */
@Composable
fun GlassSwitchCompat(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    colors: SwitchColors? = null,
) {
    // Unattached outer backdrop on purpose. These toggles live inside NavHost
    // screens, and a glass surface in there that samples the root appBackdrop ends
    // up inside its own capture — native RenderNode recursion, SIGSEGV.
    // [LiquidToggle] does not need it for the effect: the thumb refracts its own
    // track, via a backdrop the toggle attaches to the track internally and
    // combines with this one. So the look is unaffected by passing an empty outer
    // backdrop.
    val outerBackdrop = rememberLayerBackdrop()
    LiquidToggle(
        selected = { checked },
        onSelect = { if (enabled) onCheckedChange?.invoke(it) },
        backdrop = outerBackdrop,
        modifier = modifier.graphicsLayer { alpha = if (enabled) 1f else 0.5f },
    )
}
