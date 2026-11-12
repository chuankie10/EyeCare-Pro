package com.eyecare.pro.presentation.ishihara

import com.eyecare.pro.domain.ishihara.IshiharaDataset
import com.eyecare.pro.domain.ishihara.IshiharaDatasetRepository
import com.eyecare.pro.domain.ishihara.IshiharaPlate
import com.eyecare.pro.domain.ishihara.IshiharaScoringEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IshiharaViewModelTest {
    private val fixture = IshiharaDataset(
        id = "unit-test-fixture",
        sourceTitle = "Unit-test fixture only — not a real test edition",
        licenseReference = "fixture-license",
        validationReference = "fixture-validation",
        rightsConfirmedByMaintainer = true,
        validationConfirmedByMaintainer = true,
        plates = listOf(
            IshiharaPlate("p1", "fixture/p1.png", listOf("12", "21"), setOf("12")),
            IshiharaPlate("p2", "fixture/p2.png", listOf("3", "8"), setOf("8"))
        )
    )

    @Test fun answerFlowAdvancesAndFinishesWithRawCountsOnly() {
        val repository = object : IshiharaDatasetRepository {
            override fun loadVerifiedDataset() = fixture
        }
        val viewModel = IshiharaViewModel(repository)
        assertEquals(1, (viewModel.uiState.value as IshiharaUiState.Question).currentQuestion)
        viewModel.submitAnswer("12")
        assertEquals(2, (viewModel.uiState.value as IshiharaUiState.Question).currentQuestion)
        viewModel.submitAnswer(IshiharaScoringEngine.UNSURE_ANSWER)
        val completed = viewModel.uiState.value as IshiharaUiState.Completed
        assertEquals(1, completed.score.correctAnswers)
        assertEquals(1, completed.score.unsureAnswers)
        assertEquals(2, completed.score.answeredQuestions)
        assertEquals(2, completed.score.totalQuestions)
    }

    @Test fun invalidAnswerDoesNotAdvanceQuestion() {
        val repository = object : IshiharaDatasetRepository {
            override fun loadVerifiedDataset() = fixture
        }
        val viewModel = IshiharaViewModel(repository)
        viewModel.submitAnswer("not-an-option")
        assertEquals(1, (viewModel.uiState.value as IshiharaUiState.Question).currentQuestion)
    }

    @Test fun defaultRepositoryKeepsTestUnavailable() {
        assertTrue(IshiharaViewModel().uiState.value is IshiharaUiState.Unavailable)
    }
}
