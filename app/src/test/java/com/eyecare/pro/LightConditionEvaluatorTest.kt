package com.eyecare.pro

import org.junit.Assert.assertEquals
import org.junit.Test

class LightConditionEvaluatorTest {
    @Test
    fun screenWorkUsesItsOwnGuidanceBand() {
        assertEquals(LightCondition.TOO_DARK, LightConditionEvaluator.evaluate(299.9f, LightTask.SCREEN_WORK))
        assertEquals(LightCondition.IDEAL, LightConditionEvaluator.evaluate(300f, LightTask.SCREEN_WORK))
        assertEquals(LightCondition.IDEAL, LightConditionEvaluator.evaluate(500f, LightTask.SCREEN_WORK))
        assertEquals(LightCondition.TOO_BRIGHT, LightConditionEvaluator.evaluate(500.1f, LightTask.SCREEN_WORK))
    }

    @Test
    fun readingUsesItsOwnGuidanceBand() {
        assertEquals(LightCondition.TOO_DARK, LightConditionEvaluator.evaluate(499.9f, LightTask.READING))
        assertEquals(LightCondition.IDEAL, LightConditionEvaluator.evaluate(500f, LightTask.READING))
        assertEquals(LightCondition.IDEAL, LightConditionEvaluator.evaluate(750f, LightTask.READING))
        assertEquals(LightCondition.TOO_BRIGHT, LightConditionEvaluator.evaluate(750.1f, LightTask.READING))
    }

    @Test(expected = IllegalArgumentException::class)
    fun nonFiniteValuesAreRejected() {
        LightConditionEvaluator.evaluate(Float.NaN)
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeLuxIsRejected() {
        LightConditionEvaluator.evaluate(-1f)
    }
}
