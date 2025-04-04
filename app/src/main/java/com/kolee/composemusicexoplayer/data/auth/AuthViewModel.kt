package com.kolee.composemusicexoplayer.data.auth

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.work.*
import com.kolee.composemusicexoplayer.worker.TokenRefreshWorker
import java.util.concurrent.TimeUnit


class AuthViewModel(private val context: Context) : ViewModel() {
    private val userPreferences = UserPreferences(context)
    private val repository = AuthRepository()
    private val _userName = MutableStateFlow("")
    private val _email = MutableStateFlow("")

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn = _isLoggedIn.asStateFlow()
    val userName = _userName.asStateFlow()
    val email = _email.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferences.isLoggedIn.collect { loggedIn ->
                _isLoggedIn.value = loggedIn
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
                        userPreferences.saveUserInfo(name, email)
                        setLoggedIn(true)
                        scheduleTokenRefreshWorker()
                        onResult(true)
                    } ?: onResult(false)
                } else {
                    Log.e("Login", "Failed: ${response.code()} ${response.errorBody()?.string()}")
                    onResult(false)
                }
            } catch (e: Exception) {
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
            WorkManager.getInstance(context).cancelUniqueWork("TokenRefreshWorker")
        }
    }

    private fun scheduleTokenRefreshWorker() {
        val workRequest = PeriodicWorkRequestBuilder<TokenRefreshWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "TokenRefreshWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }


}
