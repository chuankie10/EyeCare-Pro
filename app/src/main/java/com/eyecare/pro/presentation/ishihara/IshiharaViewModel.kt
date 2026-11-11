package com.eyecare.pro.presentation.ishihara

import androidx.lifecycle.ViewModel
import com.eyecare.pro.data.ishihara.NoLicensedIshiharaDatasetRepository
import com.eyecare.pro.domain.ishihara.IshiharaDataset
import com.eyecare.pro.domain.ishihara.IshiharaDatasetRepository
import com.eyecare.pro.domain.ishihara.IshiharaPlate
import com.eyecare.pro.domain.ishihara.IshiharaScore
import com.eyecare.pro.domain.ishihara.IshiharaScoringEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface IshiharaUiState {
    data object Unavailable : IshiharaUiState

    data class Question(
        val sourceTitle: String,
        val currentQuestion: Int,
        val totalQuestions: Int,
        val plate: IshiharaPlate
    ) : IshiharaUiState

    data class Completed(
        val sourceTitle: String,
        val score: IshiharaScore
    ) : IshiharaUiState
}

/**
 * Presentation state for the answer flow. The default repository deliberately returns no content,
 * so the screen stays unavailable until real, reviewed content is supplied.
 */
class IshiharaViewModel(
    private val repository: IshiharaDatasetRepository = NoLicensedIshiharaDatasetRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow<IshiharaUiState>(IshiharaUiState.Unavailable)
    val uiState: StateFlow<IshiharaUiState> = _uiState.asStateFlow()

    private var activeDataset: IshiharaDataset? = null
    private val answersByPlateId = linkedMapOf<String, String>()

    init {
        startOrShowUnavailable()
    }

    fun startOrShowUnavailable() {
        val dataset = repository.loadVerifiedDataset()
            ?.takeIf { it.isEligibleForUse() }

        if (dataset == null) {
            activeDataset = null
            answersByPlateId.clear()
            _uiState.value = IshiharaUiState.Unavailable
            return
        }

        activeDataset = dataset
        answersByPlateId.clear()
        showQuestion(dataset, index = 0)
    }

    fun submitAnswer(answer: String) {
        val question = _uiState.value as? IshiharaUiState.Question ?: return
        val dataset = activeDataset ?: return

        val isUnsure = answer == IshiharaScoringEngine.UNSURE_ANSWER
        val isListedOption = question.plate.answerOptions.any {
            it.trim().equals(answer.trim(), ignoreCase = true)
        }
        if (!isUnsure && !isListedOption) return

        answersByPlateId[question.plate.id] = answer
        val nextIndex = question.currentQuestion
        if (nextIndex >= dataset.plates.size) {
            _uiState.value = IshiharaUiState.Completed(
                sourceTitle = dataset.sourceTitle,
                score = IshiharaScoringEngine.score(dataset.plates, answersByPlateId)
            )
        } else {
            showQuestion(dataset, index = nextIndex)
        }
    }

    fun abortBecauseAssetCannotBeLoaded() {
        activeDataset = null
        answersByPlateId.clear()
        _uiState.value = IshiharaUiState.Unavailable
    }

    private fun showQuestion(dataset: IshiharaDataset, index: Int) {
        _uiState.value = IshiharaUiState.Question(
            sourceTitle = dataset.sourceTitle,
            currentQuestion = index + 1,
            totalQuestions = dataset.plates.size,
            plate = dataset.plates[index]
        )
    }
}
