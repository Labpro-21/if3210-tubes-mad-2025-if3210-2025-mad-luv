package com.kolee.composemusicexoplayer.data.profile

import android.content.Context
import android.net.Uri
import com.kolee.composemusicexoplayer.data.model.ProfileResponse
import com.kolee.composemusicexoplayer.data.network.ApiClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response

class ProfileRepository {
    private val apiService = ApiClient.apiService

    suspend fun getProfile(token: String): Response<ProfileResponse> {
        return apiService.getProfile("$token")
    }

    suspend fun updateProfile(
        token: String,
        location: String? = null,
        imageUri: Uri? = null,
        context: Context
    ): Response<ProfileResponse> {
        val locationBody = location?.let {
            RequestBody.create("text/plain".toMediaTypeOrNull(), it)
        }

        val imagePart = imageUri?.let { uri ->
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()

            bytes?.let {
                val requestBody = RequestBody.create("image/*".toMediaTypeOrNull(), it)
                MultipartBody.Part.createFormData("profilePhoto", "profile.jpg", requestBody)
            }
        }

        return apiService.updateProfile(
            token = token,
            location = locationBody,
            profilePhoto = imagePart
        )
    }
}