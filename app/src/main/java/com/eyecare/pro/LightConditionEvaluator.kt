package com.eyecare.pro

enum class LightCondition {
    TOO_DARK,
    IDEAL,
    TOO_BRIGHT
}

/** Activity-specific guidance bands; these are workplace lighting references, not medical thresholds. */
enum class LightTask(val label: String, val lowerLux: Float, val upperLux: Float) {
    SCREEN_WORK("Pekerjaan dominan layar", 300f, 500f),
    READING("Membaca / menulis", 500f, 750f)
}

object LightConditionEvaluator {
    fun evaluate(lux: Float, task: LightTask = LightTask.SCREEN_WORK): LightCondition {
        require(lux.isFinite() && lux >= 0f) { "Nilai lux harus finite dan tidak negatif." }
        return when {
            lux < task.lowerLux -> LightCondition.TOO_DARK
            lux <= task.upperLux -> LightCondition.IDEAL
            else -> LightCondition.TOO_BRIGHT
        }
    }
}
