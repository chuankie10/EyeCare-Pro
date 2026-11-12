package com.eyecare.pro.data.ishihara

import com.eyecare.pro.domain.ishihara.IshiharaDataset
import com.eyecare.pro.domain.ishihara.IshiharaDatasetRepository

/**
 * Safe production default until the project owner supplies a licensed plate set,
 * its exact edition, answer key, and supporting validation documents.
 */
class NoLicensedIshiharaDatasetRepository : IshiharaDatasetRepository {
    override fun loadVerifiedDataset(): IshiharaDataset? = null
}
