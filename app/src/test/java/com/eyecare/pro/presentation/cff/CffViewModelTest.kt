package com.eyecare.pro.presentation.cff

import com.eyecare.pro.domain.cff.FlickerPerception
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CffViewModelTest {

    private fun acknowledgedViewModel(): CffViewModel =
        CffViewModel().apply {
            setWarningAcknowledged(true)
            setNoKnownPhotosensitivityRiskConfirmed(true)
        }

    @Test
    fun startRequiresBothSafetyAcknowledgements() {
        val viewModel = CffViewModel()

        viewModel.setWarningAcknowledged(true)
        viewModel.start(reportedDisplayRefreshRateHz = 60f)
        assertFalse(viewModel.uiState.value.isRunning)

        viewModel.setNoKnownPhotosensitivityRiskConfirmed(true)
        viewModel.start(reportedDisplayRefreshRateHz = 60f)
        assertTrue(viewModel.uiState.value.isRunning)
    }

    @Test
    fun invalidRefreshRateDoesNotStartStimulus() {
        val viewModel = acknowledgedViewModel()

        viewModel.start(reportedDisplayRefreshRateHz = Float.NaN)

        assertFalse(viewModel.uiState.value.isRunning)
        assertNull(viewModel.uiState.value.lastStimulusFrequencyHz)
    }

    @Test
    fun startClampsTargetToTheoreticalLimitFromReportedRefreshRate() {
        val viewModel = acknowledgedViewModel()
        viewModel.setTargetFrequency(50f)

        viewModel.start(reportedDisplayRefreshRateHz = 60f)

        assertTrue(viewModel.uiState.value.isRunning)
        assertEquals(30f, viewModel.uiState.value.maximumFrequencyHz, 0.001f)
        assertEquals(30f, viewModel.uiState.value.lastStimulusFrequencyHz!!, 0.001f)
    }

    @Test
    fun refreshRateLimitDropStopsAnUnsupportedRunningStimulus() {
        val viewModel = acknowledgedViewModel()
        viewModel.setTargetFrequency(40f)
        viewModel.start(reportedDisplayRefreshRateHz = 120f)
        assertTrue(viewModel.uiState.value.isRunning)

        viewModel.updateFrequencyLimit(maximumFrequencyHz = 30f)

        val state = viewModel.uiState.value
        assertFalse(state.isRunning)
        assertFalse(state.awaitingObservation)
        assertNull(state.lastStimulusFrequencyHz)
        assertNull(state.lastStimulusRefreshRateHz)
        assertTrue(state.statusMessage.orEmpty().contains("dihentikan"))
    }

    @Test
    fun stoppedStimulusCanRecordSubjectiveObservationWithReportedRefreshRate() {
        val viewModel = acknowledgedViewModel()
        viewModel.setTargetFrequency(20f)
        viewModel.start(reportedDisplayRefreshRateHz = 60f)
        viewModel.stop()

        val observation = viewModel.recordObservation(FlickerPerception.APPEARS_FUSED)

        assertNotNull(observation)
        assertEquals(20f, observation!!.requestedFrequencyHz, 0.001f)
        assertEquals(60f, observation.reportedDisplayRefreshRateHz, 0.001f)
        assertEquals(FlickerPerception.APPEARS_FUSED, observation.perception)
        assertFalse(viewModel.uiState.value.awaitingObservation)
        assertEquals(observation, viewModel.uiState.value.lastObservation)
    }

    @Test
    fun automaticStopPreservesObservationStepAndExplainsWhyItStopped() {
        val viewModel = acknowledgedViewModel()
        viewModel.start(reportedDisplayRefreshRateHz = 60f)

        viewModel.stopAutomatically()

        val state = viewModel.uiState.value
        assertFalse(state.isRunning)
        assertTrue(state.awaitingObservation)
        assertTrue(state.automaticallyStopped)
        assertTrue(state.statusMessage.orEmpty().contains("10 detik"))
    }

    @Test
    fun lifecycleCancellationClearsStimulusAndDoesNotResumeIt() {
        val viewModel = acknowledgedViewModel()
        viewModel.start(reportedDisplayRefreshRateHz = 60f)

        viewModel.cancelSession()

        val state = viewModel.uiState.value
        assertFalse(state.isRunning)
        assertFalse(state.awaitingObservation)
        assertNull(state.lastStimulusFrequencyHz)
        assertNull(state.lastStimulusRefreshRateHz)
        assertFalse(state.automaticallyStopped)
    }
}
