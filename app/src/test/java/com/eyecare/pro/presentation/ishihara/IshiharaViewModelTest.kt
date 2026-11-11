package com.eyecare.pro.presentation.ishihara

import com.eyecare.pro.domain.ishihara.IshiharaDataset
import com.eyecare.pro.domain.ishihara.IshiharaDatasetRepository
import com.eyecare.pro.domain.ishihara.IshiharaPlate
import com.eyecare.pro.domain.ishihara.IshiharaScoringEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IshiharaViewModelTest {
    private val fixtureDataset = IshiharaDataset(
        id = "unit-test-fixture",
        sourceTitle = "Unit-test fixture only",
        licenseReference = "not-a-license-reference-fixture",
        validationReference = "not-a-clinical-validation-fixture",
        rightsConfirmedByMaintainer = true,
        validationConfirmedByMaintainer = true,
        plates = listOf(
            IshiharaPlate(
                id = "fixture-1",
                imageAssetPath = "not-a-real-test-image",
                answerOptions = listOf("12", "21"),
                acceptedAnswers = setOf("12")
            ),
            IshiharaPlate(
                id = "fixture-2",
                imageAssetPath = "not-a-real-test-image",
                answerOptions = listOf("3", "8"),
                acceptedAnswers = setOf("8")
            )
        )
    )

    @Test
    fun answerFlowAdvancesAndFinishesWithRawScoreOnly() {
        val repository = object : IshiharaDatasetRepository {
            override fun loadVerifiedDataset(): IshiharaDataset = fixtureDataset
        }
        val viewModel = IshiharaViewModel(repository)

        val first = viewModel.uiState.value as IshiharaUiState.Question
        assertEquals(1, first.currentQuestion)

        viewModel.submitAnswer("12")
        val second = viewModel.uiState.value as IshiharaUiState.Question
        assertEquals(2, second.currentQuestion)

        viewModel.submitAnswer(IshiharaScoringEngine.UNSURE_ANSWER)
        val completed = viewModel.uiState.value as IshiharaUiState.Completed
        assertEquals(1, completed.score.correctAnswers)
        assertEquals(2, completed.score.answeredQuestions)
        assertEquals(2, completed.score.totalQuestions)
    }

    @Test
    fun unavailableRepositoryDoesNotStartAnEmptyOrSyntheticTest() {
        val viewModel = IshiharaViewModel()
        assertTrue(viewModel.uiState.value is IshiharaUiState.Unavailable)
    }
}
