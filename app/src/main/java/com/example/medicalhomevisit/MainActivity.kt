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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
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
        val auth = FirebaseAuth.getInstance()

        // Проверка и добавление администратора
        db.collection("users").whereEqualTo("role", "ADMIN").get()
            .addOnSuccessListener { adminsSnapshot ->
                if (adminsSnapshot.isEmpty) {
                    // Создаем учетную запись администратора в Firebase Auth
                    val adminEmail = "admin@medicalhomevisit.com"
                    val adminPassword = "Admin123"

                    auth.createUserWithEmailAndPassword(adminEmail, adminPassword)
                        .addOnSuccessListener { authResult ->
                            val adminUid = authResult.user?.uid ?: return@addOnSuccessListener

                            // Добавляем данные администратора в коллекцию users
                            val adminData = hashMapOf(
                                "email" to adminEmail,
                                "displayName" to "Администратор Системы",
                                "role" to "ADMIN",
                                "isActive" to true,
                                "createdAt" to FieldValue.serverTimestamp()
                            )

                            db.collection("users").document(adminUid).set(adminData)
                                .addOnSuccessListener {
                                    Log.d("Firebase", "Administrator account created with ID: $adminUid")
                                    Log.d("Firebase", "Admin credentials: Email: $adminEmail, Password: $adminPassword")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("Firebase", "Error adding administrator data", e)
                                }
                        }
                        .addOnFailureListener { e ->
                            // Проверка, если ошибка связана с тем, что email уже используется
                            if (e is FirebaseAuthUserCollisionException) {
                                Log.d("Firebase", "Admin email already exists, trying to login and update role")
                                // Можно добавить код для обновления роли существующего пользователя
                            } else {
                                Log.e("Firebase", "Error creating administrator account", e)
                            }
                        }
                } else {
                    // Администратор уже существует
                    val admin = adminsSnapshot.documents.first()
                    Log.d("Firebase", "Administrator already exists with ID: ${admin.id}")
                }
            }

        // Добавляем тестового медработника
        db.collection("users").whereEqualTo("role", "MEDICAL_STAFF").get()
            .addOnSuccessListener { staffSnapshot ->
                if (staffSnapshot.isEmpty) {
                    // Создаем учетную запись медработника в Firebase Auth
                    val staffEmail = "doctor@medicalhomevisit.com"
                    val staffPassword = "Doctor123"

                    auth.createUserWithEmailAndPassword(staffEmail, staffPassword)
                        .addOnSuccessListener { authResult ->
                            val staffUid = authResult.user?.uid ?: return@addOnSuccessListener

                            // Добавляем данные медработника в коллекцию users
                            val staffData = hashMapOf(
                                "email" to staffEmail,
                                "displayName" to "Петров Иван Сергеевич",
                                "role" to "MEDICAL_STAFF",
                                "isActive" to true,
                                "specialization" to "Терапевт",
                                "createdAt" to FieldValue.serverTimestamp()
                            )

                            db.collection("users").document(staffUid).set(staffData)
                                .addOnSuccessListener {
                                    Log.d("Firebase", "Medical staff account created with ID: $staffUid")

                                    // Добавляем подробную информацию в коллекцию medical_staff
                                    val staffDetails = hashMapOf(
                                        "displayName" to "Петров Иван Сергеевич",
                                        "specialization" to "Терапевт",
                                        "phoneNumber" to "+7 (999) 765-43-21",
                                        "licenseNumber" to "МК-123456",
                                        "experience" to 5, // лет опыта
                                        "isAvailable" to true,
                                        "createdAt" to FieldValue.serverTimestamp()
                                    )

                                    db.collection("medical_staff").document(staffUid).set(staffDetails)
                                        .addOnSuccessListener {
                                            Log.d("Firebase", "Medical staff details added")
                                            Log.d("Firebase", "Staff credentials: Email: $staffEmail, Password: $staffPassword")
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("Firebase", "Error adding medical staff details", e)
                                        }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("Firebase", "Error adding medical staff data", e)
                                }
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firebase", "Error creating medical staff account", e)
                        }
                } else {
                    // Медработник уже существует
                    val staff = staffSnapshot.documents.first()
                    Log.d("Firebase", "Medical staff already exists with ID: ${staff.id}")
                }
            }

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