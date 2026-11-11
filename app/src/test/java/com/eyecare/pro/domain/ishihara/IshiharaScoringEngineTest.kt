package com.eyecare.pro.domain.ishihara

import org.junit.Assert.assertEquals
import org.junit.Test

class IshiharaScoringEngineTest {
    private val fixturePlates = listOf(
        IshiharaPlate(
            id = "fixture-plate-1",
            imageAssetPath = "not-a-real-test-image",
            answerOptions = listOf("12", "21"),
            acceptedAnswers = setOf("12")
        ),
        IshiharaPlate(
            id = "fixture-plate-2",
            imageAssetPath = "not-a-real-test-image",
            answerOptions = listOf("3", "8"),
            acceptedAnswers = setOf("8")
        )
    )

    @Test
    fun countsCorrectAndIncorrectAnswers() {
        val score = IshiharaScoringEngine.score(
            plates = fixturePlates,
            answersByPlateId = mapOf(
                "fixture-plate-1" to "12",
                "fixture-plate-2" to "3"
            )
        )

        assertEquals(1, score.correctAnswers)
        assertEquals(2, score.answeredQuestions)
        assertEquals(2, score.totalQuestions)
        assertEquals(0, score.unansweredQuestions)
    }

    @Test
    fun countsUnansweredAndUnsureResponsesWithoutTreatingThemAsCorrect() {
        val score = IshiharaScoringEngine.score(
            plates = fixturePlates,
            answersByPlateId = mapOf(
                "fixture-plate-1" to IshiharaScoringEngine.UNSURE_ANSWER
            )
        )

        assertEquals(0, score.correctAnswers)
        assertEquals(1, score.answeredQuestions)
        assertEquals(1, score.unansweredQuestions)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsDuplicatePlateIds() {
        IshiharaScoringEngine.score(
            plates = listOf(fixturePlates.first(), fixturePlates.first()),
            answersByPlateId = emptyMap()
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsAnswersForUnknownPlateIds() {
        IshiharaScoringEngine.score(
            plates = fixturePlates,
            answersByPlateId = mapOf("unknown-plate" to "12")
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsAnEmptyDataset() {
        IshiharaScoringEngine.score(emptyList(), emptyMap())
    }
}
