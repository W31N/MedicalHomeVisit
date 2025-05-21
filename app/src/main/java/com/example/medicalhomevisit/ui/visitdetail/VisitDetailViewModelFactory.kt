// VisitDetailViewModelFactory.kt
package com.example.medicalhomevisit.ui.visitdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.medicalhomevisit.data.repository.FakePatientRepository
import com.example.medicalhomevisit.data.repository.FakeVisitRepository

class VisitDetailViewModelFactory(
    private val visitId: String
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VisitDetailViewModel::class.java)) {
            return VisitDetailViewModel(
                visitRepository = FakeVisitRepository(),
                patientRepository = FakePatientRepository(),
                visitId = visitId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}