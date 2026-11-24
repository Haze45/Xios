package com.yourapp.music.domain.repository

import com.yourapp.music.core.util.Resource
import com.yourapp.music.domain.model.DownloadProgress
import com.yourapp.music.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {

    fun getDownloadedSongs(): Flow<List<Song>>

    fun isDownloaded(songId: String): Flow<Boolean>

    fun isDownloading(songId: String): Flow<Boolean>

    fun observeDownloadProgress(): Flow<List<DownloadProgress>>

    suspend fun downloadSong(song: Song): Resource<Unit>
}
