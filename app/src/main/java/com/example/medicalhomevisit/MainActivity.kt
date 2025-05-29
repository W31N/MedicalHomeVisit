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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import java.util.Date
import java.util.UUID

// Импортируйте ваши data классы и enums, если они в другом пакете
// import com.example.medicalhomevisit.data.model.*

// Предполагается, что ваши data классы и enums выглядят примерно так:
// (Это для контекста, используйте ваши реальные определения)
enum class UserRole { ADMIN, MEDICAL_STAFF, DISPATCHER, PATIENT }
enum class Gender { MALE, FEMALE, UNKNOWN }
enum class RequestType { EMERGENCY, REGULAR, CONSULTATION }
enum class RequestStatus { NEW, PENDING, ASSIGNED, SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED }
enum class VisitStatus { PLANNED, IN_PROGRESS, COMPLETED, CANCELLED }
enum class UrgencyLevel { LOW, NORMAL, HIGH, CRITICAL }

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        db = Firebase.firestore
//        auth = FirebaseAuth.getInstance()
//        addTestData() // Вызов функции добавления тестовых данных

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

    private fun createFirebaseUser(email: String, pass: String, displayName: String, role: UserRole, additionalData: Map<String, Any>? = null, onSuccess: (uid: String) -> Unit) {
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid ?: return@addOnSuccessListener
                Log.d("Firebase", "Auth user created: $uid for role $role")

                val userData = hashMapOf<String, Any?>(
                    "email" to email,
                    "displayName" to displayName,
                    "role" to role.name, // Сохраняем enum как строку
                    "isActive" to true,
                    "isEmailVerified" to false, // Обычно false при создании
                    "createdAt" to FieldValue.serverTimestamp()
                )
                additionalData?.let {
                    // Если additionalData может содержать null значения, убедитесь, что это безопасно
                    // или что additionalData также имеет тип Map<String, Any?>
                    userData.putAll(it.mapValues { entry -> entry.value as Any? }) // Явное приведение для безопасности
                }

                db.collection("users").document(uid).set(userData)
                    .addOnSuccessListener {
                        Log.d("Firebase", "User document created in Firestore for $uid ($role)")
                        onSuccess(uid)
                    }
                    .addOnFailureListener { e -> Log.e("Firebase", "Error adding user $uid ($role) to Firestore", e) }
            }
            .addOnFailureListener { e ->
                if (e is FirebaseAuthUserCollisionException) {
                    Log.w("Firebase", "Auth user $email already exists. Attempting to find existing Firestore doc.")
                    // Пытаемся найти существующего пользователя, чтобы получить UID для дальнейших действий
                    auth.signInWithEmailAndPassword(email, pass).addOnSuccessListener { existingAuthResult ->
                        val uid = existingAuthResult.user?.uid
                        if (uid != null) {
                            Log.d("Firebase", "Found existing auth user $uid for role $role")
                            onSuccess(uid) // Передаем UID существующего пользователя
                        } else {
                            Log.e("Firebase", "Could not retrieve UID for existing user $email", e)
                        }
                    }.addOnFailureListener { signInError ->
                        Log.e("Firebase", "Error signing in existing user $email", signInError)
                    }
                } else {
                    Log.e("Firebase", "Error creating Auth user for $role", e)
                }
            }
    }


    private fun addAdminUserIfNotExists() {
        db.collection("users").whereEqualTo("role", UserRole.ADMIN.name).limit(1).get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    createFirebaseUser("admin@medicalhomevisit.com", "Admin123", "Главный Администратор", UserRole.ADMIN) { uid ->
                        Log.d("Firebase", "Admin account $uid processed.")
                        Log.d("FirebaseTestCredentials", "Admin: admin@medicalhomevisit.com / Admin123")
                    }
                } else {
                    Log.d("Firebase", "Admin user already exists: ${querySnapshot.documents.first().id}")
                }
            }
    }

    private fun addMedicalStaffUsersIfNotExists() {
        db.collection("users").whereEqualTo("role", UserRole.MEDICAL_STAFF.name).limit(2).get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.documents.size < 2) { // Добавляем, если меньше 2х медработников
                    val staff1Data = mapOf("specialization" to "Терапевт")
                    createFirebaseUser("doctor1@medicalhomevisit.com", "Doctor123", "Петров Иван Сергеевич", UserRole.MEDICAL_STAFF, staff1Data) { uid ->
                        val staffDetails = hashMapOf(
                            // "userId" to uid, // user_id уже является ID документа
                            "displayName" to "Петров Иван Сергеевич",
                            "specialization" to "Терапевт",
                            "phoneNumber" to "+79991112233",
                            "experienceYears" to 10,
                            "isAvailable" to true,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                        db.collection("medical_staff_details").document(uid).set(staffDetails)
                            .addOnSuccessListener { Log.d("Firebase", "Medical staff details added for $uid") }
                            .addOnFailureListener { e -> Log.e("Firebase", "Error adding medical staff details for $uid", e) }
                        Log.d("FirebaseTestCredentials", "Staff1: doctor1@medicalhomevisit.com / Doctor123")
                    }

                    val staff2Data = mapOf("specialization" to "Педиатр")
                    createFirebaseUser("doctor2@medicalhomevisit.com", "Doctor456", "Сидорова Мария Ивановна", UserRole.MEDICAL_STAFF, staff2Data) { uid ->
                        val staffDetails = hashMapOf(
                            "displayName" to "Сидорова Мария Ивановна",
                            "specialization" to "Педиатр",
                            "phoneNumber" to "+79994445566",
                            "experienceYears" to 5,
                            "isAvailable" to false, // Например, в отпуске
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                        db.collection("medical_staff_details").document(uid).set(staffDetails)
                            .addOnSuccessListener { Log.d("Firebase", "Medical staff details added for $uid") }
                            .addOnFailureListener { e -> Log.e("Firebase", "Error adding medical staff details for $uid", e) }
                        Log.d("FirebaseTestCredentials", "Staff2: doctor2@medicalhomevisit.com / Doctor456")
                    }
                } else {
                    querySnapshot.documents.forEach { Log.d("Firebase", "Medical staff user already exists: ${it.id}") }
                }
            }
    }

    private fun addDispatcherUserIfNotExists() {
        db.collection("users").whereEqualTo("role", UserRole.DISPATCHER.name).limit(1).get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    createFirebaseUser("dispatcher@medicalhomevisit.com", "Dispatcher123", "Диспетчер Анна", UserRole.DISPATCHER) { uid ->
                        Log.d("Firebase", "Dispatcher account $uid processed.")
                        Log.d("FirebaseTestCredentials", "Dispatcher: dispatcher@medicalhomevisit.com / Dispatcher123")
                    }
                } else {
                    Log.d("Firebase", "Dispatcher user already exists: ${querySnapshot.documents.first().id}")
                }
            }
    }


    private fun addPatientUsersAndProfilesIfNotExists() {
        // Добавляем 2-3 пациента, если их еще нет
        // Для простоты, будем проверять по email, хотя в реальном приложении нужна более надежная проверка
        val patientsToAdd = listOf(
            Triple("patient1@medicalhomevisit.com", "Patient123", "Иванов Иван Иванович"),
            Triple("patient2@medicalhomevisit.com", "Patient456", "Сергеева Ольга Петровна"),
            Triple("patient3@medicalhomevisit.com", "Patient789", "Алексеев Дмитрий Борисович")
        )

        val patientCommonData = mutableMapOf<String, String>() // Для хранения UIDов созданных пациентов

        patientsToAdd.forEachIndexed { index, (email, pass, name) ->
            db.collection("users").whereEqualTo("email", email).limit(1).get()
                .addOnSuccessListener { userSnapshot ->
                    if (userSnapshot.isEmpty) {
                        createFirebaseUser(email, pass, name, UserRole.PATIENT) { uid ->
                            Log.d("Firebase", "Patient auth user $uid processed for $name.")
                            Log.d("FirebaseTestCredentials", "Patient${index + 1}: $email / $pass")
                            patientCommonData["patient${index + 1}Uid"] = uid // Сохраняем UID

                            val calendarDob = Calendar.getInstance()
                            calendarDob.set(1980 + index * 5, Calendar.JANUARY + index, 1 + index*2)

                            val patientProfileData = hashMapOf(
                                // "userId" to uid, // user_id уже является ID документа users, а patients имеет свой ID
                                "fullName" to name,
                                "dateOfBirth" to Timestamp(calendarDob.time),
                                "gender" to if (index % 2 == 0) Gender.MALE.name else Gender.FEMALE.name,
                                "address" to "г. Москва, ул. Тестовая, д. ${index + 1}, кв. ${10 + index}",
                                "phoneNumber" to "+7900${1000000 + index}",
                                "policyNumber" to "POL${1000000000000000 + index}",
                                "allergies" to if (index == 0) listOf("Пенициллин", "Пыльца") else listOf(),
                                "chronicConditions" to if (index == 1) listOf("Гипертония") else listOf("Астма"),
                                "createdAt" to FieldValue.serverTimestamp()
                            )
                            // Документ пациента в коллекции "patients" может иметь свой ID или использовать UID пользователя
                            // Для примера будем использовать UID пользователя как ID документа в "patients"
                            // или генерировать новый и хранить uid пользователя внутри
                            // В ваших data классах Patient имеет свой id, и UserEntity там нет.
                            // Поэтому будем генерировать новый ID для Patient
                            val patientDocId = UUID.randomUUID().toString()
                            patientCommonData["patient${index + 1}ProfileId"] = patientDocId // Сохраняем ID профиля пациента

                            db.collection("patients").document(patientDocId).set(patientProfileData)
                                .addOnSuccessListener {
                                    Log.d("Firebase", "Patient profile added for $name with ID $patientDocId (User UID: $uid)")
                                    // После создания всех пациентов, добавляем заявки и визиты
                                    if (patientCommonData.filterKeys { it.endsWith("ProfileId") }.size == patientsToAdd.size) {
                                        addSampleAppointmentRequestsAndVisits(patientCommonData)
                                    }
                                }
                                .addOnFailureListener { e -> Log.e("Firebase", "Error adding patient profile for $name", e) }
                        }
                    } else {
                        val existingUserDoc = userSnapshot.documents.first()
                        val existingUid = existingUserDoc.id
                        patientCommonData["patient${index + 1}Uid"] = existingUid
                        // Проверим, есть ли профиль пациента для этого UID
                        db.collection("patients").whereEqualTo("fullName", name).limit(1).get() // Упрощенная проверка
                            .addOnSuccessListener { patientProfileSnapshot ->
                                if (patientProfileSnapshot.isEmpty) {
                                    // Профиль не найден, но пользователь Auth есть. Это странная ситуация.
                                    // Можно попробовать создать профиль, но это может нарушить консистентность.
                                    // Либо просто логируем.
                                    Log.w("Firebase", "Auth user $existingUid for $name exists, but no patient profile found by name. Skipping profile creation to avoid duplicates.")
                                } else {
                                    val existingPatientProfileId = patientProfileSnapshot.documents.first().id
                                    patientCommonData["patient${index + 1}ProfileId"] = existingPatientProfileId
                                    Log.d("Firebase", "Patient profile for $name already exists: $existingPatientProfileId (User UID: $existingUid)")
                                }
                                // После проверки всех пациентов, добавляем заявки и визиты
                                if (patientCommonData.filterKeys { it.endsWith("ProfileId") }.size == patientsToAdd.size) {
                                    addSampleAppointmentRequestsAndVisits(patientCommonData)
                                }
                            }
                    }
                }
        }
    }


    private fun addSampleProtocolTemplates() {
        db.collection("protocolTemplates").limit(1).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    val templates = listOf(
                        hashMapOf(
                            "name" to "Стандартный ОРВИ",
                            "description" to "Первичный осмотр при ОРВИ",
                            "complaintsTemplate" to "Жалобы на: повышение температуры до (__°C), насморк, кашель (сухой/влажный), боль в горле, общую слабость.",
                            "anamnesisTemplate" to "Заболел(а) остро (постепенно) __ дней назад. Началось с __. Лечение: __.",
                            "objectiveStatusTemplate" to "Общее состояние (удовл/сред.тяж/тяж). Сознание (ясное/спутанное). Кожные покровы (цвет, влажность, сыпь). Зев (гиперемия, налеты). Дыхание (везикулярное/жесткое, хрипы). ЧДД __ в мин. Тоны сердца (ясные/приглушены, ритм). ЧСС __ уд/мин. АД __/__ мм рт.ст. Живот (мягкий/напряжен, болезненность).",
                            "recommendationsTemplate" to "Режим (постельный/домашний). Диета (молочно-растительная, обильное питье). Лечение: [Список препаратов]. Повторный осмотр через __ дней.",
                            "requiredVitals" to listOf("Температура", "ЧСС", "АД", "ЧДД", "Сатурация SpO2")
                        ),
                        hashMapOf(
                            "name" to "Контрольный осмотр",
                            "description" to "Повторный осмотр после лечения",
                            "complaintsTemplate" to "На момент осмотра жалобы (активные/отсутствуют): __",
                            "anamnesisTemplate" to "Динамика состояния за последние __ дней (положительная/отрицательная/без динамики): __",
                            "objectiveStatusTemplate" to "Общее состояние (удовл/сред.тяж). Кожные покровы __. Зев __. Дыхание __. ЧДД __. Тоны сердца __. ЧСС __. АД __/__.",
                            "recommendationsTemplate" to "Продолжить/завершить лечение. Рекомендации: __. Открыть/продлить/закрыть л/н.",
                            "requiredVitals" to listOf("Температура", "АД")
                        )
                    )
                    templates.forEach { templateData ->
                        db.collection("protocolTemplates").add(templateData)
                            .addOnSuccessListener { Log.d("Firebase", "Protocol template '${templateData["name"]}' added.") }
                            .addOnFailureListener { e -> Log.e("Firebase", "Error adding protocol template", e) }
                    }
                } else {
                    Log.d("Firebase", "Protocol templates already exist.")
                }
            }
    }

    private fun addSampleAppointmentRequestsAndVisits(patientData: Map<String, String>) {
        // Получаем ID медработников для назначения
        db.collection("users").whereEqualTo("role", UserRole.MEDICAL_STAFF.name).get()
            .addOnSuccessListener { staffUsersSnapshot ->
                val staffIds = staffUsersSnapshot.documents.map { it.id }
                val staffDisplayNames = staffUsersSnapshot.documents.map { it.getString("displayName") ?: "Медработник" }

                if (staffIds.isEmpty()) {
                    Log.e("Firebase", "No medical staff found to assign requests/visits.")
                    return@addOnSuccessListener
                }

                // Получаем ID администратора/диспетчера для поля assignedBy
                db.collection("users").whereIn("role", listOf(UserRole.ADMIN.name, UserRole.DISPATCHER.name)).limit(1).get()
                    .addOnSuccessListener { assignerSnapshot ->
                        val assignerId = if (!assignerSnapshot.isEmpty) assignerSnapshot.documents.first().id else null

                        // Добавляем заявки только если их еще нет (упрощенная проверка)
                        db.collection("appointment_requests").limit(1).get()
                            .addOnSuccessListener { requestSnapshot ->
                                if (requestSnapshot.isEmpty) {
                                    val calendar = Calendar.getInstance()
                                    val requestsData = mutableListOf<Map<String, Any?>>()

                                    // Заявка 1 (пациент 1, назначена на staff1)
                                    calendar.time = Date()
                                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                                    calendar.set(Calendar.HOUR_OF_DAY, 10)
                                    val preferredTime1 = calendar.time
                                    requestsData.add(hashMapOf(
                                        "patientId" to (patientData["patient1ProfileId"] ?: "unknown_patient_id_1"),
                                        "patientName" to (patientData["patient1Name"] ?: "Пациент 1"),
                                        "patientPhone" to "+79001111111",
                                        "address" to "г. Москва, ул. Тестовая, д. 1, кв. 10",
                                        "requestType" to RequestType.REGULAR.name,
                                        "symptoms" to "Высокая температура, кашель",
                                        "preferredDate" to Timestamp(preferredTime1),
                                        "status" to RequestStatus.ASSIGNED.name,
                                        "assignedStaffId" to staffIds.getOrElse(0) { null },
                                        "assignedStaffName" to staffDisplayNames.getOrElse(0) { null },
                                        "assignedBy" to (assignerId ?: "system"),
                                        "assignedAt" to FieldValue.serverTimestamp(),
                                        "urgencyLevel" to UrgencyLevel.HIGH.name,
                                        "priority" to 1,
                                        "createdAt" to FieldValue.serverTimestamp(),
                                        "updatedAt" to FieldValue.serverTimestamp()
                                    ))

                                    // Заявка 2 (пациент 2, новая)
                                    calendar.time = Date()
                                    calendar.add(Calendar.DAY_OF_YEAR, 2)
                                    calendar.set(Calendar.HOUR_OF_DAY, 14)
                                    val preferredTime2 = calendar.time
                                    requestsData.add(hashMapOf(
                                        "patientId" to (patientData["patient2ProfileId"] ?: "unknown_patient_id_2"),
                                        "patientName" to (patientData["patient2Name"] ?: "Пациент 2"),
                                        "patientPhone" to "+79002222222",
                                        "address" to "г. Москва, ул. Тестовая, д. 2, кв. 11",
                                        "requestType" to RequestType.CONSULTATION.name,
                                        "symptoms" to "Нужна консультация по лечению",
                                        "preferredDate" to Timestamp(preferredTime2),
                                        "status" to RequestStatus.NEW.name,
                                        "urgencyLevel" to UrgencyLevel.NORMAL.name,
                                        "priority" to 2,
                                        "createdAt" to FieldValue.serverTimestamp(),
                                        "updatedAt" to FieldValue.serverTimestamp()
                                    ))

                                    // Заявка 3 (пациент 3, для отмены)
                                    calendar.time = Date()
                                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                                    calendar.set(Calendar.HOUR_OF_DAY, 12)
                                    val preferredTime3 = calendar.time
                                    requestsData.add(hashMapOf(
                                        "patientId" to (patientData["patient3ProfileId"] ?: "unknown_patient_id_3"),
                                        "patientName" to (patientData["patient3Name"] ?: "Пациент 3"),
                                        "patientPhone" to "+79003333333",
                                        "address" to "г. Москва, ул. Тестовая, д. 3, кв. 12",
                                        "requestType" to RequestType.REGULAR.name,
                                        "symptoms" to "Плановый осмотр, но нужно отменить",
                                        "preferredDate" to Timestamp(preferredTime3),
                                        "status" to RequestStatus.PENDING.name, // Представим, что она была в обработке
                                        "urgencyLevel" to UrgencyLevel.LOW.name,
                                        "priority" to 3,
                                        "createdAt" to FieldValue.serverTimestamp(),
                                        "updatedAt" to FieldValue.serverTimestamp()
                                    ))


                                    val requestIds = mutableMapOf<Int, String>()
                                    requestsData.forEachIndexed { idx, reqData ->
                                        val reqId = UUID.randomUUID().toString()
                                        requestIds[idx] = reqId
                                        db.collection("appointment_requests").document(reqId).set(reqData)
                                            .addOnSuccessListener { Log.d("Firebase", "AppointmentRequest ${idx+1} added with ID $reqId") }
                                            .addOnFailureListener { e -> Log.e("Firebase", "Error adding AppointmentRequest ${idx+1}", e) }
                                    }
                                    // Добавляем визиты только если их еще нет (упрощенная проверка)
                                    addSampleVisits(requestIds, staffIds, staffDisplayNames, patientData)

                                } else {
                                    Log.d("Firebase", "AppointmentRequests already exist.")
                                }
                            }
                    }
            }
    }

    private fun addSampleVisits(requestIds: Map<Int, String>, staffIds: List<String>, staffNames: List<String>, patientData: Map<String, String>) {
        db.collection("visits").limit(1).get().addOnSuccessListener { visitSnapshot ->
            if (visitSnapshot.isEmpty) {
                val calendar = Calendar.getInstance()
                val visitsData = mutableListOf<Map<String, Any?>>()
                val visitProtocolsToCreate = mutableMapOf<String, Map<String,Any?>>() // visitId to protocolData

                // Визит 1 (на основе заявки 1, запланирован)
                calendar.time = Date()
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 10)
                val scheduledTimeV1 = calendar.time
                val visit1Id = UUID.randomUUID().toString()
                visitsData.add(hashMapOf(
                    "patientId" to (patientData["patient1ProfileId"] ?: "unknown_patient_id_1"),
                    "scheduledTime" to Timestamp(scheduledTimeV1),
                    "status" to VisitStatus.PLANNED.name,
                    "address" to "г. Москва, ул. Тестовая, д. 1, кв. 10",
                    "reasonForVisit" to "Высокая температура, кашель (из заявки)",
                    "assignedStaffId" to staffIds.getOrElse(0) { null },
                    "assignedStaffName" to staffNames.getOrElse(0) { null },
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "isFromRequest" to true,
                    "originalRequestId" to (requestIds[0] ?: "unknown_request_id_1")
                ))

                // Визит 2 (на основе заявки 2, но представим, что он уже в процессе)
                calendar.time = Date() // Сегодня
                calendar.set(Calendar.HOUR_OF_DAY, Calendar.getInstance().get(Calendar.HOUR_OF_DAY) - 1) // Начался час назад
                val actualStartV2 = calendar.time
                calendar.add(Calendar.HOUR_OF_DAY, 2) // Запланирован на час вперед от текущего времени
                val scheduledTimeV2 = calendar.time
                val visit2Id = UUID.randomUUID().toString()
                visitsData.add(hashMapOf(
                    "patientId" to (patientData["patient2ProfileId"] ?: "unknown_patient_id_2"),
                    "scheduledTime" to Timestamp(scheduledTimeV2),
                    "actualStartTime" to Timestamp(actualStartV2),
                    "status" to VisitStatus.IN_PROGRESS.name,
                    "address" to "г. Москва, ул. Тестовая, д. 2, кв. 11",
                    "reasonForVisit" to "Консультация по лечению (из заявки)",
                    "assignedStaffId" to staffIds.getOrElse(1) { staffIds.getOrNull(0) }, // Другой врач или тот же
                    "assignedStaffName" to staffNames.getOrElse(1) { staffNames.getOrNull(0) },
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "isFromRequest" to true,
                    "originalRequestId" to (requestIds[1] ?: "unknown_request_id_2")
                ))

                // Визит 3 (на основе заявки 1, но завершен и с протоколом)
                calendar.time = Date()
                calendar.add(Calendar.DAY_OF_YEAR, -1) // Вчера
                calendar.set(Calendar.HOUR_OF_DAY, 15)
                val scheduledTimeV3 = calendar.time
                val actualStartV3 = calendar.time
                calendar.add(Calendar.MINUTE, 25)
                val actualEndV3 = calendar.time
                val visit3Id = UUID.randomUUID().toString()

                visitsData.add(hashMapOf(
                    "patientId" to (patientData["patient1ProfileId"] ?: "unknown_patient_id_1"),
                    "scheduledTime" to Timestamp(scheduledTimeV3),
                    "actualStartTime" to Timestamp(actualStartV3),
                    "actualEndTime" to Timestamp(actualEndV3),
                    "status" to VisitStatus.COMPLETED.name,
                    "address" to "г. Москва, ул. Тестовая, д. 1, кв. 10",
                    "reasonForVisit" to "Повторный осмотр после ОРВИ",
                    "assignedStaffId" to staffIds.getOrElse(0) { null },
                    "assignedStaffName" to staffNames.getOrElse(0) { null },
                    "notes" to "Состояние улучшилось, рекомендовано наблюдение.",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "isFromRequest" to false, // Например, это был повторный визит, не связанный с новой заявкой
                    "originalRequestId" to null
                ))
                val protocolDataV3 = hashMapOf(
                    "visitId" to visit3Id, // Будет установлен после создания визита
                    "complaints" to "Остаточный кашель, общая слабость.",
                    "anamnesis" to "ОРВИ в течение 5 дней, лечение по назначениям.",
                    "objectiveStatus" to "Температура 36.8°C. Зев спокоен. В легких дыхание везикулярное, хрипов нет.",
                    "diagnosis" to "ОРВИ, фаза выздоровления.",
                    "diagnosisCode" to "J06.9",
                    "recommendations" to "Продолжить симптоматическое лечение. Полоскание горла. Витамины.",
                    "temperature" to 36.8f,
                    "systolicBP" to 120,
                    "diastolicBP" to 80,
                    "pulse" to 70,
                    "additionalVitals" to mapOf("SpO2" to "98%"),
                    "createdAt" to Timestamp(actualEndV3), // Время создания протокола совпадает с окончанием визита
                    "updatedAt" to Timestamp(actualEndV3)
                )
                visitProtocolsToCreate[visit3Id] = protocolDataV3


                visitsData.forEachIndexed { idx, visitData ->
                    val currentVisitId = when(idx) {
                        0 -> visit1Id
                        1 -> visit2Id
                        2 -> visit3Id
                        else -> UUID.randomUUID().toString()
                    }
                    db.collection("visits").document(currentVisitId).set(visitData)
                        .addOnSuccessListener {
                            Log.d("Firebase", "Visit ${idx+1} added with ID $currentVisitId")
                            if (visitProtocolsToCreate.containsKey(currentVisitId)) {
                                val protocolData = visitProtocolsToCreate[currentVisitId]?.toMutableMap()
                                protocolData?.set("visitId", currentVisitId) // Убедимся, что visitId правильный
                                if (protocolData != null) {
                                    val protocolId = UUID.randomUUID().toString()
                                    db.collection("visit_protocols").document(protocolId).set(protocolData)
                                        .addOnSuccessListener { Log.d("Firebase", "VisitProtocol added for visit $currentVisitId with ID $protocolId") }
                                        .addOnFailureListener { e -> Log.e("Firebase", "Error adding VisitProtocol for visit $currentVisitId", e) }
                                }
                            }
                        }
                        .addOnFailureListener { e -> Log.e("Firebase", "Error adding Visit ${idx+1}", e) }
                }
            } else {
                Log.d("Firebase", "Visits already exist.")
            }
        }
    }


    private fun addTestData() {
//        addAdminUserIfNotExists()
//        addMedicalStaffUsersIfNotExists()
//        addDispatcherUserIfNotExists()
        // Шаблоны и пациенты добавляются перед заявками, т.к. заявки могут ссылаться на пациентов
//        addSampleProtocolTemplates()
//        addPatientUsersAndProfilesIfNotExists() // Эта функция теперь вызовет addSampleAppointmentRequestsAndVisits
    }
}