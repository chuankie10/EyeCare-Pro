package com.eyecare.pro.presentation.cff

import com.eyecare.pro.domain.cff.FlickerPerception
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CffViewModelTest {
    @Test fun startRequiresSafetyAcknowledgement() {
        val viewModel = CffViewModel()
        viewModel.start(60f)
        assertFalse(viewModel.uiState.value.isRunning)
    }

    @Test fun startCapturesFrequencyAndRefreshRateAndStopAllowsObservation() {
        val viewModel = CffViewModel()
        viewModel.updateFrequencyLimit(45f)
        viewModel.setTargetFrequency(18f)
        viewModel.setWarningAcknowledged(true)
        viewModel.start(90f)
        assertTrue(viewModel.uiState.value.isRunning)
        assertEquals(18f, viewModel.uiState.value.lastStimulusFrequencyHz!!, 0.001f)
        assertEquals(90f, viewModel.uiState.value.lastStimulusRefreshRateHz!!, 0.001f)

        viewModel.stop()
        val observation = viewModel.recordObservation(FlickerPerception.STILL_VISIBLE)
        assertEquals(18f, observation!!.requestedFrequencyHz, 0.001f)
        assertEquals(90f, observation.reportedDisplayRefreshRateHz, 0.001f)
        assertEquals(FlickerPerception.STILL_VISIBLE, observation.perception)
        assertFalse(viewModel.uiState.value.awaitingObservation)
    }

    @Test fun targetCannotChangeWhileStimulusOrObservationIsPending() {
        val viewModel = CffViewModel()
        viewModel.setWarningAcknowledged(true)
        viewModel.setTargetFrequency(18f)
        viewModel.start(60f)
        viewModel.setTargetFrequency(24f)
        assertEquals(18f, viewModel.uiState.value.targetFrequencyHz, 0.001f)
        viewModel.stop()
        viewModel.setTargetFrequency(30f)
        assertEquals(18f, viewModel.uiState.value.targetFrequencyHz, 0.001f)
        viewModel.recordObservation(FlickerPerception.APPEARS_FUSED)
        viewModel.setTargetFrequency(24f)
        assertEquals(24f, viewModel.uiState.value.targetFrequencyHz, 0.001f)
    }

    @Test fun startClampsTargetToTheoreticalDisplayLimit() {
        val viewModel = CffViewModel()
        viewModel.setWarningAcknowledged(true)
        viewModel.setTargetFrequency(55f)
        viewModel.start(60f)
        assertEquals(30f, viewModel.uiState.value.targetFrequencyHz, 0.001f)
    }

    @Test fun cancelSessionStopsStimulusAndClearsPendingObservation() {
        val viewModel = CffViewModel()
        viewModel.setWarningAcknowledged(true)
        viewModel.start(60f)
        viewModel.stop()
        viewModel.cancelSession()
        assertFalse(viewModel.uiState.value.isRunning)
        assertFalse(viewModel.uiState.value.awaitingObservation)
        assertNull(viewModel.uiState.value.lastStimulusFrequencyHz)
    }

    @Test fun cannotStartAgainUntilPendingObservationIsRecorded() {
        val viewModel = CffViewModel()
        viewModel.setWarningAcknowledged(true)
        viewModel.start(60f)
        viewModel.stop()
        viewModel.start(60f)
        assertFalse(viewModel.uiState.value.isRunning)
        viewModel.recordObservation(FlickerPerception.UNCERTAIN)
        viewModel.start(60f)
        assertTrue(viewModel.uiState.value.isRunning)
    }

    @Test fun refreshRateDropBelowActiveStimulusCancelsTheRun() {
        val viewModel = CffViewModel()
        viewModel.setWarningAcknowledged(true)
        viewModel.setTargetFrequency(25f)
        viewModel.start(60f)
        assertTrue(viewModel.uiState.value.isRunning)
        viewModel.updateFrequencyLimit(20f)
        assertFalse(viewModel.uiState.value.isRunning)
        assertNull(viewModel.uiState.value.lastStimulusFrequencyHz)
    }
}
