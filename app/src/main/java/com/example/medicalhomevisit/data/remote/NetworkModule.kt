//package com.example.medicalhomevisit.data.remote
//
//import android.content.Context
//import com.google.gson.GsonBuilder // Если используете Gson
//import okhttp3.OkHttpClient
//import okhttp3.logging.HttpLoggingInterceptor
//import retrofit2.Retrofit
//import retrofit2.converter.gson.GsonConverterFactory // Если используете Gson
//import java.util.concurrent.TimeUnit
//
//object NetworkModule {
//
//    // Замените на IP вашего компьютера (если тестируете на эмуляторе)
//    // или на реальный адрес вашего развернутого бэкенда
//    private const val BASE_URL = "http://10.0.2.2:8080/" // Для эмулятора Android
//
//    // Переменные для хранения инстансов, инициализируются один раз
//    private var tokenManagerInstance: TokenManager? = null
//    private var okHttpClientInstance: OkHttpClient? = null
//    private var retrofitInstance: Retrofit? = null
//    private var authApiInstance: AuthApiService? = null
//
//    // Метод для инициализации (вызывать из Application.onCreate())
//    fun initialize(applicationContext: Context) {
//        if (tokenManagerInstance == null) {
//            tokenManagerInstance = TokenManager(applicationContext)
//        }
//        if (okHttpClientInstance == null) {
//            okHttpClientInstance = provideOkHttpClient(tokenManagerInstance!!)
//        }
//        if (retrofitInstance == null) {
//            retrofitInstance = provideRetrofit(okHttpClientInstance!!)
//        }
//        if (authApiInstance == null) {
//            authApiInstance = provideAuthApiService(retrofitInstance!!)
//        }
//    }
//
//    // Методы для получения инстансов (после инициализации)
//    fun getTokenManager(): TokenManager {
//        return tokenManagerInstance ?: throw IllegalStateException("TokenManager not initialized")
//    }
//
//    fun getAuthApiService(): AuthApiService {
//        return authApiInstance ?: throw IllegalStateException("AuthApiService not initialized")
//    }
//
//
//    private fun provideOkHttpClient(tokenManager: TokenManager): OkHttpClient {
//        val loggingInterceptor = HttpLoggingInterceptor().apply {
//            // В BuildConfig.DEBUG можно обернуть, чтобы логирование было только в debug-сборках
//            level = HttpLoggingInterceptor.Level.BODY
//        }
//        return OkHttpClient.Builder()
//            .addInterceptor(AuthInterceptor(tokenManager))
//            .addInterceptor(loggingInterceptor)
//            .connectTimeout(30, TimeUnit.SECONDS)
//            .readTimeout(30, TimeUnit.SECONDS)
//            .writeTimeout(30, TimeUnit.SECONDS)
//            .build()
//    }
//
//    private fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
//        val gson = GsonBuilder()
//            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") // Настройте формат даты, если нужно
//            .create()
//
//        return Retrofit.Builder()
//            .baseUrl(BASE_URL)
//            .client(okHttpClient)
//            .addConverterFactory(GsonConverterFactory.create(gson))
//            .build()
//    }
//
//    private fun provideAuthApiService(retrofit: Retrofit): AuthApiService {
//        return retrofit.create(AuthApiService::class.java)
//    }
//}