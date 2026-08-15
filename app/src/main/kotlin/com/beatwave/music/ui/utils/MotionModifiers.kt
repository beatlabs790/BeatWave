/**
 * BeatWave Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.beatwave.music.ui.utils

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.beatwave.music.constants.IosOverscrollKey
import com.beatwave.music.utils.rememberPreference
import kotlin.math.sign

/**
 * Everything a hero screen needs for pull-to-zoom, so each one is two lines
 * instead of five: pass [scale] to `HeroBackground(heroScale = …)` and
 * [Modifier.heroPullZoom] to its list.
 *
 * [rawPull] is the distance the FINGER has travelled past the top edge; [offset]
 * is how far the content is actually allowed to move, which is [rubberBand] of
 * that. Keeping the two apart is the whole point: the previous version stored the
 * damped value and damped it *again* on each delta with
 * `resistance = (1 - pull/maxPull) * 0.5`, so the content moved at HALF finger
 * speed from the very first pixel and then hit a hard clamp at maxPull. Starting
 * at half speed is what made the pull feel cheap and disconnected — iOS tracks the
 * finger 1:1 at the start and only stiffens as you go, which is exactly what the
 * rubber band curve does.
 *
 * Both are plain [MutableFloatState]s rather than an
 * [androidx.compose.animation.core.Animatable] mutated via `scope.launch { snapTo(...) }`
 * on every scroll delta — that pattern let a fast drag queue up dozens of
 * coroutines racing each other (same bug class as the app's overscroll had),
 * and the zoom could get stuck mid-scale when one of them applied a stale
 * value after a newer one. Scroll-time updates are now a direct synchronous
 * write; a coroutine (and a real spring) is only used once, on release.
 */
@Stable
class HeroZoom internal constructor(
    internal val rawPull: MutableFloatState,
    /** List height in px, measured by [heroPullZoom]; the curve scales with it. */
    internal val viewport: MutableFloatState,
    val maxPull: Float,
    val enabled: Boolean,
) {
    /**
     * How far the list is translated down, in px. Also what a hero image must
     * grow by to keep covering the top of the screen while the list slides.
     */
    val offset: Float
        get() = if (enabled) rubberBand(rawPull.floatValue, viewport.floatValue) else 0f

    /**
     * Zoom only responds to the TOP pull. The bottom edge shares the same offset
     * (it is the same rubber band, just negative) but must not shrink the hero.
     */
    val scale: Float
        get() = if (enabled) 1f + (offset / maxPull).coerceIn(0f, 1f) * 0.18f else 1f
}

@Composable
fun rememberHeroZoom(maxPull: Dp = 220.dp): HeroZoom {
    val rawPull = remember { mutableFloatStateOf(0f) }
    val viewport = remember { mutableFloatStateOf(0f) }
    val maxPullPx = with(LocalDensity.current) { maxPull.toPx() }
    val enabled by rememberPreference(IosOverscrollKey, defaultValue = false)
    return remember(rawPull, viewport, maxPullPx, enabled) {
        HeroZoom(rawPull, viewport, maxPullPx, enabled)
    }
}

/**
 * What to pass as a hero list's `overscrollEffect`: null while [heroPullZoom] owns
 * BOTH edges (it would otherwise eat the leftover before the zoom connection ever
 * sees it), otherwise the ambient effect, so switching the motion preference off
 * leaves the list with normal overscroll rather than none at all.
 */
@Composable
fun HeroZoom.listOverscroll(): OverscrollEffect? =
    if (enabled) null else rememberOverscrollEffect()

/** Displayed pull past which releasing fires [heroPullZoom]'s `onRefresh`. */
private val RefreshThreshold = 120.dp

/**
 * Rubber-band overscroll for a hero-header list, on the same [rubberBand] curve as
 * the plain list bounce. The top pull additionally drives the zoom: pass
 * [HeroZoom.scale] to `HeroBackground(heroScale = …)`. The bottom edge is a plain
 * bounce. No-ops when the iOS-motion preference is off.
 *
 * When [onRefresh] is non-null the top pull doubles as pull-to-refresh: crossing
 * [RefreshThreshold] ticks a haptic, and releasing past it fires [onRefresh]. There
 * is deliberately no spinner — the stretch IS the indicator, so the gesture costs
 * no extra chrome and the zoom is not given up to get a reload.
 */
