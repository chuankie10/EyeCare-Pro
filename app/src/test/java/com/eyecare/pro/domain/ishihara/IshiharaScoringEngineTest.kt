package com.eyecare.pro.domain.ishihara

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IshiharaScoringEngineTest {
    private val plates = listOf(
        IshiharaPlate("p1", "fixture/p1.png", listOf("12", "21"), setOf("12")),
        IshiharaPlate("p2", "fixture/p2.png", listOf("3", "8"), setOf("8")),
        IshiharaPlate("p3", "fixture/p3.png", listOf("5", "6"), setOf("6"))
    )

    @Test fun countsCorrectIncorrectUnsureAndUnansweredSeparately() {
        val score = IshiharaScoringEngine.score(
            plates,
            mapOf("p1" to "12", "p2" to "3", "p3" to IshiharaScoringEngine.UNSURE_ANSWER)
        )
        assertEquals(1, score.correctAnswers)
        assertEquals(1, score.incorrectAnswers)
        assertEquals(1, score.unsureAnswers)
        assertEquals(0, score.unansweredQuestions)
        assertEquals(3, score.answeredQuestions)
        assertEquals(3, score.totalQuestions)
    }

    @Test fun countsMissingAnswersAsUnanswered() {
        val score = IshiharaScoringEngine.score(plates, mapOf("p1" to "12"))
        assertEquals(1, score.correctAnswers)
        assertEquals(0, score.incorrectAnswers)
        assertEquals(0, score.unsureAnswers)
        assertEquals(2, score.unansweredQuestions)
    }

    @Test fun datasetRequiresRightsAndValidationReviewFlags() {
        val dataset = IshiharaDataset(
            "fixture", "Unit-test fixture only", "fixture-license", "fixture-validation",
            true, true, plates
        )
        assertTrue(dataset.isEligibleForUse())
        assertFalse(dataset.copy(rightsConfirmedByMaintainer = false).isEligibleForUse())
        assertFalse(dataset.copy(validationConfirmedByMaintainer = false).isEligibleForUse())
    }

    @Test(expected = IllegalArgumentException::class) fun rejectsDuplicatePlateIds() {
        IshiharaScoringEngine.score(listOf(plates.first(), plates.first()), emptyMap())
    }

    @Test(expected = IllegalArgumentException::class) fun rejectsUnknownPlateIds() {
        IshiharaScoringEngine.score(plates, mapOf("unknown" to "12"))
    }

    @Test(expected = IllegalArgumentException::class) fun rejectsAnswerOutsideOptions() {
        IshiharaScoringEngine.score(plates, mapOf("p1" to "not-an-option"))
    }

    @Test(expected = IllegalArgumentException::class) fun rejectsEmptyDataset() {
        IshiharaScoringEngine.score(emptyList(), emptyMap())
    }
}
