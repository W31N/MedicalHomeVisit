package com.example.medicalhomevisit

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.example.medicalhomevisit.data.local.AppDatabase
import com.example.medicalhomevisit.data.local.dao.PatientDao
import com.example.medicalhomevisit.data.local.entity.PatientEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PatientDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var patientDao: PatientDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java
        ).build()
        patientDao = db.patientDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun insertPatientAndGetById_returnsSamePatient() = runTest {
        val patientId = UUID.randomUUID().toString()
        val testPatient = PatientEntity(
            id = patientId,
            fullName = "Иван Тестовый",
            address = "ул. Тестовая, д.1",
            isSynced = true
        )
        patientDao.insertPatient(testPatient)

        val retrievedPatient = patientDao.getPatientById(patientId)

        assertNotNull("Пациент не должен быть null", retrievedPatient)
        assertEquals("ID пациента не совпадает", testPatient.id, retrievedPatient?.id)
        assertEquals("Имя пациента не совпадает", testPatient.fullName, retrievedPatient?.fullName)
        assertEquals("Адрес не совпадает", testPatient.address, retrievedPatient?.address)
    }

    @Test
    fun updatePatientProfile_updatesCorrectly() = runTest {
        val patientId = UUID.randomUUID().toString()
        val initialPatient = PatientEntity(id = patientId, fullName = "Анна Начальная", address = "Старый адрес")
        patientDao.insertPatient(initialPatient)

        val newAddress = "Новый адрес, кв. 5"
        val newPhoneNumber = "88005553535"
        val newPolicyNumber = "123456789"

        patientDao.updatePatientProfile(
            patientId = patientId,
            dateOfBirth = null,
            gender = null,
            address = newAddress,
            phoneNumber = newPhoneNumber,
            policyNumber = newPolicyNumber,
            allergies = null,
            chronicConditions = null
        )

        val updatedPatient = patientDao.getPatientById(patientId)
        assertNotNull(updatedPatient)
        assertEquals("Адрес должен был обновиться", newAddress, updatedPatient?.address)
        assertEquals("Телефон должен был обновиться", newPhoneNumber, updatedPatient?.phoneNumber)
        assertEquals("Номер полиса должен был обновиться", newPolicyNumber, updatedPatient?.policyNumber)
        assertEquals("isSynced должен стать false после обновления", false, updatedPatient?.isSynced)
        assertEquals("syncAction должен стать 'UPDATE'", "UPDATE", updatedPatient?.syncAction)
    }

    @Test
    fun observePatient_emitsValueOnInsert() = runTest {
        val patientId = "patient-flow-id"

        patientDao.observePatient(patientId).test {
            assertEquals("Изначально Flow должен эмитить null", null, awaitItem())

            val testPatient = PatientEntity(id = patientId, fullName = "Реактивный Пациент")
            patientDao.insertPatient(testPatient)

            val emittedPatient = awaitItem()
            assertNotNull("После вставки Flow должен эмитить пациента, а не null", emittedPatient)
            assertEquals("ID в Flow не совпадает", patientId, emittedPatient?.id)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun getUnsyncedPatients_returnsOnlyUnsynced() = runTest {
        val syncedPatient = PatientEntity(id = "synced-1", fullName = "Синхронизированный", isSynced = true)
        val unsyncedPatient1 = PatientEntity(id = "unsynced-1", fullName = "Несинхронизированный 1", isSynced = false)
        val unsyncedPatient2 = PatientEntity(id = "unsynced-2", fullName = "Несинхронизированный 2", isSynced = false)

        patientDao.insertPatients(listOf(syncedPatient, unsyncedPatient1, unsyncedPatient2))

        val unsyncedList = patientDao.getUnsyncedPatients()

        assertEquals("Должно быть найдено 2 несинхронизированных пациента", 2, unsyncedList.size)
        assertTrue("Список не должен содержать синхронизированного пациента", unsyncedList.none { it.id == "synced-1" })
        assertTrue("Список должен содержать первого несинхронного пациента", unsyncedList.any { it.id == "unsynced-1" })
    }
}