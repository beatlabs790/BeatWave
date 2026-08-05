/**
 * BeatWave Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convx.music.playback.audio

import kotlin.math.abs
import kotlin.math.max

/** Locked 3-tier fallback from the grilling session: never force a treatment
 *  the BPM data can't support — a bad auto-mix sounds worse than a plain
 *  crossfade, and that's exactly what tier 3 protects against. */
enum class DjMixTier {
    /** Confident BPM on both sides, tempos close enough to stretch without
     *  it sounding wrong — full treatment: EQ sweep + reverb/delay + tempo. */
    FULL_DJ,
    /** BPM detected but not confident/compatible enough for the full
     *  treatment — Apple Automix-style: small tempo nudge only. */
    SMART_CROSSFADE,
    /** No usable BPM on one or both sides (spoken word, ambient, detection
     *  failed) — today's plain volume crossfade, unchanged. */
    PLAIN_CROSSFADE,
}

data class DjMixPlan(
    val tier: DjMixTier,
    /** Playback speed to apply to the incoming track, 1f = no change.
     *  Only meaningful for FULL_DJ/SMART_CROSSFADE. */
    val incomingSpeedAdjustment: Float = 1f,
)

object DjMixPlanner {
    private const val MIN_CONFIDENCE_FOR_SMART = 0.35f
    private const val MIN_CONFIDENCE_FOR_FULL = 0.55f
    /** Stretch tolerance for the full treatment — beyond this the tempo
     *  correction would be audible/unnatural rather than seamless. */
    private const val MAX_FULL_STRETCH_PERCENT = 0.08f
    /** Smart crossfade allows a slightly wider nudge since there's no EQ/
     *  effects camouflage riding along with it. */
    private const val MAX_SMART_STRETCH_PERCENT = 0.06f

    fun plan(outgoing: BpmEstimate?, incoming: BpmEstimate?): DjMixPlan {
        if (outgoing == null || incoming == null || outgoing.bpm <= 0f || incoming.bpm <= 0f) {
            return DjMixPlan(DjMixTier.PLAIN_CROSSFADE)
        }

        val minConfidence = max(0f, minOf(outgoing.confidence, incoming.confidence))
        val stretchPercent = abs(outgoing.bpm - incoming.bpm) / incoming.bpm

        return when {
            minConfidence >= MIN_CONFIDENCE_FOR_FULL && stretchPercent <= MAX_FULL_STRETCH_PERCENT ->
                DjMixPlan(DjMixTier.FULL_DJ, incomingSpeedAdjustment = outgoing.bpm / incoming.bpm)

            minConfidence >= MIN_CONFIDENCE_FOR_SMART && stretchPercent <= MAX_SMART_STRETCH_PERCENT ->
                DjMixPlan(DjMixTier.SMART_CROSSFADE, incomingSpeedAdjustment = outgoing.bpm / incoming.bpm)

            else -> DjMixPlan(DjMixTier.PLAIN_CROSSFADE)
        }
    }
}
