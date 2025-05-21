package com.example.medicalhomevisit.ui.protocol

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.medicalhomevisit.data.repository.FirebaseProtocolRepository
import com.example.medicalhomevisit.data.repository.FirebaseVisitRepository

class ProtocolViewModelFactory(private val visitId: String) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProtocolViewModel::class.java)) {
            return ProtocolViewModel(
                visitId = visitId,
                protocolRepository = FirebaseProtocolRepository(),
                visitRepository = FirebaseVisitRepository()
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}