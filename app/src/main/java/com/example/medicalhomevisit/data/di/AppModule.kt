package com.example.medicalhomevisit.data.di

import android.content.Context
import com.example.medicalhomevisit.data.remote.api.AdminApiService
import com.example.medicalhomevisit.data.remote.api.AppointmentApiService
import com.example.medicalhomevisit.data.remote.api.AuthApiService
import com.example.medicalhomevisit.data.remote.network.AuthInterceptor
import com.example.medicalhomevisit.data.remote.repository.BackendAdminRepository
import com.example.medicalhomevisit.data.remote.repository.BackendAppointmentRequestRepository
import com.example.medicalhomevisit.data.remote.repository.BackendAuthRepository
import com.example.medicalhomevisit.data.remote.repository.HttpPatientRepository
import com.example.medicalhomevisit.data.remote.repository.HttpProtocolRepository
import com.example.medicalhomevisit.data.remote.repository.HttpVisitRepository
import com.example.medicalhomevisit.data.remote.api.PatientApiService
import com.example.medicalhomevisit.data.remote.api.ProtocolApiService
import com.example.medicalhomevisit.data.remote.network.TokenManager
import com.example.medicalhomevisit.data.remote.api.VisitApiService
import com.example.medicalhomevisit.domain.repository.AdminRepository
import com.example.medicalhomevisit.domain.repository.AppointmentRequestRepository
import com.example.medicalhomevisit.domain.repository.AuthRepository
import com.example.medicalhomevisit.domain.repository.PatientRepository
import com.example.medicalhomevisit.domain.repository.ProtocolRepository
import com.example.medicalhomevisit.domain.repository.VisitRepository
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // Зависимости будут жить, пока живо приложение
object AppModule { // Используем object для предоставления статических методов @Provides

    private const val BASE_URL = "http://10.0.2.2:8080/"

    @Provides
    @Singleton // Гарантирует, что будет создан только один экземпляр TokenManager
    fun provideTokenManager(@ApplicationContext context: Context): TokenManager {
        return TokenManager(context)
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenManager: TokenManager): AuthInterceptor {
        return AuthInterceptor(tokenManager)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level =
                HttpLoggingInterceptor.Level.BODY // Используйте BuildConfig.DEBUG для включения только в debug
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor) // Сначала ваш AuthInterceptor
            .addInterceptor(loggingInterceptor) // Потом логирующий
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val gson = GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") // Настройте, если нужно
            .create()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        authApiService: AuthApiService,
        tokenManager: TokenManager
    ): AuthRepository { // Указываем интерфейс
        return BackendAuthRepository(
            authApiService,
            tokenManager
        ) // Возвращаем реализацию
    }

    @Provides
    @Singleton
    fun provideAppointmentApiService(retrofit: Retrofit): AppointmentApiService {
        return retrofit.create(AppointmentApiService::class.java)
    }


    @Provides
    @Singleton
    fun provideAppointmentRequestRepository(
        appointmentApiService: AppointmentApiService,
        tokenManager: TokenManager, // Добавляем TokenManager как параметр
        authRepository: AuthRepository // Добавляем AuthRepository как параметр
        // Используй конкретный тип интерфейса, который ты указал для AuthRepository
    ): AppointmentRequestRepository { // Это тип возвращаемого интерфейса
        return BackendAppointmentRequestRepository(
            appointmentApiService,
            tokenManager, // Передаем его здесь
            authRepository  // Передаем его здесь
        )
    }


    @Provides
    @Singleton
    fun provideAdminApiService(retrofit: Retrofit): AdminApiService {
        return retrofit.create(AdminApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAdminRepository(
        adminApiService: AdminApiService,
        tokenManager: TokenManager // Если нужен в BackendAdminRepository
    ): AdminRepository { // Убедись, что это твой интерфейс AdminRepository
        return BackendAdminRepository(adminApiService, tokenManager)
    }

    // НОВОЕ: Добавляем VisitApiService
    @Provides
    @Singleton
    fun provideVisitApiService(retrofit: Retrofit): VisitApiService {
        return retrofit.create(VisitApiService::class.java)
    }

    // НОВОЕ: Добавляем VisitRepository
    @Provides
    @Singleton
    fun provideVisitRepository(
        visitApiService: VisitApiService,
        authRepository: AuthRepository
    ): VisitRepository {
        return HttpVisitRepository(visitApiService, authRepository)
    }

    @Provides
    @Singleton
    fun providePatientApiService(retrofit: Retrofit): PatientApiService {
        return retrofit.create(PatientApiService::class.java)
    }

    @Provides
    @Singleton
    fun providePatientRepository(
        patientApiService: PatientApiService,
        authRepository: AuthRepository
    ): PatientRepository {
        return HttpPatientRepository(patientApiService, authRepository)
    }

    @Provides
    @Singleton
    fun provideProtocolApiService(retrofit: Retrofit): ProtocolApiService {
        return retrofit.create(ProtocolApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideProtocolRepository(
        protocolApiService: ProtocolApiService,
        authRepository: AuthRepository
    ): ProtocolRepository {
        return HttpProtocolRepository(protocolApiService, authRepository)
    }
}