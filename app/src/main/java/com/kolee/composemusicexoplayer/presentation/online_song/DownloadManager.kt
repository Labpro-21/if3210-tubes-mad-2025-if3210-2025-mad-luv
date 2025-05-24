package com.kolee.composemusicexoplayer.presentation.online_song

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.kolee.composemusicexoplayer.data.auth.UserPreferences
import com.kolee.composemusicexoplayer.data.roomdb.MusicEntity
import com.kolee.composemusicexoplayer.data.roomdb.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import javax.inject.Inject

class DownloadManager @Inject constructor(
    private val context: Context,
    private val musicRepository: MusicRepository,
    private val userPreferences: UserPreferences
) {
    private val downloadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @OptIn(UnstableApi::class)
    suspend fun downloadMusic(musicList: List<MusicEntity>): Boolean {
        return try {
            // Ensure all network operations happen on IO dispatcher
            withContext(Dispatchers.IO) {
                musicList.forEach { music ->
                    val audioUrl = music.audioPath
                    val fileExtension = audioUrl.substringAfterLast('.', "mp3")
                    val fileName = "${music.audioId}_${System.currentTimeMillis()}.$fileExtension"
                    val outputFile = File(context.getExternalFilesDir(null), fileName)

                    URL(audioUrl).openStream().use { input ->
                        outputFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    val downloadedMusic = music.copy(
                        audioPath = outputFile.absolutePath,
                        owner = userPreferences.getUserEmail.first() ?: "unknown",
                        isDownloaded = true
                    )

                    musicRepository.updateMusic(downloadedMusic)

                    val downloadedMusics = musicRepository.getDownloadedMusic().first()
                    android.util.Log.d("DownloadManager", "Downloaded musics " +
                            downloadedMusics.joinToString("\n") { music ->
                                "ID: ${music.audioId}, Title: ${music.title}, Path: ${music.audioPath}"
                            }
                    )
                }
            }
            true
        } catch (e: Exception) {
            Log.e("DownloadManager", "Download failed", e)
            false
        }
    }

    fun cancelAllDownloads() {
        downloadScope.coroutineContext.cancelChildren()
    }
}