package com.eyecare.pro

object FaceDistanceEstimator {
    fun estimateCentimeters(
        currentEyeDistancePx: Float,
        calibratedEyeDistancePx: Float,
        calibratedDistanceCm: Float
    ): Float? {
        if (!currentEyeDistancePx.isFinite() || !calibratedEyeDistancePx.isFinite() ||
            !calibratedDistanceCm.isFinite()
        ) return null
        if (currentEyeDistancePx <= 0f || calibratedEyeDistancePx <= 0f || calibratedDistanceCm <= 0f) {
            return null
        }
        val estimate = calibratedDistanceCm * calibratedEyeDistancePx / currentEyeDistancePx
        return estimate.takeIf { it.isFinite() && it > 0f }
    }
}
