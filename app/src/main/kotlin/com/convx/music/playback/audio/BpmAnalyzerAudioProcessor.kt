/**
 * BeatWave Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convx.music.playback.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.roundToInt

data class BpmEstimate(val bpm: Float, val confidence: Float)

/**
 * Passive, always-transparent (never alters the audio) BPM estimator: energy-
 * based onset detection (short-window RMS peaks above a rolling average) plus
 * an inter-onset-interval histogram to estimate tempo. Good enough for
 * confidence-gated auto-DJ mixing — it isn't meant to be broadcast-grade beat
 * tracking, and low-confidence results are exactly what the 3-tier fallback
 * (see DjMixPlanner) is for: ambient/classical/spoken word correctly end up
 * with no usable estimate rather than a wrong one.
 *
 * Analyzes the first [analysisWindowMs] of playback for the current track,
 * then reports once via [onEstimateReady] and goes idle until [resetForTrack]
 * is called for the next one.
 */
@UnstableApi
class BpmAnalyzerAudioProcessor(
    private val analysisWindowMs: Long = 20_000L,
    private val onEstimateReady: (BpmEstimate) -> Unit,
) : AudioProcessor {

    private var sampleRate = 0
    private var channelCount = 0
    private var bytesPerSample = 0

    private var outputBuffer: ByteBuffer = EMPTY_BUFFER
    private var inputEnded = false

    private var analyzedFrames = 0L
    private var reported = false

    // Short-window RMS envelope, sampled at ~100 windows/sec, plus a slow
    // rolling average to detect peaks (onsets) against.
    private val windowSizeFrames get() = (sampleRate / 100).coerceAtLeast(1)
    private var frameAccumulator = 0.0
    private var framesInWindow = 0
    private var rollingAverage = 0.0
    private val onsetTimesMs = mutableListOf<Long>()
    private var elapsedMs = 0L

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        sampleRate = inputAudioFormat.sampleRate
        channelCount = inputAudioFormat.channelCount
        bytesPerSample = when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_16BIT, C.ENCODING_PCM_16BIT_BIG_ENDIAN -> 2
            C.ENCODING_PCM_24BIT, C.ENCODING_PCM_24BIT_BIG_ENDIAN -> 3
            C.ENCODING_PCM_32BIT, C.ENCODING_PCM_32BIT_BIG_ENDIAN, C.ENCODING_PCM_FLOAT -> 4
            else -> 0
        }
        if (bytesPerSample == 0) {
            sampleRate = 0
            channelCount = 0
        }
        return inputAudioFormat
    }

    override fun isActive(): Boolean = bytesPerSample != 0 && !reported

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) {
            outputBuffer = EMPTY_BUFFER
            return
        }

        if (!reported && sampleRate > 0) {
            analyze(inputBuffer)
        }

        val out = replaceOutputBuffer(inputBuffer.remaining())
        out.put(inputBuffer)
        out.flip()
    }

    private fun analyze(buffer: ByteBuffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        val frameSize = bytesPerSample * channelCount
        if (frameSize <= 0) return
        val frameCount = buffer.remaining() / frameSize
        val basePosition = buffer.position()
        val windowFrames = windowSizeFrames

        repeat(frameCount) { frameIndex ->
            var peak = 0
            repeat(channelCount) { channelIndex ->
                val sampleIndex = basePosition + (frameIndex * channelCount + channelIndex) * bytesPerSample
                val magnitude = when (bytesPerSample) {
                    2 -> abs(buffer.getShort(sampleIndex).toInt())
                    3 -> {
                        val b0 = buffer.get(sampleIndex).toInt() and 0xFF
                        val b1 = buffer.get(sampleIndex + 1).toInt() and 0xFF
                        val b2 = buffer.get(sampleIndex + 2).toInt()
                        abs((b2 shl 16) or (b1 shl 8) or b0) shr 8
                    }
                    else -> abs(buffer.getInt(sampleIndex)) shr 16
                }
                if (magnitude > peak) peak = magnitude
            }

            frameAccumulator += peak.toDouble() * peak.toDouble()
            framesInWindow++
            analyzedFrames++

            if (framesInWindow >= windowFrames) {
                val rms = kotlin.math.sqrt(frameAccumulator / framesInWindow)
                elapsedMs += (framesInWindow * 1000L) / sampleRate

                // Onset: energy well above the recent rolling average.
                if (rollingAverage > 0.0 && rms > rollingAverage * 1.4) {
                    val lastOnset = onsetTimesMs.lastOrNull()
                    // Debounce: ignore onsets closer than 200ms (>300 BPM ceiling).
                    if (lastOnset == null || elapsedMs - lastOnset > 200L) {
                        onsetTimesMs.add(elapsedMs)
                    }
                }
                rollingAverage = if (rollingAverage == 0.0) rms else (rollingAverage * 0.94 + rms * 0.06)

                frameAccumulator = 0.0
                framesInWindow = 0
            }
        }

        if (!reported && elapsedMs >= analysisWindowMs) {
            reported = true
            onEstimateReady(estimateFromOnsets(onsetTimesMs))
        }
    }

    /** Inter-onset-interval histogram: bucket the gaps between onsets into
     *  BPM bins and take the strongest bin as the estimate. Confidence is
     *  how dominant that bin is relative to the rest — a clean, steady beat
     *  concentrates in one bin; noise/no-beat material spreads thin. */
    private fun estimateFromOnsets(onsets: List<Long>): BpmEstimate {
        if (onsets.size < 8) return BpmEstimate(0f, 0f)

        val bpmBins = HashMap<Int, Int>()
        for (i in onsets.indices) {
            for (j in (i + 1) until onsets.size) {
                val deltaMs = onsets[j] - onsets[i]
                if (deltaMs <= 0) continue
                if (deltaMs > 2000L) break // > ~30 BPM apart, not a useful interval
                var bpm = 60_000.0 / deltaMs
                // Fold doubles/halves into the same octave (60-180 BPM) so
                // "beat every other beat" doesn't split the vote.
                while (bpm < 60.0) bpm *= 2.0
                while (bpm > 180.0) bpm /= 2.0
                val bin = bpm.roundToInt()
                bpmBins[bin] = (bpmBins[bin] ?: 0) + 1
            }
        }
        if (bpmBins.isEmpty()) return BpmEstimate(0f, 0f)

        val totalVotes = bpmBins.values.sum()
        val (bestBin, bestVotes) = bpmBins.maxByOrNull { it.value } ?: return BpmEstimate(0f, 0f)
        val confidence = (bestVotes.toFloat() / totalVotes).coerceIn(0f, 1f)
        return BpmEstimate(bestBin.toFloat(), confidence)
    }

    fun resetForTrack() {
        analyzedFrames = 0L
        reported = false
        frameAccumulator = 0.0
        framesInWindow = 0
        rollingAverage = 0.0
        onsetTimesMs.clear()
        elapsedMs = 0L
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer {
        val output = outputBuffer
        outputBuffer = EMPTY_BUFFER
        return output
    }

    override fun isEnded(): Boolean = inputEnded && outputBuffer === EMPTY_BUFFER

    @Deprecated("Deprecated in AudioProcessor")
    override fun flush() {
        outputBuffer = EMPTY_BUFFER
        inputEnded = false
    }

    @Deprecated("Deprecated in AudioProcessor")
    override fun reset() {
        flush()
        sampleRate = 0
        channelCount = 0
        bytesPerSample = 0
        resetForTrack()
    }

    private fun replaceOutputBuffer(size: Int): ByteBuffer {
        if (outputBuffer.capacity() < size) {
            outputBuffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder())
        } else {
            outputBuffer.clear()
        }
        return outputBuffer
    }

    companion object {
        private val EMPTY_BUFFER: ByteBuffer = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder())
    }
}
