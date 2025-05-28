package com.example.medicalhomevisit.data.repository

import android.util.Log
import com.example.medicalhomevisit.data.model.User
import com.example.medicalhomevisit.data.model.UserRole
import com.example.medicalhomevisit.domain.repository.AdminRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.Date

class FirebaseAdminRepository : AdminRepository {
    private val auth = Firebase.auth
    private val db = Firebase.firestore
    private val usersCollection = db.collection("users")
    private val patientsCollection = db.collection("patients")
    private val staffCollection = db.collection("medical_staff")

    override suspend fun getAllStaff(): List<User> {
        try {
            val snapshot = usersCollection
                .whereEqualTo("role", UserRole.MEDICAL_STAFF.name)
                .get()
                .await()

            return snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null

                    User(
                        id = doc.id,
                        email = data["email"] as String,
                        displayName = data["displayName"] as String,
                        role = UserRole.valueOf(data["role"] as String)
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error converting staff document", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all staff", e)
            throw e
        }
    }

    override suspend fun getActiveStaff(): List<User> {
        try {
            val snapshot = usersCollection
                .whereEqualTo("role", UserRole.MEDICAL_STAFF.name)
                .get()
                .await()

            val staff = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null

                    User(
                        id = doc.id,
                        email = data["email"] as String,
                        displayName = data["displayName"] as String,
                        role = UserRole.valueOf(data["role"] as String)
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error converting staff document", e)
                    null
                }
            }

            Log.d(TAG, "Found ${staff.size} medical staff members")
            return staff
        } catch (e: Exception) {
            Log.e(TAG, "Error getting medical staff", e)
            throw e
        }
    }

    override suspend fun registerNewPatient(
        email: String,
        password: String,
        displayName: String,
        phoneNumber: String,
        address: String,
        dateOfBirth: Date,
        gender: String,
        medicalCardNumber: String?,
        additionalInfo: String?
    ): User {
        try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Failed to create user")

            val userData = hashMapOf(
                "email" to email,
                "displayName" to displayName,
                "role" to UserRole.PATIENT.name,
                "createdAt" to FieldValue.serverTimestamp()
            )

            usersCollection.document(firebaseUser.uid).set(userData).await()

            val patientData = hashMapOf(
                "displayName" to displayName,
                "phoneNumber" to phoneNumber,
                "address" to address,
                "dateOfBirth" to dateOfBirth,
                "gender" to gender,
                "medicalCardNumber" to (medicalCardNumber ?: ""),
                "additionalInfo" to (additionalInfo ?: ""),
                "createdAt" to FieldValue.serverTimestamp()
            )

            patientsCollection.document(firebaseUser.uid).set(patientData).await()

            return User(
                id = firebaseUser.uid,
                email = email,
                displayName = displayName,
                role = UserRole.PATIENT
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error registering new patient", e)
            throw e
        }
    }

    companion object {
        private const val TAG = "FirebaseAdminRepository"
    }
}