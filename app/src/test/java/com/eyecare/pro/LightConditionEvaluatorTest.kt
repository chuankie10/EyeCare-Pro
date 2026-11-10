package com.eyecare.pro

import org.junit.Assert.assertEquals
import org.junit.Test

class LightConditionEvaluatorTest {
    @Test
    fun valuesBelowOperationalRangeAreTooDark() {
        assertEquals(LightCondition.TOO_DARK, LightConditionEvaluator.evaluate(0f))
        assertEquals(LightCondition.TOO_DARK, LightConditionEvaluator.evaluate(99.9f))
    }

    @Test
    fun rangeBoundariesAreIncluded() {
        assertEquals(LightCondition.IDEAL, LightConditionEvaluator.evaluate(100f))
        assertEquals(LightCondition.IDEAL, LightConditionEvaluator.evaluate(400f))
    }

    @Test
    fun valuesAboveOperationalRangeAreTooBright() {
        assertEquals(LightCondition.TOO_BRIGHT, LightConditionEvaluator.evaluate(400.1f))
        assertEquals(LightCondition.TOO_BRIGHT, LightConditionEvaluator.evaluate(1000f))
    }

    @Test(expected = IllegalArgumentException::class)
    fun nonFiniteValuesAreRejected() {
        LightConditionEvaluator.evaluate(Float.NaN)
    }
}
