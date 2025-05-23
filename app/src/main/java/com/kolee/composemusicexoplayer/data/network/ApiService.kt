package com.kolee.composemusicexoplayer.data.network

import com.kolee.composemusicexoplayer.data.model.LoginResponse
import com.kolee.composemusicexoplayer.data.model.LoginRequest
import com.kolee.composemusicexoplayer.data.model.OnlineSong
import com.kolee.composemusicexoplayer.data.model.ProfileResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @POST("/api/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("/api/verify-token")
    suspend fun verifyToken(@Header("Authorization") token: String): Response<Unit>

    @POST("/api/refresh-token")
    suspend fun refreshToken(@Body request: Map<String, String>): LoginResponse

    @GET("/api/profile")
    suspend fun getProfile(@Header("Authorization") token: String): Response<ProfileResponse>

    @GET("api/top-songs/global")
    suspend fun getTopGlobalSongs(): List<OnlineSong>

    @GET("api/top-songs/{countryCode}")
    suspend fun getTopSongsByCountry(@Path("countryCode") countryCode: String): List<OnlineSong>
}