package com.eyecare.pro

object CffStimulusTiming {
    fun halfCycleMillis(frequencyHz: Float): Long {
        require(frequencyHz.isFinite() && frequencyHz > 0f) { "Frekuensi harus positif dan finite." }
        return (500f / frequencyHz).toLong().coerceAtLeast(16L)
    }
}
