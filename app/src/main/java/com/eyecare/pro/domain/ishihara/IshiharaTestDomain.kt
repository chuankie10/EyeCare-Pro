package com.eyecare.pro.domain.ishihara

/**
 * Metadata for one plate from a legally obtained, professionally validated edition.
 * The app must not generate substitute pseudo-Ishihara artwork.
 */
data class IshiharaPlate(
    val id: String,
    val imageAssetPath: String,
    val answerOptions: List<String>,
    val acceptedAnswers: Set<String>
)

data class IshiharaDataset(
    val id: String,
    val sourceTitle: String,
    val licenseReference: String,
    val validationReference: String,
    /**
     * Maintainer review gates only. These flags are not proof by themselves:
     * the referenced license and edition-specific validation documents must be retained.
     */
    val rightsConfirmedByMaintainer: Boolean,
    val validationConfirmedByMaintainer: Boolean,
    val plates: List<IshiharaPlate>
) {
    fun isEligibleForUse(): Boolean {
        if (!rightsConfirmedByMaintainer || !validationConfirmedByMaintainer) return false
        if (id.isBlank() || sourceTitle.isBlank()) return false
        if (licenseReference.isBlank() || validationReference.isBlank()) return false
        if (plates.isEmpty() || plates.map { it.id }.distinct().size != plates.size) return false

        return plates.all { plate ->
            val normalizedOptions = plate.answerOptions.map { it.trim().lowercase() }
            plate.id.isNotBlank() &&
                plate.imageAssetPath.isNotBlank() &&
                normalizedOptions.isNotEmpty() &&
                normalizedOptions.all { it.isNotBlank() } &&
                normalizedOptions.distinct().size == normalizedOptions.size &&
                plate.acceptedAnswers.isNotEmpty() &&
                plate.acceptedAnswers.all { accepted ->
                    accepted.isNotBlank() && plate.answerOptions.any {
                        it.trim().equals(accepted.trim(), ignoreCase = true)
                    }
                }
        }
    }
}

/**
 * Raw response summary only. No normal/abnormal or diagnostic classification is derived.
 */
data class IshiharaScore(
    val correctAnswers: Int,
    val incorrectAnswers: Int,
    val unsureAnswers: Int,
    val unansweredQuestions: Int,
    val totalQuestions: Int
) {
    val answeredQuestions: Int
        get() = correctAnswers + incorrectAnswers + unsureAnswers
}

object IshiharaScoringEngine {
    const val UNSURE_ANSWER = "__UNSURE__"

    fun score(
        plates: List<IshiharaPlate>,
        answersByPlateId: Map<String, String>
    ): IshiharaScore {
        require(plates.isNotEmpty()) { "Dataset Ishihara tidak boleh kosong." }
        val plateIds = plates.map { it.id }
        require(plateIds.all { it.isNotBlank() }) { "ID pelat wajib diisi." }
        require(plateIds.distinct().size == plateIds.size) { "ID pelat harus unik." }
        require(answersByPlateId.keys.all { it in plateIds }) {
            "Jawaban memuat ID pelat yang tidak ada di dataset."
        }
        require(plates.all { it.acceptedAnswers.isNotEmpty() }) {
            "Setiap pelat harus memiliki kunci jawaban edisi yang ditinjau."
        }

        var correct = 0
        var incorrect = 0
        var unsure = 0
        var unanswered = 0

        plates.forEach { plate ->
            val answer = answersByPlateId[plate.id]?.trim().orEmpty()
            when {
                answer.isEmpty() -> unanswered++
                answer.equals(UNSURE_ANSWER, ignoreCase = true) -> unsure++
                else -> {
                    require(plate.answerOptions.any {
                        it.trim().equals(answer, ignoreCase = true)
                    }) { "Jawaban tidak termasuk opsi untuk pelat ${plate.id}." }
                    if (plate.acceptedAnswers.any {
                            it.trim().equals(answer, ignoreCase = true)
                        }
                    ) {
                        correct++
                    } else {
                        incorrect++
                    }
                }
            }
        }

        return IshiharaScore(
            correctAnswers = correct,
            incorrectAnswers = incorrect,
            unsureAnswers = unsure,
            unansweredQuestions = unanswered,
            totalQuestions = plates.size
        )
    }
}

interface IshiharaDatasetRepository {
    /** Returns content only after licensing and edition-specific validation have been reviewed. */
    fun loadVerifiedDataset(): IshiharaDataset?
}
