package com.example.medicalhomevisit.ui.visitlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.medicalhomevisit.data.repository.FirebaseVisitRepository

class VisitListViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VisitListViewModel::class.java)) {
            return VisitListViewModel(
                visitRepository = FirebaseVisitRepository()
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}