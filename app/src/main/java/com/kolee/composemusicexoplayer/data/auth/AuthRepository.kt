package com.kolee.composemusicexoplayer.data.auth

import com.kolee.composemusicexoplayer.data.model.LoginRequest
import com.kolee.composemusicexoplayer.data.model.LoginResponse
import com.kolee.composemusicexoplayer.data.model.ProfileResponse
import com.kolee.composemusicexoplayer.data.network.ApiClient
import retrofit2.Response

class AuthRepository {
    private val apiService = ApiClient.apiService

    suspend fun login(email: String, password: String): Response<LoginResponse> {
        return apiService.login(LoginRequest(email, password))
    }
}
