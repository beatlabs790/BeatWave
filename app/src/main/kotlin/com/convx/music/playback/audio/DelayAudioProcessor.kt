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
import kotlin.math.roundToInt

/**
 * Simple feedback delay line (16-bit PCM only) for the DJ-mix transition
 * effect chain — Android has no stock delay/echo AudioEffect the way it has
 * PresetReverb, so this fills that one gap. Transparent passthrough (echo
 * mix at 0) when [wetMix] is 0, which is the default and what every non-DJ-
 * mode player instance sits at.
 */
@UnstableApi
class DelayAudioProcessor(
    private val delayMs: Long = 220L,
    private val feedback: Float = 0.35f,
) : AudioProcessor {

    /** 0 = dry (no effect, default), up to ~0.6 for an audible slap-back echo. */
    @Volatile
    var wetMix: Float = 0f

    private var sampleRate = 0
    private var channelCount = 0

    private var delayBuffer: ShortArray = ShortArray(0)
    private var writeIndex = 0

    private var outputBuffer: ByteBuffer = EMPTY_BUFFER
    private var inputEnded = false

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        sampleRate = inputAudioFormat.sampleRate
        channelCount = inputAudioFormat.channelCount
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || sampleRate <= 0 || channelCount <= 0) {
            sampleRate = 0
            channelCount = 0
            return inputAudioFormat
        }
        val delayFrames = ((sampleRate * delayMs) / 1000L).toInt().coerceAtLeast(1)
        delayBuffer = ShortArray(delayFrames * channelCount)
        writeIndex = 0
        return inputAudioFormat
    }

    override fun isActive(): Boolean = sampleRate != 0 && channelCount != 0

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) {
            outputBuffer = EMPTY_BUFFER
            return
        }
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)

        val mix = wetMix
        val out = replaceOutputBuffer(inputBuffer.remaining())
        out.order(ByteOrder.LITTLE_ENDIAN)

        if (mix <= 0f || delayBuffer.isEmpty()) {
            out.put(inputBuffer)
            out.flip()
            return
        }

        val frameCount = inputBuffer.remaining() / 2 / channelCount
        val basePosition = inputBuffer.position()
        val bufferLen = delayBuffer.size

        repeat(frameCount) { frameIndex ->
            repeat(channelCount) { channelIndex ->
                val sampleIndex = basePosition + (frameIndex * channelCount + channelIndex) * 2
                val dry = inputBuffer.getShort(sampleIndex)
                val bufIdx = (writeIndex + channelIndex) % bufferLen
                val delayed = delayBuffer[bufIdx]

                val wet = (dry + delayed * feedback).roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                delayBuffer[bufIdx] = wet.toShort()

                val mixed = (dry * (1f - mix) + wet * mix).roundToInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                out.putShort(mixed.toShort())
            }
            writeIndex = (writeIndex + channelCount) % bufferLen
        }
        out.flip()
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
        delayBuffer.fill(0)
        writeIndex = 0
    }

    @Deprecated("Deprecated in AudioProcessor")
    override fun reset() {
        flush()
        sampleRate = 0
        channelCount = 0
        delayBuffer = ShortArray(0)
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
