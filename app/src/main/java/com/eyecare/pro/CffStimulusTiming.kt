package com.eyecare.pro

import kotlin.math.roundToLong

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
}
