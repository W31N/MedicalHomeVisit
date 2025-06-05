package com.example.medicalhomevisit.data.di

import android.content.Context
import androidx.room.Room
import com.example.medicalhomevisit.data.local.AppDatabase
import com.example.medicalhomevisit.data.local.dao.ProtocolTemplateDao
import com.example.medicalhomevisit.data.local.dao.VisitDao
import com.example.medicalhomevisit.data.local.dao.VisitProtocolDao
import com.example.medicalhomevisit.data.remote.api.AdminApiService
import com.example.medicalhomevisit.data.remote.api.AppointmentApiService
import com.example.medicalhomevisit.data.remote.api.AuthApiService
import com.example.medicalhomevisit.data.remote.network.AuthInterceptor
import com.example.medicalhomevisit.data.remote.repository.AdminRepositoryImpl
import com.example.medicalhomevisit.data.remote.repository.AppointmentRequestRepositoryImpl
import com.example.medicalhomevisit.data.remote.repository.PatientRepositoryImpl
import com.example.medicalhomevisit.data.remote.api.PatientApiService
import com.example.medicalhomevisit.data.remote.api.ProtocolApiService
import com.example.medicalhomevisit.data.remote.network.TokenManager
import com.example.medicalhomevisit.data.remote.api.VisitApiService
import com.example.medicalhomevisit.data.remote.repository.AuthRepositoryImpl
import com.example.medicalhomevisit.data.repository.SimpleOfflineProtocolRepository
import com.example.medicalhomevisit.data.repository.SimpleOfflineVisitRepository
import com.example.medicalhomevisit.data.sync.SyncManager
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
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val BASE_URL = "http://192.168.0.102:8080/"
//    private const val BASE_URL = "http://10.0.2.2:8080/"

    // ===== ROOM DATABASE =====

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "medical_home_visit_database"
        )
            .fallbackToDestructiveMigration() // Для разработки - удаляет БД при изменении схемы
            .build()
    }

    @Provides
    fun provideVisitDao(database: AppDatabase): VisitDao = database.visitDao()

    @Provides
    fun provideVisitProtocolDao(database: AppDatabase): VisitProtocolDao = database.visitProtocolDao()

    @Provides
    fun provideProtocolTemplateDao(database: AppDatabase): ProtocolTemplateDao = database.protocolTemplateDao()


    // ===== NETWORKING =====

    @Provides
    @Singleton
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
                HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val gson = GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .create()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    // ===== API SERVICES =====

    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAppointmentApiService(retrofit: Retrofit): AppointmentApiService {
        return retrofit.create(AppointmentApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAdminApiService(retrofit: Retrofit): AdminApiService {
        return retrofit.create(AdminApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideVisitApiService(retrofit: Retrofit): VisitApiService {
        return retrofit.create(VisitApiService::class.java)
    }

    @Provides
    @Singleton
    fun providePatientApiService(retrofit: Retrofit): PatientApiService {
        return retrofit.create(PatientApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideProtocolApiService(retrofit: Retrofit): ProtocolApiService {
        return retrofit.create(ProtocolApiService::class.java)
    }

    // ===== REPOSITORIES =====

    @Provides
    @Singleton
    fun provideAuthRepository(
        authApiService: AuthApiService,
        tokenManager: TokenManager
    ): AuthRepository {
        return AuthRepositoryImpl(
            authApiService,
            tokenManager
        )
    }

    @Provides
    @Singleton
    fun provideAppointmentRequestRepository(
        appointmentApiService: AppointmentApiService,
        tokenManager: TokenManager, // Добавляем TokenManager как параметр
        authRepository: AuthRepository // Добавляем AuthRepository как параметр
        // Используй конкретный тип интерфейса, который ты указал для AuthRepository
    ): AppointmentRequestRepository { // Это тип возвращаемого интерфейса
        return AppointmentRequestRepositoryImpl(
            appointmentApiService,
            tokenManager, // Передаем его здесь
            authRepository  // Передаем его здесь
        )
    }

    @Provides
    @Singleton
    fun provideAdminRepository(
        adminApiService: AdminApiService,
        tokenManager: TokenManager // Если нужен в BackendAdminRepository
    ): AdminRepository { // Убедись, что это твой интерфейс AdminRepository
        return AdminRepositoryImpl(adminApiService, tokenManager)
    }

//    @Provides
//    @Singleton
//    fun provideVisitRepository(
//        visitApiService: VisitApiService,
//        authRepository: AuthRepository
//    ): VisitRepository {
//        return VisitRepositoryImpl(visitApiService, authRepository)
//    }

    @Provides
    @Singleton
    fun provideSyncManager(@ApplicationContext context: Context): SyncManager {
        return SyncManager(context)
    }

    @Provides
    @Singleton
    fun provideVisitRepository(
        visitDao: VisitDao,
        visitApiService: VisitApiService,
        authRepository: AuthRepository,
        syncManager: SyncManager
    ): VisitRepository {
        return SimpleOfflineVisitRepository(visitDao, visitApiService, authRepository, syncManager)
    }

    @Provides
    @Singleton
    fun providePatientRepository(
        patientApiService: PatientApiService,
        authRepository: AuthRepository
    ): PatientRepository {
        return PatientRepositoryImpl(patientApiService, authRepository)
    }

//    @Provides
//    @Singleton
//    fun provideProtocolRepository(
//        protocolApiService: ProtocolApiService,
//        authRepository: AuthRepository
//    ): ProtocolRepository {
//        return ProtocolRepositoryImpl(protocolApiService, authRepository)
//    }
    @Provides
    @Singleton
    fun provideProtocolRepository(
        protocolApiService: ProtocolApiService,
        visitProtocolDao: VisitProtocolDao,
        protocolTemplateDao: ProtocolTemplateDao,
        authRepository: AuthRepository,
        syncManager: SyncManager
        // visitDao: VisitDao,
    ): ProtocolRepository {
        return SimpleOfflineProtocolRepository(
            protocolApiService,
            visitProtocolDao,         // Передаем DAO
            protocolTemplateDao,      // Передаем DAO
            authRepository,           // Передаем, если нужно
            syncManager               // Передаем SyncManager
            // visitDao
        )
    }
}