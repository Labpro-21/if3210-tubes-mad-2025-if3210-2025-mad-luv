package com.kolee.composemusicexoplayer.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kolee.composemusicexoplayer.data.auth.UserPreferences
import com.kolee.composemusicexoplayer.data.network.ApiClient
import kotlinx.coroutines.flow.first

class TokenRefreshWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val userPreferences = UserPreferences(context)
    private val apiService = ApiClient.apiService

    override suspend fun doWork(): Result {
        val token = userPreferences.getToken.first()
        val refreshToken = userPreferences.getRefreshToken.first()

        if (token.isNullOrBlank() || refreshToken.isNullOrBlank()) {
            return Result.failure()
        }

        return try {
            val verifyResponse = apiService.verifyToken("Bearer $token")
            if (verifyResponse.isSuccessful) {
                Result.success()
            } else if (verifyResponse.code() == 403) {
                val newTokenResponse = apiService.refreshToken(mapOf("refreshToken" to refreshToken))
                userPreferences.saveToken(newTokenResponse.accessToken)
                userPreferences.saveRefreshToken(newTokenResponse.refreshToken)
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

}
