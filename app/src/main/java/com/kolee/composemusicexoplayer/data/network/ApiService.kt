package com.kolee.composemusicexoplayer.data.network

import com.kolee.composemusicexoplayer.data.model.LoginResponse
import com.kolee.composemusicexoplayer.data.model.LoginRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {
    @POST("/api/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("/api/verify-token")
    suspend fun verifyToken(@Header("Authorization") token: String): Response<Unit>

    @POST("/api/refresh-token")
    suspend fun refreshToken(@Body request: Map<String, String>): LoginResponse
}