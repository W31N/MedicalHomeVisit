// com/example/medicalhomevisit/data/model/AppointmentRequest.kt
package com.example.medicalhomevisit.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "appointment_requests")
data class AppointmentRequest(
    @PrimaryKey val id: String = "",
    val patientId: String,
    val requestType: RequestType,
    val symptoms: String,
    val preferredDate: Date?, // Предпочтительная дата визита (может быть null для срочных)
    val preferredTimeRange: String?, // Например, "Утро", "После обеда" и т.д.
    val address: String, // Адрес для визита
    val additionalNotes: String?, // Дополнительная информация от пациента
    val status: RequestStatus = RequestStatus.NEW,
    val assignedStaffId: String? = null, // ID назначенного медработника
    val responseMessage: String? = null, // Сообщение от медучреждения при изменении статуса
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

enum class RequestType {
    EMERGENCY, // Неотложный визит
    REGULAR,   // Плановый визит
    CONSULTATION // Консультация
}

enum class RequestStatus {
    NEW,       // Новая заявка
    PENDING,   // На рассмотрении
    ASSIGNED,  // Назначен медработник
    SCHEDULED, // Запланирован визит (создан Visit)
    COMPLETED, // Выполнено
    CANCELLED  // Отменено
}