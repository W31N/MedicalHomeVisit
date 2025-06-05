package com.example.medicalhomevisit.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.medicalhomevisit.data.local.entity.PatientEntity
import java.util.Date

@Dao
interface PatientDao {

    // 📱 ОСНОВНЫЕ ОПЕРАЦИИ
    @Query("SELECT * FROM patients WHERE id = :patientId LIMIT 1")
    suspend fun getPatientById(patientId: String): PatientEntity?

    @Query("SELECT * FROM patients WHERE id = :patientId LIMIT 1")
    fun observePatient(patientId: String): Flow<PatientEntity?>

    @Query("SELECT * FROM patients ORDER BY fullName ASC")
    fun getAllPatients(): Flow<List<PatientEntity>>

    @Query("SELECT * FROM patients ORDER BY fullName ASC")
    suspend fun getAllPatientsSync(): List<PatientEntity>

    @Query("SELECT * FROM patients WHERE fullName LIKE '%' || :query || '%' ORDER BY fullName ASC")
    fun searchPatients(query: String): Flow<List<PatientEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatient(patient: PatientEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatients(patients: List<PatientEntity>)

    @Update
    suspend fun updatePatient(patient: PatientEntity)

    @Delete
    suspend fun deletePatient(patient: PatientEntity)

    // 🔄 МЕТОДЫ ДЛЯ СИНХРОНИЗАЦИИ
    @Query("SELECT * FROM patients WHERE isSynced = 0 ORDER BY updatedAt ASC")
    suspend fun getUnsyncedPatients(): List<PatientEntity>

    @Query("UPDATE patients SET isSynced = 1, syncAction = null WHERE id = :patientId")
    suspend fun markAsSynced(patientId: String)

    @Query("UPDATE patients SET lastSyncAttempt = :timestamp WHERE id = :patientId")
    suspend fun updateLastSyncAttempt(patientId: String, timestamp: Date)

    // Вспомогательные методы для обновления с офлайн пометкой
    suspend fun updatePatientProfile(
        patientId: String,
        dateOfBirth: Date?,
        gender: String?,
        address: String?,
        phoneNumber: String?,
        policyNumber: String?,
        allergies: List<String>?,
        chronicConditions: List<String>?
    ) {
        val now = Date()
        updatePatientProfileInternal(
            patientId, dateOfBirth, gender, address, phoneNumber,
            policyNumber, allergies, chronicConditions, now
        )
    }

    @Query("""
        UPDATE patients SET 
        dateOfBirth = :dateOfBirth,
        gender = :gender,
        address = :address,
        phoneNumber = :phoneNumber,
        policyNumber = :policyNumber,
        allergies = :allergies,
        chronicConditions = :chronicConditions,
        isSynced = 0,
        syncAction = 'UPDATE',
        updatedAt = :now
        WHERE id = :patientId
    """)
    suspend fun updatePatientProfileInternal(
        patientId: String,
        dateOfBirth: Date?,
        gender: String?,
        address: String?,
        phoneNumber: String?,
        policyNumber: String?,
        allergies: List<String>?,
        chronicConditions: List<String>?,
        now: Date
    )

    // 🗑️ УДОБНЫЕ МЕТОДЫ
    @Query("DELETE FROM patients")
    suspend fun deleteAllPatients()

    @Query("SELECT COUNT(*) FROM patients WHERE isSynced = 0")
    suspend fun getUnsyncedCount(): Int

    // 🔍 ОТЛАДОЧНЫЕ МЕТОДЫ
    @Query("SELECT COUNT(*) FROM patients")
    suspend fun getTotalPatientsCount(): Int

    @Query("SELECT * FROM patients ORDER BY updatedAt DESC LIMIT 10")
    suspend fun getRecentPatients(): List<PatientEntity>
}