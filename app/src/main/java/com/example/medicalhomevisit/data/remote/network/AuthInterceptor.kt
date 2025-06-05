package com.example.medicalhomevisit.data.remote.network

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenManager.getToken()
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()
        if (token != null &&
            !originalRequest.url.encodedPath.contains("/api/auth/login") &&
            !originalRequest.url.encodedPath.contains("/api/auth/register")) {
            requestBuilder.header("Authorization", "Bearer $token")
        }
        val request = requestBuilder.build()
        return chain.proceed(request)
    }
}