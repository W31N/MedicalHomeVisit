// ProtocolViewModelFactory.kt
package com.example.medicalhomevisit.ui.protocol

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.medicalhomevisit.data.repository.FakeProtocolRepository
import com.example.medicalhomevisit.data.repository.FakeVisitRepository

class ProtocolViewModelFactory(
    private val visitId: String
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProtocolViewModel::class.java)) {
            return ProtocolViewModel(
                protocolRepository = FakeProtocolRepository(),
                visitRepository = FakeVisitRepository(),
                visitId = visitId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}