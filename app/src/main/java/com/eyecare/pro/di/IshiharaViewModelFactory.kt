package com.eyecare.pro.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.eyecare.pro.domain.ishihara.IshiharaDatasetRepository
import com.eyecare.pro.presentation.ishihara.IshiharaViewModel

/** Composition-root factory: injects a data implementation into a presentation ViewModel. */
class IshiharaViewModelFactory(
    private val repository: IshiharaDatasetRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(IshiharaViewModel::class.java)) {
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
        @Suppress("UNCHECKED_CAST")
        return IshiharaViewModel(repository) as T
    }
}
