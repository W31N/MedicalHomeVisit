package com.example.medicalhomevisit.data.remote

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenManager.getToken()
        val originalRequest = chain.request()

        val requestBuilder = originalRequest.newBuilder()
        // Добавляем Content-Type для всех запросов, если API его ожидает
        // (для POST с @Body Retrofit обычно добавляет его сам)
        // .header("Content-Type", "application/json")

        if (token != null &&
            !originalRequest.url.encodedPath.contains("/api/auth/login") &&
            !originalRequest.url.encodedPath.contains("/api/auth/register")) {
            // Не добавляем токен к эндпоинтам входа и регистрации
            requestBuilder.header("Authorization", "Bearer $token")
        }

        val request = requestBuilder.build()
        return chain.proceed(request)
    }
}