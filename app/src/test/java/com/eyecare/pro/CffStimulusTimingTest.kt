package com.eyecare.pro

import org.junit.Assert.assertEquals
import org.junit.Test

class CffStimulusTimingTest {
    @Test
    fun computesHalfCycleForFiveHertz() {
        assertEquals(100_000_000L, CffStimulusTiming.halfCycleNanos(5f))
    }

    @Test
    fun computesHalfCycleForThirtyHertzWithoutTruncatingToMilliseconds() {
        assertEquals(16_666_667L, CffStimulusTiming.halfCycleNanos(30f))
    }

    @Test
    fun sixtyHertzDisplayHasThirtyHertzTheoreticalAlternationLimit() {
        assertEquals(30f, CffStimulusTiming.maxAlternatingFrequencyHz(60f), 0.001f)
    }

    @Test
    fun ninetyHertzDisplayHasFortyFiveHertzTheoreticalAlternationLimit() {
        assertEquals(45f, CffStimulusTiming.maxAlternatingFrequencyHz(90f), 0.001f)
    }

    @Test
    fun appCapsTheoreticalLimitAtSixtyHertz() {
        assertEquals(60f, CffStimulusTiming.maxAlternatingFrequencyHz(144f), 0.001f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsZeroFrequency() {
        CffStimulusTiming.halfCycleNanos(0f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNanFrequency() {
        CffStimulusTiming.halfCycleNanos(Float.NaN)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidDisplayRefreshRate() {
        CffStimulusTiming.maxAlternatingFrequencyHz(Float.POSITIVE_INFINITY)
    }
}
