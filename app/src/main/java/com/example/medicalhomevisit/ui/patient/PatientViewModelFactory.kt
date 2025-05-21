// com/example/medicalhomevisit/ui/patient/PatientViewModelFactory.kt
package com.example.medicalhomevisit.ui.patient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.medicalhomevisit.data.repository.FirebaseAppointmentRequestRepository
import com.example.medicalhomevisit.data.repository.FirebaseAuthRepository

class PatientViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PatientViewModel::class.java)) {
            return PatientViewModel(
                appointmentRequestRepository = FirebaseAppointmentRequestRepository(),
                authRepository = FirebaseAuthRepository()
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}