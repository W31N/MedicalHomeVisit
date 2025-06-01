//package com.example.medicalhomevisit.ui.visitdetail
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.ViewModelProvider
//import com.example.medicalhomevisit.data.repository.FirebaseAppointmentRequestRepository
//import com.example.medicalhomevisit.data.repository.FirebasePatientRepository
//import com.example.medicalhomevisit.data.repository.FirebaseProtocolRepository
//import com.example.medicalhomevisit.data.repository.FirebaseVisitRepository
//
//class VisitDetailViewModelFactory(
//    private val visitId: String
//) : ViewModelProvider.Factory {
//
//    @Suppress("UNCHECKED_CAST")
//    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//        if (modelClass.isAssignableFrom(VisitDetailViewModel::class.java)) {
//            return VisitDetailViewModel(
//                visitId = visitId,
//                visitRepository = FirebaseVisitRepository(),
//                appointmentRequestRepository = FirebaseAppointmentRequestRepository(),
//                protocolRepository = FirebaseProtocolRepository(),
//                patientRepository = FirebasePatientRepository()
//            ) as T
//        }
//        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
//    }
//}