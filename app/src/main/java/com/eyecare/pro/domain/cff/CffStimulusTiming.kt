package com.eyecare.pro.domain.cff

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

    /** A black/white cycle needs at least two display refreshes; this is a theoretical bound only. */
    fun maxAlternatingFrequencyHz(displayRefreshRateHz: Float): Float {
        require(displayRefreshRateHz.isFinite() && displayRefreshRateHz > 0f) {
            "Refresh rate harus positif dan finite."
        }
        return minOf(displayRefreshRateHz / 2f, APP_MAX_FREQUENCY_HZ)
    }

    /** Carries fractional frame time forward instead of rounding every half-cycle the same way. */
    fun advanceFramePhase(
        accumulatedNanos: Long,
        frameDeltaNanos: Long,
        halfCycleNanos: Long
    ): FramePhaseStep {
        require(accumulatedNanos >= 0L) { "Akumulasi waktu tidak boleh negatif." }
        require(frameDeltaNanos >= 0L) { "Durasi frame tidak boleh negatif." }
        require(halfCycleNanos > 0L) { "Durasi setengah siklus harus positif." }
        require(Long.MAX_VALUE - accumulatedNanos >= frameDeltaNanos) {
            "Akumulasi waktu melebihi batas."
        }
        val totalNanos = accumulatedNanos + frameDeltaNanos
        val completedHalfCycles = totalNanos / halfCycleNanos
        return FramePhaseStep(
            shouldToggle = completedHalfCycles % 2L == 1L,
            remainderNanos = totalNanos % halfCycleNanos,
            completedHalfCycles = completedHalfCycles
        )
    }
}