fun Modifier.heroPullZoom(
    zoom: HeroZoom,
    onRefresh: (() -> Unit)? = null,
): Modifier = composed {
    if (!zoom.enabled) return@composed this

    val rawPull = zoom.rawPull
    val viewport = zoom.viewport
    val thresholdPx = with(LocalDensity.current) { RefreshThreshold.toPx() }
    val haptics = LocalHapticFeedback.current
    // Kept live so a screen swapping its refresh lambda doesn't rebuild the
    // connection (and lose the in-flight gesture's state with it).
    val refresh = rememberUpdatedState(onRefresh)
    val connection = remember(zoom, thresholdPx, haptics) {
        object : NestedScrollConnection {
            /** Pull has crossed the refresh threshold during the current gesture. */
            private var armed = false
            // Scrolling back toward rest pays down the existing stretch before the
            // list itself gets to scroll, otherwise the content jumps. In raw
            // (finger) space, so the return trip is damped by the same curve as
            // the outbound pull and the content tracks the finger symmetrically.
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val curr = rawPull.floatValue
                if (curr != 0f && available.y != 0f && sign(available.y) != sign(curr)) {
                    val target = curr + available.y
                    val settled = if (sign(target) != sign(curr)) 0f else target
                    rawPull.floatValue = settled
                    return Offset(0f, settled - curr)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                // Both edges: pulling down past the top drives the hero stretch,
                // pushing up past the bottom is a plain bounce. The bottom used to
                // do nothing at all here — heroPullZoom was top-only AND
                // listOverscroll() hands these lists a null effect, so the bottom
                // edge had no bounce of any kind.
                //
                // Drag only. Absorbing (and reporting as consumed) the leftover
                // of a fling keeps the scrollable's decay animation alive for its
                // full duration with the zoom held at max — it reads as the pull
                // sticking, then snapping back late. Let the fling end at the
                // edge instead; onPreFling below does the spring.
                if (available.y == 0f || source != NestedScrollSource.UserInput) return Offset.Zero
                // Accumulate the RAW finger distance; the resistance lives entirely
                // in rubberBand, which starts 1:1 and stiffens as it goes rather
                // than starting at half speed and clamping.
                rawPull.floatValue += available.y

                // Threshold is on the DISPLAYED offset, not the raw finger travel:
                // the tick has to land where the user sees the stretch reach it.
                if (refresh.value != null) {
                    val past = zoom.offset >= thresholdPx
                    if (past != armed) {
                        armed = past
                        if (past) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
                return available
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                // Fire before the spring, not after: the reload should start the
                // instant the finger lifts, not ~350ms later when the stretch has
                // finished retracting.
                if (armed) {
                    armed = false
                    refresh.value?.invoke()
                }
                if (rawPull.floatValue != 0f) {
                    // Snappy spring-back on release — StiffnessLow read as "waits,
                    // then drifts down"; Medium gives the immediate iOS rubber-band.
                    // Critically damped: iOS returns and stops dead, it does not
                    // wobble at the end, and a wobbling hero zoom is the tell.
                    animate(
                        initialValue = rawPull.floatValue,
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                    ) { value, _ -> rawPull.floatValue = value }
                    return available
                }
                return Velocity.Zero
            }
        }
    }
    // Translate the WHOLE list down by the pull so the header and the content
    // below move as one unit (iOS stretch), not just the image scaling in place.
    this
        .onSizeChanged { viewport.floatValue = it.height.toFloat() }
        .graphicsLayer { translationY = zoom.offset }
        .nestedScroll(connection)
}

/**
 * [basicMarquee] that only runs while the text is actually on screen.
 *
 * A plain `Modifier.basicMarquee()` starts an infinite animation the moment it is
 * composed and never stops. In a lazy list that means every row Compose keeps
 * around — including the ones scrolled out of view and the ones prefetched ahead
 * of the viewport — drives an animation frame forever, so the whole app redraws
 * every vsync with nothing visible moving. Gating on real visibility keeps the
 * effect where the user can see it and costs one bounds check per layout pass.
 *
 * Visibility is read from the node's bounds in the window rather than a lazy
 * list's item info, so this works in any container: grids, rows, the player, a
 * plain Column.
 */
@Composable
fun Modifier.marqueeWhenVisible(): Modifier {
    var visible by remember { mutableStateOf(false) }
    return this
        .onGloballyPositioned { coordinates ->
            // boundsInWindow() collapses to an empty rect once the node is fully
            // clipped by an ancestor, which is exactly "scrolled out of sight".
            val onScreen = coordinates.isAttached && !coordinates.boundsInWindow().isEmpty
            if (onScreen != visible) visible = onScreen
        }
        .then(if (visible) Modifier.basicMarquee() else Modifier)
}
