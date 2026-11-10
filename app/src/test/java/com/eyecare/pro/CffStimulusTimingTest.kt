package com.eyecare.pro

import org.junit.Assert.assertEquals
import org.junit.Test

class CffStimulusTimingTest {
    @Test
    fun computesHalfCycleForFiveHertz() {
        assertEquals(100L, CffStimulusTiming.halfCycleMillis(5f))
    }

    @Test
    fun capsHalfCycleAtDisplayFriendlyMinimum() {
        assertEquals(16L, CffStimulusTiming.halfCycleMillis(30f))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsZeroFrequency() {
        CffStimulusTiming.halfCycleMillis(0f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNanFrequency() {
        CffStimulusTiming.halfCycleMillis(Float.NaN)
    }
}
