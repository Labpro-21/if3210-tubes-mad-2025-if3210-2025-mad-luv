package com.kolee.composemusicexoplayer.data.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kolee.composemusicexoplayer.data.auth.UserPreferences

class ProfileViewModelFactory(private val userPreferences: UserPreferences) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(userPreferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
