package com.kolee.composemusicexoplayer.data.network

import com.kolee.composemusicexoplayer.data.model.OnlineSong
import retrofit2.http.GET
import retrofit2.http.Path

interface SongService {
    @GET("api/songs/{id}")
    suspend fun getSongById(@Path("id") id: Long): OnlineSong
}