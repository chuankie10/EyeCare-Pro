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
    data class Unavailable(val message: String) : IshiharaUiState
    data class Question(
        val sourceTitle: String,
        val currentQuestion: Int,
        val totalQuestions: Int,
        val plate: IshiharaPlate
    ) : IshiharaUiState
    data class Completed(val sourceTitle: String, val score: IshiharaScore) : IshiharaUiState
}

class IshiharaViewModel(
    private val repository: IshiharaDatasetRepository = NoLicensedIshiharaDatasetRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow<IshiharaUiState>(
        IshiharaUiState.Unavailable(DEFAULT_UNAVAILABLE_MESSAGE)
    )
    val uiState: StateFlow<IshiharaUiState> = _uiState.asStateFlow()
    private var activeDataset: IshiharaDataset? = null
    private val answersByPlateId = linkedMapOf<String, String>()

    init {
        startOrShowUnavailable()
    }

    fun startOrShowUnavailable() {
        val dataset = repository.loadVerifiedDataset()?.takeIf { it.isEligibleForUse() }
        if (dataset == null) {
            activeDataset = null
            answersByPlateId.clear()
            _uiState.value = IshiharaUiState.Unavailable(DEFAULT_UNAVAILABLE_MESSAGE)
            return
        }
        activeDataset = dataset
        answersByPlateId.clear()
        showQuestion(dataset, 0)
    }

    fun submitAnswer(answer: String) {
        val question = _uiState.value as? IshiharaUiState.Question ?: return
        val dataset = activeDataset ?: return
        val unsure = answer.equals(IshiharaScoringEngine.UNSURE_ANSWER, ignoreCase = true)
        val listed = question.plate.answerOptions.any {
            it.trim().equals(answer.trim(), ignoreCase = true)
        }
        if (answer.isBlank() || (!unsure && !listed)) return

        answersByPlateId[question.plate.id] = answer
        val nextIndex = question.currentQuestion
        if (nextIndex >= dataset.plates.size) {
            _uiState.value = IshiharaUiState.Completed(
                sourceTitle = dataset.sourceTitle,
                score = IshiharaScoringEngine.score(dataset.plates, answersByPlateId)
            )
        } else {
            showQuestion(dataset, nextIndex)
        }
    }

    fun abortBecauseAssetCannotBeLoaded() {
        activeDataset = null
        answersByPlateId.clear()
        _uiState.value = IshiharaUiState.Unavailable(ASSET_UNAVAILABLE_MESSAGE)
    }

    private fun showQuestion(dataset: IshiharaDataset, index: Int) {
        _uiState.value = IshiharaUiState.Question(
            sourceTitle = dataset.sourceTitle,
            currentQuestion = index + 1,
            totalQuestions = dataset.plates.size,
            plate = dataset.plates[index]
        )
    }

    private companion object {
        const val DEFAULT_UNAVAILABLE_MESSAGE =
            "Materi tes belum tersedia. Pelat Ishihara asli, izin distribusi digital, identitas edisi, kunci jawaban, dan bukti validasi penyajian layar belum disediakan."
        const val ASSET_UNAVAILABLE_MESSAGE =
            "Aset pelat tidak dapat dimuat. Sesi dibatalkan dan jawaban tidak dinilai."
    }
}
