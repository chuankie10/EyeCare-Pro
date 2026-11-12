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
    val lastObservation: CffObservation? = null
)

class CffViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CffUiState())
    val uiState: StateFlow<CffUiState> = _uiState.asStateFlow()

    fun updateFrequencyLimit(maximumFrequencyHz: Float) {
        if (!maximumFrequencyHz.isFinite() || maximumFrequencyHz <= 0f) return
        val current = _uiState.value
        val target = if (maximumFrequencyHz >= MIN_FREQUENCY_HZ) {
            current.targetFrequencyHz.coerceIn(MIN_FREQUENCY_HZ, maximumFrequencyHz)
        } else {
            maximumFrequencyHz
        }
        _uiState.value = current.copy(
            maximumFrequencyHz = maximumFrequencyHz,
            targetFrequencyHz = target
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
        if (!current.warningAcknowledged || current.isRunning || maximum < MIN_FREQUENCY_HZ) return
        val target = current.targetFrequencyHz.coerceIn(MIN_FREQUENCY_HZ, maximum)
        _uiState.value = current.copy(
            targetFrequencyHz = target,
            isRunning = true,
            awaitingObservation = false,
            lastStimulusFrequencyHz = target,
            lastStimulusRefreshRateHz = reportedDisplayRefreshRateHz,
            lastObservation = null
        )
    }

    fun stop() {
        val current = _uiState.value
        if (!current.isRunning) return
        _uiState.value = current.copy(isRunning = false, awaitingObservation = true)
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
            lastStimulusRefreshRateHz = null
        )
    }

    private companion object {
        const val MIN_FREQUENCY_HZ = 5f
    }
}
