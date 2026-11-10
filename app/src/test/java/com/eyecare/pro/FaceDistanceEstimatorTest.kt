package com.eyecare.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FaceDistanceEstimatorTest {
    @Test
    fun sameEyeLandmarkSpacingReturnsCalibrationDistance() {
        assertEquals(40f, FaceDistanceEstimator.estimateCentimeters(120f, 120f, 40f)!!, 0.001f)
    }

    @Test
    fun doubleEyeLandmarkSpacingMeansHalfTheDistance() {
        assertEquals(20f, FaceDistanceEstimator.estimateCentimeters(240f, 120f, 40f)!!, 0.001f)
    }

    @Test
    fun halfEyeLandmarkSpacingMeansDoubleTheDistance() {
        assertEquals(80f, FaceDistanceEstimator.estimateCentimeters(60f, 120f, 40f)!!, 0.001f)
    }

    @Test
    fun invalidInputsAreRejected() {
        assertNull(FaceDistanceEstimator.estimateCentimeters(0f, 120f, 40f))
        assertNull(FaceDistanceEstimator.estimateCentimeters(120f, -1f, 40f))
        assertNull(FaceDistanceEstimator.estimateCentimeters(120f, 120f, Float.NaN))
    }
}
