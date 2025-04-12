package com.kolee.composemusicexoplayer.data.auth

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
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

    val isLoggedIn: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[IS_LOGGED_IN] ?: false }

    val getUserName: Flow<String?> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[USER_NAME] }

    val getUserEmail: Flow<String?> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[USER_EMAIL] }

    suspend fun getTokenOnce(): String {
        val encoded = context.dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { it[TOKEN_KEY] ?: "" }
            .first()
        return decodeBase64(encoded)
    }

    suspend fun getRefreshTokenOnce(): String {
        val encoded = context.dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { it[REFRESH_TOKEN_KEY] ?: "" }
            .first()
        return decodeBase64(encoded)
    }

    suspend fun setLoggedIn(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_LOGGED_IN] = value
        }
    }

    suspend fun saveToken(token: String) {
        if (token.isNotBlank()) {
            val encoded = encodeBase64(token)
            context.dataStore.edit { prefs ->
                prefs[TOKEN_KEY] = encoded
            }
        }
    }

    suspend fun saveRefreshToken(refreshToken: String) {
        if (refreshToken.isNotBlank()) {
            val encoded = encodeBase64(refreshToken)
            context.dataStore.edit { prefs ->
                prefs[REFRESH_TOKEN_KEY] = encoded
            }
        }
    }

    suspend fun saveUserInfo(name: String, email: String) {
        if (email.contains("@")) {
            context.dataStore.edit { prefs ->
                prefs[USER_NAME] = name
                prefs[USER_EMAIL] = email
            }
        }
    }

    private fun encodeBase64(input: String): String {
        return Base64.encodeToString(input.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    private fun decodeBase64(encoded: String): String {
        return if (encoded.isNotEmpty()) {
            String(Base64.decode(encoded, Base64.NO_WRAP), Charsets.UTF_8)
        } else ""
    }
}
