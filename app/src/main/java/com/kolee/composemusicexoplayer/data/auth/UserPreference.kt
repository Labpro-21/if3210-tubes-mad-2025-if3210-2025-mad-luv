package com.kolee.composemusicexoplayer.data.auth

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class UserPreferences(private val context: Context) {
    companion object {
        private val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val TOKEN_KEY = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val USER_NAME = stringPreferencesKey("user_name")
        private val USER_EMAIL = stringPreferencesKey("user_email")
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[IS_LOGGED_IN] ?: false
    }

    val getUserName: Flow<String?> = context.dataStore.data.map { it[USER_NAME] }
    val getUserEmail: Flow<String?> = context.dataStore.data.map { it[USER_EMAIL] }
    val getToken: Flow<String?> = context.dataStore.data.map { it[TOKEN_KEY] }
    val getRefreshToken: Flow<String?> = context.dataStore.data.map { it[REFRESH_TOKEN_KEY] }

    suspend fun getTokenOnce(): String {
        return context.dataStore.data.map { it[TOKEN_KEY] ?: "" }.first()
    }

    suspend fun getRefreshTokenOnce(): String {
        return context.dataStore.data.map { it[REFRESH_TOKEN_KEY] ?: "" }.first()
    }

    suspend fun setLoggedIn(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_LOGGED_IN] = value
        }
    }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
        }
    }

    suspend fun saveRefreshToken(refreshToken: String) {
        context.dataStore.edit { prefs ->
            prefs[REFRESH_TOKEN_KEY] = refreshToken
        }
    }

    suspend fun saveUserInfo(name: String, email: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_NAME] = name
            prefs[USER_EMAIL] = email
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
