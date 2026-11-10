package com.eyecare.pro

enum class LightCondition {
    TOO_DARK,
    IDEAL,
    TOO_BRIGHT
}

object LightConditionEvaluator {
    fun evaluate(lux: Float): LightCondition {
        require(lux.isFinite()) { "Nilai lux harus berupa angka finite." }
        return when {
            lux < 100f -> LightCondition.TOO_DARK
            lux <= 400f -> LightCondition.IDEAL
            else -> LightCondition.TOO_BRIGHT
        }
    }
}
