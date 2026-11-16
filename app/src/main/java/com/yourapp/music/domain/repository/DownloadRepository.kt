package com.yourapp.music.domain.repository

import com.yourapp.music.core.util.Resource
import com.yourapp.music.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    /** Live list of every downloaded song, most recent first. */
    fun getDownloadedSongs(): Flow<List<Song>>

    fun isDownloaded(songId: String): Flow<Boolean>

    /** True while a download for this song is in flight — survives leaving/re-entering screens. */
    fun isDownloading(songId: String): Flow<Boolean>

    /**
     * Downloads the song's audio file to the device and records it as
     * downloaded. Runs independently of the caller's lifecycle, so
     * navigating away mid-download doesn't cancel it. Returns Loading
     * (nothing to report) if this song is already being downloaded.
     */
    suspend fun downloadSong(song: Song): Resource<Unit>
}
