// MainActivity.kt
package com.example.medicalhomevisit

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.medicalhomevisit.ui.AppNavigation
import com.example.medicalhomevisit.ui.theme.MedicalHomeVisitTheme
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Calendar
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

            addTestData()

        setContent {
            MedicalHomeVisitTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }

    private fun addTestData() {
        val db = Firebase.firestore

        // Проверка наличия данных перед добавлением
        db.collection("patients").get()
            .addOnSuccessListener { patientsSnapshot ->
                if (patientsSnapshot.isEmpty) {
                    // Добавляем пациента
                    val patient = hashMapOf(
                        "fullName" to "Иванов Иван Иванович",
                        "dateOfBirth" to Timestamp(Date(94, 6, 12)), // 12 июля 1994
                        "gender" to "MALE",
                        "address" to "г. Москва, ул. Примерная, д. 1, кв. 1",
                        "phoneNumber" to "+7 (999) 123-45-67",
                        "policyNumber" to "2345678901234567",
                        "allergies" to listOf("Пыль")
                    )

                    db.collection("patients").add(patient)
                        .addOnSuccessListener { patientRef ->
                            Log.d("Firebase", "Patient added with ID: ${patientRef.id}")

                            // После добавления пациента проверяем наличие визитов
                            db.collection("visits").get()
                                .addOnSuccessListener { visitsSnapshot ->
                                    if (visitsSnapshot.isEmpty) {
                                        // Добавляем визит
                                        val currentTime = Calendar.getInstance().time
                                        val scheduledTime = Calendar.getInstance()
                                        scheduledTime.add(Calendar.HOUR, 2) // Визит через 2 часа

                                        val visit = hashMapOf(
                                            "patientId" to patientRef.id,
                                            "scheduledTime" to Timestamp(scheduledTime.time),
                                            "status" to "PLANNED",
                                            "address" to "г. Москва, ул. Примерная, д. 1, кв. 1",
                                            "reasonForVisit" to "Первичный осмотр",
                                            "notes" to "Пациент жалуется на головную боль",
                                            "createdAt" to FieldValue.serverTimestamp(),
                                            "updatedAt" to FieldValue.serverTimestamp()
                                        )

                                        db.collection("visits").add(visit)
                                            .addOnSuccessListener { visitRef ->
                                                Log.d("Firebase", "Visit added with ID: ${visitRef.id}")
                                            }
                                            .addOnFailureListener { e ->
                                                Log.e("Firebase", "Error adding visit", e)
                                            }
                                    }
                                }
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firebase", "Error adding patient", e)
                        }
                }
            }

        // Добавляем шаблоны протоколов
        db.collection("protocolTemplates").get()
            .addOnSuccessListener { templatesSnapshot ->
                if (templatesSnapshot.isEmpty) {
                    val template1 = hashMapOf(
                        "name" to "ОРВИ",
                        "description" to "Шаблон для острых респираторных вирусных инфекций",
                        "complaints" to "Повышение температуры тела, головная боль, боль в горле, насморк, общая слабость",
                        "anamnesis" to "Заболел(а) остро ... дней назад, когда появились вышеуказанные жалобы",
                        "objectiveStatus" to "Состояние удовлетворительное. Кожные покровы обычной окраски. Зев гиперемирован.",
                        "recommendations" to "Обильное теплое питье. Постельный режим. Парацетамол 500 мг при температуре выше 38.5°C.",
                        "requiredVitals" to listOf("temperature", "pulse", "blood_pressure")
                    )

                    db.collection("protocolTemplates").add(template1)
                        .addOnSuccessListener { documentReference ->
                            Log.d("Firebase", "Template added with ID: ${documentReference.id}")
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firebase", "Error adding template", e)
                        }
                }
            }
    }
}