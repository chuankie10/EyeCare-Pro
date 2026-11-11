package com.eyecare.pro

import kotlin.math.roundToLong

data class FramePhaseStep(
    val shouldToggle: Boolean,
    val remainderNanos: Long,
    val completedHalfCycles: Long
)

object CffStimulusTiming {
    const val APP_MAX_FREQUENCY_HZ = 60f

    fun halfCycleNanos(frequencyHz: Float): Long {
        require(frequencyHz.isFinite() && frequencyHz > 0f) {
            "Frekuensi harus positif dan finite."
        }
        return (500_000_000.0 / frequencyHz.toDouble()).roundToLong().coerceAtLeast(1L)
    }

    /**
     * A black/white cycle needs at least two display refreshes. This is only a theoretical
     * upper bound; Compose frame delivery and panel behavior can lower the actual rate.
     */
    fun maxAlternatingFrequencyHz(displayRefreshRateHz: Float): Float {
        require(displayRefreshRateHz.isFinite() && displayRefreshRateHz > 0f) {
            "Refresh rate harus positif dan finite."
        }
        return minOf(displayRefreshRateHz / 2f, APP_MAX_FREQUENCY_HZ)
    }

    /**
     * Carries fractional frame time forward so non-integer frame intervals (for example,
     * 20 Hz on a 60 Hz display) are distributed over adjacent frames instead of always
     * rounding every half-cycle up to the same whole number of frames.
     */
    fun advanceFramePhase(
        accumulatedNanos: Long,
        frameDeltaNanos: Long,
        halfCycleNanos: Long
    ): FramePhaseStep {
        require(accumulatedNanos >= 0L) { "Akumulasi waktu tidak boleh negatif." }
        require(frameDeltaNanos >= 0L) { "Durasi frame tidak boleh negatif." }
        require(halfCycleNanos > 0L) { "Durasi setengah siklus harus positif." }

        val totalNanos = accumulatedNanos + frameDeltaNanos
        val completedHalfCycles = totalNanos / halfCycleNanos
        return FramePhaseStep(
            shouldToggle = completedHalfCycles % 2L == 1L,
            remainderNanos = totalNanos % halfCycleNanos,
            completedHalfCycles = completedHalfCycles
        )
    }
}
