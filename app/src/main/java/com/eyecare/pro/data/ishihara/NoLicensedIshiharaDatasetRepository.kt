package com.eyecare.pro.data.ishihara

import com.eyecare.pro.domain.ishihara.IshiharaDataset
import com.eyecare.pro.domain.ishihara.IshiharaDatasetRepository

/**
 * Safe default until the project owner supplies a licensed, verified plate set and its
 * edition-specific scoring instructions. No placeholder or pseudo-Ishihara images are generated.
 */
class NoLicensedIshiharaDatasetRepository : IshiharaDatasetRepository {
    override fun loadVerifiedDataset(): IshiharaDataset? = null
}
