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

class ProfileViewModel(private val userPreferences: UserPreferences) : ViewModel() {
    private val repository = ProfileRepository()
    private val _profile = MutableStateFlow<ProfileResponse?>(null)
    val profile = _profile.asStateFlow()

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
}
