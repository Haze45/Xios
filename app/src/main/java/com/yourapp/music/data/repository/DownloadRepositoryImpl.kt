package com.yourapp.music.data.repository

import com.yourapp.music.core.download.SongDownloader
import com.yourapp.music.core.util.Resource
import com.yourapp.music.data.local.dao.DownloadDao
import com.yourapp.music.data.local.entity.DownloadedSongEntity
import com.yourapp.music.domain.model.Song
import com.yourapp.music.domain.repository.DownloadRepository
import com.yourapp.music.domain.repository.SongRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    private val songDownloader: SongDownloader,
    private val downloadDao: DownloadDao,
    private val songRepository: SongRepository
) : DownloadRepository {

    // Downloads are owned by this singleton, not by whichever screen's
    // ViewModel kicked them off — otherwise leaving the Player screen
    // mid-download would clear that ViewModel and cancel the download.
    private val downloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val downloadingIds = MutableStateFlow<Set<String>>(emptySet())

    /**
     * Resolves each downloaded song's id against the cached song catalog,
     * keeping most-recently-downloaded first. Live: updates the moment a
     * download finishes, or if the underlying catalog changes.
     */
    override fun getDownloadedSongs(): Flow<List<Song>> =
        combine(downloadDao.getAllDownloads(), songRepository.getAllSongs()) { downloads, allSongs ->
            val songsById = allSongs.associateBy { it.id }
            downloads.mapNotNull { songsById[it.songId] }
        }

    override fun isDownloaded(songId: String): Flow<Boolean> = downloadDao.isDownloaded(songId)

    override fun isDownloading(songId: String): Flow<Boolean> =
        downloadingIds.map { songId in it }

    override suspend fun downloadSong(song: Song): Resource<Unit> {
        // Guard against a second tap (or a second screen instance) starting a duplicate file.
        var alreadyRunning = false
        downloadingIds.update { current ->
            if (song.id in current) {
                alreadyRunning = true
                current
            } else {
                current + song.id
            }
        }
        // Already in flight elsewhere — nothing for this caller to report.
        if (alreadyRunning) return Resource.Loading

        // async in our own scope: if the caller gets cancelled (screen left),
        // only its await() is cancelled — the download itself keeps going.
        return downloadScope.async<Resource<Unit>> {
            try {
                val localUri = songDownloader.download(song)
                downloadDao.upsertDownload(
                    DownloadedSongEntity(
                        songId = song.id,
                        localUri = localUri,
                        downloadedAt = System.currentTimeMillis()
                    )
                )
                Resource.Success(Unit)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Couldn't download this song", e)
            } finally {
                downloadingIds.update { it - song.id }
            }
        }.await()
    }
}
