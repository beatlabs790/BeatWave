/**
 * BeatWave Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.beatwave.music.ui.utils

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.OverscrollFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlin.math.abs
import kotlin.math.sign

/**
 * UIKit's rubber band constant. Apple's own value; lower is stiffer.
 */
private const val RubberBandConstant = 0.55f

/**
 * Fallback container height (px) for the one frame before the node has measured.
 */
private const val FallbackContainerPx = 2000f

/** Fraction of unconsumed fling velocity handed to the spring-back. */
private const val FlingBounceScale = 0.4f
private const val MaxBounceVelocity = 4000f

/**
 * UIScrollView's rubber band curve: `(1 - 1/(d*c/dim + 1)) * dim/c`.
 *
 * [rawDistance] is how far the finger has actually travelled past the edge; the
 * result is how far the content is allowed to move. Near zero it is 1:1 with the
 * finger (iOS feels immediate, never mushy), and it asymptotes toward `dim/c`, so
 * it self-limits instead of needing an arbitrary hard cap that the pull slams
 * into.
 *
 * Shared with [heroPullZoom] so the hero screens' pull and the plain list bounce
 * are literally the same curve.
 */
internal fun rubberBand(rawDistance: Float, containerPx: Float): Float {
    val dim = if (containerPx > 0f) containerPx else FallbackContainerPx
    val d = abs(rawDistance)
    val banded = (1f - 1f / (d * RubberBandConstant / dim + 1f)) * dim / RubberBandConstant
    return banded * sign(rawDistance)
}

/**
 * iOS-style rubber-band overscroll, expressed as an [OverscrollEffect] so it can be
 * installed once via `LocalOverscrollFactory` instead of being applied per scroll
 * container. Every LazyColumn / LazyGrid / scrollable Column under the provider
 * bounces, including ones in screens nobody remembered to update.
 *
 * Replaces Android's stretch/glow edge effect while it is provided.
 *
 * State is the RAW distance dragged past the edge, held in a plain mutable float
 * mutated synchronously in [applyToScroll] — not an
 * [androidx.compose.animation.core.Animatable] — because `applyToScroll` isn't
 * suspend and every scroll delta during a drag used to launch its own
 * fire-and-forget `snapTo` coroutine. Under a fast drag that's dozens of coroutines
 * racing each other per second, and the offset could end up stuck on a stale value
 * from a coroutine that hadn't run yet. A coroutine (and a real spring) is only
 * needed once, on release, to animate back to rest.
 */
class IosOverscrollEffect : OverscrollEffect {

    /** Raw finger distance past the edge; the drawn offset is [rubberBand] of it. */
    private val rawPull = mutableFloatStateOf(0f)

    /** Measured by [IosOverscrollNode]; the rubber band scales with it, as on iOS. */
    private var containerPx = 0f

    private val drawOffset: Float
        get() = rubberBand(rawPull.floatValue, containerPx)

    override val isInProgress: Boolean
        get() = rawPull.floatValue != 0f

    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset,
    ): Offset {
        var selfConsumed = 0f
        val current = rawPull.floatValue

        // Dragging back toward rest pays down the existing stretch before the
        // list itself gets to scroll — otherwise the content jumps. Paying down
        // happens in raw (finger) space, so the return trip is damped by exactly
        // the same curve as the outbound pull, which is what makes the stretch
        // track the finger symmetrically.
        if (current != 0f && delta.y != 0f && sign(delta.y) != sign(current)) {
            val target = current + delta.y
            val settled = if (sign(target) != sign(current)) 0f else target
            selfConsumed = settled - current
            rawPull.floatValue = settled
        }

        val remaining = Offset(delta.x, delta.y - selfConsumed)
        val consumedByScroll = performScroll(remaining)
        val leftover = remaining - consumedByScroll

        // Leftover means the list hit an edge, so stretch. Drag only: absorbing
        // leftover during a fling reported the whole delta back as consumed, so
        // the scrollable's decay animation never saw an edge and ran its full
        // duration with the stretch pinned at maximum — the bounce appeared to
        // hang, then finally sprang back once the decay expired. Returning
        // nothing consumed makes the decay cancel at once and hand its remaining
        // velocity to applyToFling, which is where the bounce belongs.
        if (leftover.y != 0f && source == NestedScrollSource.UserInput) {
            rawPull.floatValue += leftover.y
            selfConsumed += leftover.y
        }

        return Offset(consumedByScroll.x, consumedByScroll.y + selfConsumed)
    }

    override suspend fun applyToFling(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity,
    ) {
        val leftoverVelocity = velocity.y - performFling(velocity).y
        if (rawPull.floatValue == 0f && leftoverVelocity == 0f) return

        // Critically damped, not springy. iOS snaps back and stops dead; a
        // dampingRatio below 1 wobbles at the end, which is the single thing that
        // most makes a rubber band read as "Android imitating iOS".
        //
        // A new drag beats this: the scrollable cancels this suspend fling (and
        // this animate call with it) as soon as the next pointer-down starts a
        // fresh drag, so there's no coroutine to race against.
        //
        // Velocity the list could not consume (a flick straight into an
        // already-reached edge) seeds the spring, so it overshoots into a real
        // bounce instead of just easing a static stretch back to rest. At rest
        // the rubber band is 1:1, so fling velocity needs no curve conversion.
        animate(
            initialValue = rawPull.floatValue,
            targetValue = 0f,
            initialVelocity = (leftoverVelocity * FlingBounceScale)
                .coerceIn(-MaxBounceVelocity, MaxBounceVelocity),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        ) { value, _ -> rawPull.floatValue = value }
    }

    override val node: DelegatableNode = IosOverscrollNode(
        offsetY = { drawOffset },
        onMeasured = { containerPx = it },
    )
}

private class IosOverscrollNode(
    private val offsetY: () -> Float,
    private val onMeasured: (Float) -> Unit,
) : Modifier.Node(), LayoutModifierNode {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        // The rubber band is scaled by the scroll container's own size on iOS —
        // a short list resists sooner than a full-screen one.
        onMeasured(
            (if (constraints.hasBoundedHeight) constraints.maxHeight else placeable.height).toFloat()
        )
        return layout(placeable.width, placeable.height) {
            // Read the offset inside the layer block so the bounce animates in the
            // draw phase, without re-laying-out the list every frame.
            placeable.placeWithLayer(0, 0) { translationY = offsetY() }
        }
    }
}

private class IosOverscrollFactory(
    private val density: Density,
    private val scope: CoroutineScope,
) : OverscrollFactory {
    override fun createOverscrollEffect(): OverscrollEffect = IosOverscrollEffect()

    override fun equals(other: Any?): Boolean =
        other is IosOverscrollFactory && other.density == density && other.scope === scope

    override fun hashCode(): Int = 31 * density.hashCode() + scope.hashCode()
}

/** Factory to hand to `LocalOverscrollFactory` to make the whole app bounce. */
@Composable
fun rememberIosOverscrollFactory(): OverscrollFactory {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    return remember(density, scope) { IosOverscrollFactory(density, scope) }
}
