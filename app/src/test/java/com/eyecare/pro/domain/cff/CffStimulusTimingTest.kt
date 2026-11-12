package com.eyecare.pro.domain.cff

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CffStimulusTimingTest {
    @Test fun computesHalfCycleForFiveHertz() {
        assertEquals(100_000_000L, CffStimulusTiming.halfCycleNanos(5f))
    }

    @Test fun computesHalfCycleForThirtyHertzWithoutMillisecondTruncation() {
        assertEquals(16_666_667L, CffStimulusTiming.halfCycleNanos(30f))
    }

    @Test fun sixtyHertzDisplayHasThirtyHertzTheoreticalAlternationLimit() {
        assertEquals(30f, CffStimulusTiming.maxAlternatingFrequencyHz(60f), 0.001f)
    }

    @Test fun ninetyHertzDisplayHasFortyFiveHertzTheoreticalAlternationLimit() {
        assertEquals(45f, CffStimulusTiming.maxAlternatingFrequencyHz(90f), 0.001f)
    }

    @Test fun appCapsTheoreticalLimitAtSixtyHertz() {
        assertEquals(60f, CffStimulusTiming.maxAlternatingFrequencyHz(144f), 0.001f)
    }

    @Test fun carriesFractionalFrameIntervalsForward() {
        val first = CffStimulusTiming.advanceFramePhase(0L, 16_666_667L, 25_000_000L)
        assertFalse(first.shouldToggle)
        assertEquals(16_666_667L, first.remainderNanos)
        val second = CffStimulusTiming.advanceFramePhase(first.remainderNanos, 16_666_667L, 25_000_000L)
        assertTrue(second.shouldToggle)
        assertEquals(8_333_334L, second.remainderNanos)
        val third = CffStimulusTiming.advanceFramePhase(second.remainderNanos, 16_666_667L, 25_000_000L)
        assertTrue(third.shouldToggle)
        assertEquals(1L, third.remainderNanos)
    }

    @Test fun skippedFramesPreserveToggleParity() {
        val step = CffStimulusTiming.advanceFramePhase(0L, 50_000_000L, 20_000_000L)
        assertFalse(step.shouldToggle)
        assertEquals(2L, step.completedHalfCycles)
        assertEquals(10_000_000L, step.remainderNanos)
    }

    @Test(expected = IllegalArgumentException::class) fun rejectsZeroFrequency() {
        CffStimulusTiming.halfCycleNanos(0f)
    }

    @Test(expected = IllegalArgumentException::class) fun rejectsNanFrequency() {
        CffStimulusTiming.halfCycleNanos(Float.NaN)
    }

    @Test(expected = IllegalArgumentException::class) fun rejectsInvalidDisplayRefreshRate() {
        CffStimulusTiming.maxAlternatingFrequencyHz(Float.POSITIVE_INFINITY)
    }

    @Test(expected = IllegalArgumentException::class) fun rejectsTimeAccumulatorOverflow() {
        CffStimulusTiming.advanceFramePhase(Long.MAX_VALUE, 1L, 1L)
    }
}
