package com.eyecare.pro.presentation.cff

import androidx.lifecycle.ViewModel
import com.eyecare.pro.domain.cff.CffObservation
import com.eyecare.pro.domain.cff.CffStimulusTiming
import com.eyecare.pro.domain.cff.FlickerPerception
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CffUiState(
    val targetFrequencyHz: Float = 12f,
    val maximumFrequencyHz: Float = 60f,
    val warningAcknowledged: Boolean = false,
    val isRunning: Boolean = false,
    val awaitingObservation: Boolean = false,
    val lastStimulusFrequencyHz: Float? = null,
    val lastStimulusRefreshRateHz: Float? = null,
    val lastObservation: CffObservation? = null,
    val automaticallyStopped: Boolean = false,
    val statusMessage: String? = null
)

class CffViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CffUiState())
    val uiState: StateFlow<CffUiState> = _uiState.asStateFlow()

    fun updateFrequencyLimit(maximumFrequencyHz: Float) {
        if (!maximumFrequencyHz.isFinite() || maximumFrequencyHz <= 0f) return
        val current = _uiState.value
        val stimulusExceedsNewLimit = current.isRunning &&
            (current.lastStimulusFrequencyHz ?: current.targetFrequencyHz) > maximumFrequencyHz
        val target = when {
            stimulusExceedsNewLimit && maximumFrequencyHz >= MIN_FREQUENCY_HZ ->
                current.targetFrequencyHz.coerceIn(MIN_FREQUENCY_HZ, maximumFrequencyHz)
            stimulusExceedsNewLimit -> maximumFrequencyHz
            current.isRunning || current.awaitingObservation -> current.targetFrequencyHz
            maximumFrequencyHz >= MIN_FREQUENCY_HZ ->
                current.targetFrequencyHz.coerceIn(MIN_FREQUENCY_HZ, maximumFrequencyHz)
            else -> maximumFrequencyHz
        }
        _uiState.value = current.copy(
            maximumFrequencyHz = maximumFrequencyHz,
            targetFrequencyHz = target,
            isRunning = current.isRunning && !stimulusExceedsNewLimit,
            awaitingObservation = current.awaitingObservation && !stimulusExceedsNewLimit,
            lastStimulusFrequencyHz = if (stimulusExceedsNewLimit) null else current.lastStimulusFrequencyHz,
            lastStimulusRefreshRateHz = if (stimulusExceedsNewLimit) null else current.lastStimulusRefreshRateHz,
            automaticallyStopped = if (stimulusExceedsNewLimit) false else current.automaticallyStopped,
            statusMessage = if (stimulusExceedsNewLimit) {
                "Stimulasi dihentikan karena batas refresh rate yang dilaporkan perangkat berubah."
            } else {
                current.statusMessage
            }
        )
    }

    fun setTargetFrequency(frequencyHz: Float) {
        val current = _uiState.value
        if (current.isRunning || current.awaitingObservation || !frequencyHz.isFinite()) return
        if (current.maximumFrequencyHz < MIN_FREQUENCY_HZ) return
        _uiState.value = current.copy(
            targetFrequencyHz = frequencyHz.coerceIn(MIN_FREQUENCY_HZ, current.maximumFrequencyHz)
        )
    }

    fun setWarningAcknowledged(acknowledged: Boolean) {
        val current = _uiState.value
        if (current.isRunning || current.awaitingObservation) return
        _uiState.value = current.copy(warningAcknowledged = acknowledged)
    }

    fun start(reportedDisplayRefreshRateHz: Float) {
        if (!reportedDisplayRefreshRateHz.isFinite() || reportedDisplayRefreshRateHz <= 0f) return
        val maximum = CffStimulusTiming.maxAlternatingFrequencyHz(reportedDisplayRefreshRateHz)
        updateFrequencyLimit(maximum)
        val current = _uiState.value
        if (!current.warningAcknowledged || current.isRunning || current.awaitingObservation ||
            maximum < MIN_FREQUENCY_HZ
        ) return
        val target = current.targetFrequencyHz.coerceIn(MIN_FREQUENCY_HZ, maximum)
        _uiState.value = current.copy(
            targetFrequencyHz = target,
            isRunning = true,
            awaitingObservation = false,
            lastStimulusFrequencyHz = target,
            lastStimulusRefreshRateHz = reportedDisplayRefreshRateHz,
            lastObservation = null,
            automaticallyStopped = false,
            statusMessage = null
        )
    }

    fun stop() {
        val current = _uiState.value
        if (!current.isRunning) return
        _uiState.value = current.copy(
            isRunning = false,
            awaitingObservation = true,
            automaticallyStopped = false,
            statusMessage = null
        )
    }

    /** Called by the UI's bounded trial timer; keeps the observation flow available. */
    fun stopAutomatically() {
        val current = _uiState.value
        if (!current.isRunning) return
        _uiState.value = current.copy(
            isRunning = false,
            awaitingObservation = true,
            automaticallyStopped = true,
            statusMessage = "Stimulasi otomatis dihentikan setelah batas 10 detik."
        )
    }

    fun recordObservation(perception: FlickerPerception): CffObservation? {
        val current = _uiState.value
        if (current.isRunning || !current.awaitingObservation) return null
        val frequency = current.lastStimulusFrequencyHz ?: return null
        val refreshRate = current.lastStimulusRefreshRateHz ?: return null
        val observation = CffObservation(
            requestedFrequencyHz = frequency,
            reportedDisplayRefreshRateHz = refreshRate,
            perception = perception
        )
        _uiState.value = current.copy(
            awaitingObservation = false,
            lastObservation = observation
        )
        return observation
    }

    fun cancelSession() {
        val current = _uiState.value
        _uiState.value = current.copy(
            isRunning = false,
            awaitingObservation = false,
            lastStimulusFrequencyHz = null,
            lastStimulusRefreshRateHz = null,
            automaticallyStopped = false,
            statusMessage = null
        )
    }

    private companion object {
        const val MIN_FREQUENCY_HZ = 5f
    }
}
