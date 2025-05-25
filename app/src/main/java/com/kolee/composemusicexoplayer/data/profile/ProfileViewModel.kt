package com.kolee.composemusicexoplayer.data.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.kolee.composemusicexoplayer.data.auth.UserPreferences
import com.kolee.composemusicexoplayer.data.model.ProfileResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
 import android.content.Context
 import android.net.Uri


class ProfileViewModel(private val userPreferences: UserPreferences) : ViewModel() {
    private val repository = ProfileRepository()
    private val _profile = MutableStateFlow<ProfileResponse?>(null)
    val profile = _profile.asStateFlow()

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus = _updateStatus.asStateFlow()

    fun resetProfile() {
        _profile.value = null
    }

    fun fetchProfile() {
        viewModelScope.launch {
            _profile.value = null

            try {
                val token = userPreferences.getTokenOnce()
                if (token.isNullOrEmpty()) {
                    Log.e("ProfileVM", "Token kosong. Tidak bisa mengambil profil.")
                    return@launch
                }


                val bearerToken = "Bearer $token"
                Log.d("ProfileVM", "Mengambil profil dengan token: $bearerToken")

                val response = repository.getProfile(bearerToken)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        _profile.value = body
                        Log.d("ProfileVM", "Profil berhasil diambil: $body")
                    } else {
                        Log.e("ProfileVM", "Response berhasil tapi body null.")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("ProfileVM", "Gagal mengambil profil: ${response.code()} - $errorBody")
                }
            } catch (e: Exception) {
                Log.e("ProfileVM", "Exception saat fetch profile: ${e.message}", e)
            }
        }
    }

    fun updateProfile(
        location: String? = null,
        imageUri: Uri? = null,
        context: Context
    ) {
        viewModelScope.launch {
            _updateStatus.value = UpdateStatus.Loading

            try {
                val token = userPreferences.getTokenOnce()
                if (token.isNullOrEmpty()) {
                    _updateStatus.value = UpdateStatus.Error("Token not found")
                    return@launch
                }

                val bearerToken = "Bearer $token"
                Log.d("ProfileVM", "Updating profile with token: $bearerToken")

                val response = repository.updateProfile(
                    token = bearerToken,
                    location = location,
                    imageUri = imageUri,
                    context = context
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        _profile.value = body
                        _updateStatus.value = UpdateStatus.Success
                        Log.d("ProfileVM", "Profile updated successfully: $body")
                    } else {
                        _updateStatus.value = UpdateStatus.Error("Empty response body")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    _updateStatus.value = UpdateStatus.Error("Update failed: ${response.code()} - $errorBody")
                    Log.e("ProfileVM", "Failed to update profile: ${response.code()} - $errorBody")
                }
            } catch (e: Exception) {
                _updateStatus.value = UpdateStatus.Error("Exception: ${e.message}")
                Log.e("ProfileVM", "Exception during profile update: ${e.message}", e)
            }
        }
    }

    fun resetUpdateStatus() {
        _updateStatus.value = UpdateStatus.Idle
    }
}

sealed class UpdateStatus {
    object Idle : UpdateStatus()
    object Loading : UpdateStatus()
    object Success : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
}