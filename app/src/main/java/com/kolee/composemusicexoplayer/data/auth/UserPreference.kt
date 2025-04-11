package com.kolee.composemusicexoplayer.data.auth

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.kolee.composemusicexoplayer.data.roomdb.MusicEntity
import com.kolee.composemusicexoplayer.data.roomdb.MusicRepository
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

    private val gson = Gson()

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

    // =====================
    // MUSIC LIST MANAGEMENT
    // =====================

    private fun keyForList(base: String, email: String): Preferences.Key<String> =
        stringPreferencesKey("${base}_$email")

    private fun keyForInitFlag(email: String): Preferences.Key<Boolean> =
        booleanPreferencesKey("has_initialized_data_$email")

    // Music List
    suspend fun saveMusicList(email: String, list: List<MusicEntity>) {
        val json = gson.toJson(list)
        context.dataStore.edit { prefs ->
            prefs[keyForList("music_list", email)] = json
        }
    }

    fun getMusicList(email: String): Flow<List<MusicEntity>> = context.dataStore.data.map { prefs ->
        prefs[keyForList("music_list", email)]?.let { json ->
            gson.fromJson(json, Array<MusicEntity>::class.java).toList()
        } ?: emptyList()
    }


    // ===========================
    // INITIALIZE ON FIRST LOGIN
    // ===========================

//    suspend fun initializeMusicDataIfNeeded(email: String, initialMusicList: List<MusicEntity>) {
//        val existingList = getMusicList(email).first()
//
//
//        if (existingList.isEmpty()) {
//            val defaultList = initialMusicList.map {
//                it.copy(loved = false, lastPlayedAt = 0L)
//            }
//
//            saveMusicList(email, defaultList)
//
////            context.dataStore.edit { it[initFlagKey] = true }
//        }
//    }
//        suspend fun initializeUserMusicIfNeeded(email: String, defaultList: List<MusicEntity>) {
//            val currentList = musicRepository.getAllMusic(email).first()
//            if (currentList.isEmpty()) {
//                musicRepository.insertMusics(
//                    *defaultList.map {
//                        it.copy(userEmail = email, loved = false, lastPlayedAt = 0L)
//                    }.toTypedArray()
//                )
//            }
//        }


    // =====================
    // CLEAR ALL OR PER USER
    // =====================

    suspend fun clear(email: String? = null) {
        context.dataStore.edit { prefs ->
            prefs.clear()

            email?.let {
//                prefs.remove(keyForInitFlag(it))
//                prefs.remove(keyForList("music_list", it))
            }
        }
    }
}
