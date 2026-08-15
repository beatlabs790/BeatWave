/**
 * BeatWave Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.beatwave.music.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Fast-scroll thumb for a [LazyListState], drawn over the list's trailing edge.
 *
 * Two rules keep it honest, and both were previously broken:
 *
 * 1. While dragging, the thumb follows the finger and the list follows the thumb.
 *    The list must never be allowed to move the thumb — the old version scrolled
 *    the list, read the resulting `firstVisibleItemIndex` back out to position the
 *    thumb, and wrote that position into a `var` from inside a `derivedStateOf`.
 *    That is a write to observed state during a derived computation, so it
 *    re-invalidated itself; combined with three separate smoothing filters fighting
 *    each other, the thumb drifted away from the finger and juddered.
 *
 * 2. One scroll at a time. The old version launched a fresh `animateScrollToItem`
 *    every 40ms without cancelling the last, so several scroll animations ran
 *    concurrently toward different targets. Dragging now cancels the in-flight
 *    scroll and issues an instant `scrollToItem`; there is nothing to animate,
 *    because the finger is already providing the motion.
 */
@Composable
fun DraggableScrollbar(
    scrollState: LazyListState,
    modifier: Modifier = Modifier,
    thumbColor: Color = LocalContentColor.current.copy(alpha = 0.8f),
    thumbColorActive: Color = MaterialTheme.colorScheme.secondary,
    thumbHeight: Dp = 72.dp,
    thumbWidth: Dp = 8.dp,
    thumbCornerRadius: Dp = 4.dp,
    trackWidth: Dp = 24.dp,
    minItemCountForScroll: Int = 15,
    minScrollRangeForDrag: Int = 5,
    headerItems: Int = 0,
    /**
     * Maps a content index (header items already subtracted) to the text shown in a
     * bubble beside the thumb while dragging — an initial for an A-Z sort, a year, a
     * duration. Null keeps the bare thumb, which is what every other caller wants.
     */
    label: ((Int) -> String)? = null,
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // Both are read only from the Canvas draw lambda, so writing them during a drag
    // invalidates the draw phase and nothing else — no recomposition per touch move.
    /** Thumb top edge while the finger owns it. Only the drag handler writes this. */
    var dragThumbY by remember { mutableFloatStateOf(0f) }
    var isDraggingState by remember { mutableStateOf(false) }

    /** Highest first-visible index the list can actually come to rest on. */
    val maxScrollIndex by remember(scrollState, headerItems) {
        derivedStateOf {
            val layoutInfo = scrollState.layoutInfo
            val visible = layoutInfo.visibleItemsInfo.size
            max(1, layoutInfo.totalItemsCount - headerItems - visible)
        }
    }

    val isScrollable by remember(scrollState, headerItems) {
        derivedStateOf {
            val layoutInfo = scrollState.layoutInfo
            val contentCount = layoutInfo.totalItemsCount - headerItems
            contentCount > minItemCountForScroll &&
                contentCount > layoutInfo.visibleItemsInfo.size
        }
    }

    /**
     * Where the list currently is, 0..1. Read-only: it observes the list and never
     * writes anything back, which is what stops the thumb and the list from
     * driving each other in a circle.
     *
     * The intra-item fraction is what makes the thumb glide. Index alone moves it
     * one whole item-height at a time, which on a long list reads as the thumb
     * stuttering downward in visible steps.
     */
    val scrollProgress by remember(scrollState, headerItems) {
        derivedStateOf {
            val visible = scrollState.layoutInfo.visibleItemsInfo
            if (visible.isEmpty()) return@derivedStateOf 0f
            val index = (scrollState.firstVisibleItemIndex - headerItems).coerceAtLeast(0)
            val firstSize = visible.first().size.takeIf { it > 0 } ?: return@derivedStateOf 0f
            val withinItem =
                (scrollState.firstVisibleItemScrollOffset.toFloat() / firstSize).coerceIn(0f, 1f)
            ((index + withinItem) / maxScrollIndex).coerceIn(0f, 1f)
        }
    }

    if (!isScrollable) return

    BoxWithConstraints(
        modifier = modifier
            .width(trackWidth)
            .fillMaxHeight()
            .pointerInput(scrollState, headerItems) {
                val thumbHeightPx = with(density) { thumbHeight.toPx() }
                // Plain locals, not remembered state: only this gesture loop reads
                // them, so making them observable would just cost recompositions.
                var lastTargetIndex = -1
                var scrollJob: Job? = null

                fun seek(touchY: Float) {
                    val maxThumbY = (size.height - thumbHeightPx).coerceAtLeast(1f)
                    dragThumbY = (touchY - thumbHeightPx / 2f).coerceIn(0f, maxThumbY)
                    if (maxScrollIndex <= minScrollRangeForDrag) return

                    val target = headerItems + (dragThumbY / maxThumbY * maxScrollIndex).roundToInt()
                    if (target == lastTargetIndex) return
                    lastTargetIndex = target
                    // Cancel first: overlapping scrolls toward different targets are
                    // what made the list lurch back and forth under the finger.
                    scrollJob?.cancel()
                    scrollJob = coroutineScope.launch {
                        scrollState.scrollToItem(
                            target.coerceIn(0, scrollState.layoutInfo.totalItemsCount - 1)
                        )
                    }
                }

                detectDragGestures(
                    onDragStart = { offset ->
                        isDraggingState = true
                        lastTargetIndex = -1
                        seek(offset.y)
                    },
                    onDragEnd = { isDraggingState = false },
                    onDragCancel = { isDraggingState = false },
                ) { change, _ -> seek(change.position.y) }
            }
    ) {
        val viewportHeight = with(density) { this@BoxWithConstraints.maxHeight.toPx() }
        val thumbHeightPx = with(density) { thumbHeight.toPx() }
        val maxThumbY = (viewportHeight - thumbHeightPx).coerceAtLeast(0f)

        Canvas(
            modifier = Modifier
                .width(thumbWidth)
                .fillMaxHeight()
                .align(Alignment.CenterEnd)
        ) {
            // Dragging: the finger owns the thumb. Otherwise: the list does. Never
            // both, so there is no filter needed to reconcile them.
            val thumbY = if (isDraggingState) dragThumbY else scrollProgress * maxThumbY

            drawRoundRect(
                color = if (isDraggingState) thumbColorActive else thumbColor,
                topLeft = Offset(0f, thumbY),
                size = Size(size.width, thumbHeightPx),
                cornerRadius = CornerRadius(thumbCornerRadius.toPx())
            )
        }

        // Only while dragging, so a resting list pays nothing for this. The index comes
        // from the list rather than the drag handler because the list is already
        // following the thumb, and reading it here keeps the bubble a composition-level
        // concern instead of another thing the gesture loop has to drive.
        if (label != null && isDraggingState) {
            val contentIndex = (scrollState.firstVisibleItemIndex - headerItems).coerceAtLeast(0)
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = RoundedCornerShape(10.dp),
                shadowElevation = 3.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset {
                        IntOffset(
                            x = -with(density) { (trackWidth + 8.dp).roundToPx() },
                            y = (dragThumbY + thumbHeightPx / 2f - with(density) { 16.dp.toPx() })
                                .coerceIn(0f, (viewportHeight - with(density) { 32.dp.toPx() }).coerceAtLeast(0f))
                                .roundToInt(),
                        )
                    }
            ) {
                Text(
                    text = label(contentIndex),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}
