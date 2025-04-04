package com.kolee.composemusicexoplayer.data.auth

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kolee.composemusicexoplayer.data.roomdb.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val context: Context) : ViewModel() {
    private val userPreferences = UserPreferences(context)
    private val repository = AuthRepository()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn = _isLoggedIn.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferences.isLoggedIn.collect { loggedIn ->
                _isLoggedIn.value = loggedIn
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
                        setLoggedIn(true)
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
}
