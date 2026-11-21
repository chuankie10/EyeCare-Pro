package com.eyecare.pro.domain.cff

enum class FlickerPerception(val displayLabel: String) {
    STILL_VISIBLE("Masih terlihat berkedip"),
    APPEARS_FUSED("Tampak menyatu"),
    UNCERTAIN("Tidak yakin")
}

/**
 * Subjective response at a user-selected target, not a measured clinical CFF threshold.
 *
 * The requested frequency is checked against the theoretical software limit implied by the
 * reported refresh rate. This does not validate the physical light output of the display.
 */
data class CffObservation(
    val requestedFrequencyHz: Float,
    val reportedDisplayRefreshRateHz: Float,
    val perception: FlickerPerception
) {
    init {
        require(requestedFrequencyHz.isFinite() && requestedFrequencyHz > 0f) {
            "Frekuensi target harus positif dan finite."
        }
        require(reportedDisplayRefreshRateHz.isFinite() && reportedDisplayRefreshRateHz > 0f) {
            "Refresh rate yang dilaporkan harus positif dan finite."
        }
        val theoreticalLimitHz =
            CffStimulusTiming.maxAlternatingFrequencyHz(reportedDisplayRefreshRateHz)
        require(requestedFrequencyHz <= theoreticalLimitHz + FREQUENCY_TOLERANCE_HZ) {
            "Frekuensi target melebihi batas teoritis refresh rate yang dilaporkan."
        }
    }

    private companion object {
        const val FREQUENCY_TOLERANCE_HZ = 0.001f
    }
}
