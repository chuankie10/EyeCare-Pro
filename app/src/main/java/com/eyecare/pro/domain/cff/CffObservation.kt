package com.eyecare.pro.domain.cff

enum class FlickerPerception(val displayLabel: String) {
    STILL_VISIBLE("Masih terlihat berkedip"),
    APPEARS_FUSED("Tampak menyatu"),
    UNCERTAIN("Tidak yakin")
}

/**
 * A subjective observation at a user-selected setting, not a measured clinical CFF threshold.
 */
data class CffObservation(
    val requestedFrequencyHz: Float,
    val reportedDisplayRefreshRateHz: Float,
    val perception: FlickerPerception
)
