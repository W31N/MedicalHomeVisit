package com.example.medicalhomevisit.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.medicalhomevisit.data.repository.FirebaseAdminRepository
import com.example.medicalhomevisit.data.repository.FirebaseAppointmentRequestRepository
import com.example.medicalhomevisit.data.repository.FirebaseAuthRepository


class AdminViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminViewModel::class.java)) {
            return AdminViewModel(
                adminRepository = FirebaseAdminRepository(),
                appointmentRequestRepository = FirebaseAppointmentRequestRepository(),
                authRepository = FirebaseAuthRepository()
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}