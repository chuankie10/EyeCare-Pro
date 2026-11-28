package com.eyecare.pro.domain.cff

import org.junit.Assert.assertEquals
import org.junit.Test

class CffObservationTest {
    @Test
    fun acceptsTargetAtTheoreticalDisplayLimit() {
        val observation = CffObservation(
            requestedFrequencyHz = 30f,
            reportedDisplayRefreshRateHz = 60f,
            perception = FlickerPerception.STILL_VISIBLE
        )

        assertEquals(30f, observation.requestedFrequencyHz, 0.001f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsTargetAboveTheoreticalDisplayLimit() {
        CffObservation(
            requestedFrequencyHz = 30.1f,
            reportedDisplayRefreshRateHz = 60f,
            perception = FlickerPerception.APPEARS_FUSED
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonPositiveTargetFrequency() {
        CffObservation(
            requestedFrequencyHz = 0f,
            reportedDisplayRefreshRateHz = 60f,
            perception = FlickerPerception.UNCERTAIN
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonFiniteTargetFrequency() {
        CffObservation(
            requestedFrequencyHz = Float.NaN,
            reportedDisplayRefreshRateHz = 60f,
            perception = FlickerPerception.UNCERTAIN
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidReportedRefreshRate() {
        CffObservation(
            requestedFrequencyHz = 10f,
            reportedDisplayRefreshRateHz = Float.POSITIVE_INFINITY,
            perception = FlickerPerception.UNCERTAIN
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsTargetAboveApplicationCapOnHighRefreshDisplay() {
        CffObservation(
            requestedFrequencyHz = 60.1f,
            reportedDisplayRefreshRateHz = 144f,
            perception = FlickerPerception.UNCERTAIN
        )
    }
}
