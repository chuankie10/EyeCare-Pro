package com.eyecare.pro.domain.ishihara

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the safety gates and raw-response scoring contract.
 * The fixtures below are synthetic test metadata only; they are not Ishihara plates.
 */
class IshiharaDatasetEligibilityTest {

    private fun plate(
        id: String = "plate-1",
        options: List<String> = listOf("12", "21"),
        accepted: Set<String> = setOf("12")
    ) = IshiharaPlate(
        id = id,
        imageAssetPath = "test-fixtures/$id.png",
        answerOptions = options,
        acceptedAnswers = accepted
    )

    private fun dataset(
        rights: Boolean = true,
        validation: Boolean = true,
        licenseReference: String = "fixture-license-reference",
        validationReference: String = "fixture-validation-reference",
        plates: List<IshiharaPlate> = listOf(plate())
    ) = IshiharaDataset(
        id = "unit-test-only",
        sourceTitle = "Synthetic metadata fixture; not a clinical edition",
        licenseReference = licenseReference,
        validationReference = validationReference,
        rightsConfirmedByMaintainer = rights,
        validationConfirmedByMaintainer = validation,
        plates = plates
    )

    @Test
    fun validReviewedMetadataPassesEligibilityGate() {
        assertTrue(dataset().isEligibleForUse())
    }

    @Test
    fun missingDistributionRightsFailsClosed() {
        assertFalse(dataset(rights = false).isEligibleForUse())
    }

    @Test
    fun missingEditionValidationFailsClosed() {
        assertFalse(dataset(validation = false).isEligibleForUse())
    }

    @Test
    fun blankLicenseReferenceFailsClosed() {
        assertFalse(dataset(licenseReference = "  ").isEligibleForUse())
    }

    @Test
    fun blankValidationReferenceFailsClosed() {
        assertFalse(dataset(validationReference = "").isEligibleForUse())
    }

    @Test
    fun duplicatePlateIdsAreRejected() {
        assertFalse(dataset(plates = listOf(plate("same"), plate("same"))).isEligibleForUse())
    }

    @Test
    fun duplicateAnswerOptionsAreRejected() {
        assertFalse(
            dataset(plates = listOf(plate(options = listOf("12", " 12 "), accepted = setOf("12"))))
                .isEligibleForUse()
        )
    }

    @Test
    fun acceptedAnswerMustBeOneOfTheDisplayedOptions() {
        assertFalse(
            dataset(plates = listOf(plate(options = listOf("12", "21"), accepted = setOf("8"))))
                .isEligibleForUse()
        )
    }

    @Test
    fun reservedUnsureTokenCannotBeDisplayedAsAnAnswerOption() {
        assertFalse(
            dataset(
                plates = listOf(
                    plate(
                        options = listOf("12", IshiharaScoringEngine.UNSURE_ANSWER),
                        accepted = setOf("12")
                    )
                )
            ).isEligibleForUse()
        )
    }

    @Test
    fun scoringReportsRawCountsWithoutClinicalClassification() {
        val plates = listOf(
            plate("one", accepted = setOf("12")),
            plate("two", options = listOf("3", "8"), accepted = setOf("8")),
            plate("three", options = listOf("5", "6"), accepted = setOf("5")),
            plate("four", options = listOf("7", "9"), accepted = setOf("7"))
        )

        val score = IshiharaScoringEngine.score(
            plates,
            mapOf(
                "one" to "12",
                "two" to "3",
                "three" to IshiharaScoringEngine.UNSURE_ANSWER
            )
        )

        assertEquals(1, score.correctAnswers)
        assertEquals(1, score.incorrectAnswers)
        assertEquals(1, score.unsureAnswers)
        assertEquals(1, score.unansweredQuestions)
        assertEquals(3, score.answeredQuestions)
        assertEquals(4, score.totalQuestions)
    }

    @Test(expected = IllegalArgumentException::class)
    fun scoringRejectsAnswerNotInDisplayedOptions() {
        IshiharaScoringEngine.score(
            listOf(plate()),
            mapOf("plate-1" to "not-an-option")
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun scoringRejectsAnswerForUnknownPlate() {
        IshiharaScoringEngine.score(
            listOf(plate()),
            mapOf("not-a-plate" to "12")
        )
    }
}
