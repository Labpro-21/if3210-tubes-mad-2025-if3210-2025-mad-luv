package com.kolee.composemusicexoplayer.data.profile

import com.kolee.composemusicexoplayer.data.model.ProfileResponse
import com.kolee.composemusicexoplayer.data.network.ApiClient
import retrofit2.Response

class ProfileRepository {
    private val apiService = ApiClient.apiService

    suspend fun getProfile(token: String): Response<ProfileResponse> {
        return apiService.getProfile("$token")
    }
}