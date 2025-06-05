package com.example.medicalhomevisit.data.remote.repository
import android.util.Log
import com.example.medicalhomevisit.data.remote.api.AdminApiService
import com.example.medicalhomevisit.domain.model.MedicalStaffDisplay
import com.example.medicalhomevisit.domain.model.User
import com.example.medicalhomevisit.domain.model.UserRole
import com.example.medicalhomevisit.data.remote.dto.AdminPatientRegistrationDto
import com.example.medicalhomevisit.domain.repository.AdminRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject

class AdminRepositoryImpl @Inject constructor(
    private val adminApiService: AdminApiService,
) : AdminRepository {

    companion object {
        private const val TAG = "BackendAdminRepo"
    }

    override suspend fun getActiveStaff(): Result<List<MedicalStaffDisplay>> {
        Log.d(TAG, "getActiveStaff called")
        return withContext(Dispatchers.IO) {
            try {
                val response = adminApiService.getActiveMedicalStaff()
                Log.d(TAG, "getActiveMedicalStaff API response: code=${response.code()}, isSuccessful=${response.isSuccessful}")
                if (response.isSuccessful && response.body() != null) {
                    val staffDtoList = response.body()!!
                    Log.d(TAG, "getActiveMedicalStaff successful. Received ${staffDtoList.size} DTOs.")
                    val staffDomainList = staffDtoList.map { dto ->
                        MedicalStaffDisplay(
                            medicalPersonId  = dto.medicalPersonId,
                            userId = dto.userId,
                            displayName = dto.fullName,
                            role = UserRole.MEDICAL_STAFF,
                            specialization = dto.specialization
                        )
                    }
                    Log.d(TAG, "Mapped to ${staffDomainList.size} User domain models for staff list.")
                    Result.success(staffDomainList)
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.e(TAG, "Error getting active staff: Code=${response.code()}, Body='$errorBody'")
                    Result.failure(Exception("Ошибка загрузки мед. персонала (код: ${response.code()})"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in getActiveStaff", e)
                Result.failure(e)
            }
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
    ): Result<User> {
        Log.d(TAG, "registerNewPatient called for email: $email")
        return withContext(Dispatchers.IO) {
            try {
                val registrationDto = AdminPatientRegistrationDto(
                    email = email,
                    password = password,
                    fullName = displayName,
                    phoneNumber = phoneNumber,
                    address = address,
                    dateOfBirth = dateOfBirth,
                    gender = gender,
                    medicalCardNumber = medicalCardNumber,
                    additionalInfo = additionalInfo
                )
                Log.d(TAG, "Attempting to register patient on backend with DTO: $registrationDto")

                val response = adminApiService.registerPatientByAdmin(registrationDto)
                Log.d(TAG, "Register patient API response: code=${response.code()}, isSuccessful=${response.isSuccessful}")

                if (response.isSuccessful && response.body() != null) {
                    val userDto = response.body()!!
                    Log.d(TAG, "Patient registration successful. Response UserDto: $userDto")
                    val createdUser = userDto.toDomainUser()
                    Result.success(createdUser)
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.e(TAG, "Error registering patient: Code=${response.code()}, Body='$errorBody'")
                    Result.failure(Exception("Ошибка регистрации пациента (код: ${response.code()})"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in registerNewPatient", e)
                Result.failure(e)
            }
        }
    }
}