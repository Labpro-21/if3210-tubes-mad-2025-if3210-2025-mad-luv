package com.kolee.composemusicexoplayer.data.auth

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.kolee.composemusicexoplayer.data.network.ApiClient
import com.kolee.composemusicexoplayer.data.network.ApiClient.apiService
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import androidx.work.ListenableWorker.Result
import com.kolee.composemusicexoplayer.data.profile.ProfileRepository

class AuthViewModel(private val context: Context) : ViewModel() {
    private val userPreferences = UserPreferences(context)
    private val repository = AuthRepository()
    private val profileRepository = ProfileRepository()
    private val _userName = MutableStateFlow("")
    private val _email = MutableStateFlow("")

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn = _isLoggedIn.asStateFlow()
    val userName = _userName.asStateFlow()
    val email = _email.asStateFlow()

    private var loginTimestamp: Long = 0
    private val refreshDelayMillis = TimeUnit.MINUTES.toMillis(4)

    init {
        viewModelScope.launch {
            userPreferences.isLoggedIn.collect { loggedIn ->
                _isLoggedIn.value = loggedIn
                if (loggedIn) {
                    startTokenRefreshTimer()
                }
            }
        }
        viewModelScope.launch {
            userPreferences.getUserName.collect {
                _userName.value = it ?: ""
            }
        }
        viewModelScope.launch {
            userPreferences.getUserEmail.collect {
                _email.value = it ?: ""
            }
        }
    }

    fun login(email: String, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val response = repository.login(email, password)
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    loginResponse?.let {
                        userPreferences.saveToken(it.accessToken)
                        userPreferences.saveRefreshToken(it.refreshToken)
                        val name = email.substringBefore("@")
                        val bearerToken = "Bearer ${it.accessToken ?: ""}"
                        try {
                            val response = profileRepository.getProfile(bearerToken)
                            if (response.isSuccessful) {
                                val profile = response.body()
                                Log.d("Profile", "Success: ${profile?.toString() ?: "null body"}")
                                if (profile != null) {
                                    userPreferences.saveUserInfo(name, email, profile.location)
                                }
                            } else {
                                Log.e("Profile", "Error: ${response.code()} - ${response.errorBody()?.string()}")
                            }
                        } catch (e: Exception) {
                            Log.e("Profile", "Exception: ${e.message}")
                        }

                        setLoggedIn(true)
                        loginTimestamp = System.currentTimeMillis()
                        Log.d("Login","${it.accessToken}${it.refreshToken} ")
                        onResult(true)
                    } ?: onResult(false)
                } else {
                    logout()
                    Log.e("Login", "Failed: ${response.code()} ${response.errorBody()?.string()}")
                    onResult(false)
                }
            } catch (e: Exception) {
                logout()
                Log.e("Login", "Exception: ${e.message}")
                onResult(false)
            }
        }
    }

    fun setLoggedIn(value: Boolean) {
        viewModelScope.launch {
            userPreferences.setLoggedIn(value)
        }
    }

    fun logout() {
        viewModelScope.launch {
            userPreferences.setLoggedIn(false)
            userPreferences.saveToken("")
            userPreferences.saveRefreshToken("")
        }
    }

    private fun startTokenRefreshTimer() {
        viewModelScope.launch {
            while (_isLoggedIn.value) {
                delay(refreshDelayMillis)
                val currentTime = System.currentTimeMillis()
                if (currentTime - loginTimestamp >= refreshDelayMillis) {
                    Log.d("AuthViewModel", "Refreshing token after 5 minutes.")
                    refreshToken()
                }
            }
        }
    }

    private suspend fun refreshToken() {
        val token = userPreferences.getTokenOnce()
        val refreshToken = userPreferences.getRefreshTokenOnce()
        if (!token.isNullOrBlank()) {
            val verifyResponse = apiService.verifyToken("Bearer $token")
            if (verifyResponse.isSuccessful) {
                Log.d("AuthViewModel", "Token is still valid.")
                Result.success()
            } else if (verifyResponse.code() == 403) {
                try {
                    val newTokenResponse = apiService.refreshToken(mapOf("refreshToken" to refreshToken!!))
                    userPreferences.saveToken(newTokenResponse.accessToken)
                    userPreferences.saveRefreshToken(newTokenResponse.refreshToken)
                    Log.d("TokenRefresh", "Refreshing token with: $refreshToken")
                    Result.success()
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Error refreshing token: ${e.message}")
                    Result.retry()
                }
            } else {
                try {
                    val newTokenResponse = apiService.refreshToken(mapOf("refreshToken" to refreshToken!!))
                    userPreferences.saveToken(newTokenResponse.accessToken)
                    userPreferences.saveRefreshToken(newTokenResponse.refreshToken)
                    Log.d("TokenRefresh", "Refreshing token with: $refreshToken")
                    Result.success()
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Error refreshing token: ${e.message}")
                    Result.retry()
                }
            }
        } else {
            Log.e("AuthViewModel", "No valid token available for refresh.")
            logout()
            Result.retry()
        }
    }
}
