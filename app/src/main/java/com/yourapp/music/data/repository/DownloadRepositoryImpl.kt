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

    private val downloadScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val downloadingIds =
        MutableStateFlow<Set<String>>(emptySet())

    override fun getDownloadedSongs(): Flow<List<Song>> =
        combine(
            downloadDao.getAllDownloads(),
            songRepository.getAllSongs()
        ) { downloads, allSongs ->

            val songsById = allSongs.associateBy { it.id }

            downloads.mapNotNull { download ->
                songsById[download.songId]?.copy(
                    audioUrl = download.localUri
                )
            }
        }

    override fun isDownloaded(songId: String): Flow<Boolean> =
        downloadDao.isDownloaded(songId)

    override fun isDownloading(songId: String): Flow<Boolean> =
        downloadingIds.map { songId in it }

    override suspend fun downloadSong(song: Song): Resource<Unit> {
        var alreadyRunning = false

        downloadingIds.update { current ->
            if (song.id in current) {
                alreadyRunning = true
                current
            } else {
                current + song.id
            }
        }

        if (alreadyRunning) return Resource.Loading

        return downloadScope.async {
            try {
                val encryptedUri =
                    songDownloader.download(song)

                downloadDao.upsertDownload(
                    DownloadedSongEntity(
                        songId = song.id,
                        localUri = encryptedUri,
                        downloadedAt = System.currentTimeMillis()
                    )
                )

                Resource.Success(Unit)
            } catch (exception: Exception) {
                Resource.Error(
                    exception.message
                        ?: "Couldn't download this song",
                    exception
                )
            } finally {
                downloadingIds.update { it - song.id }
            }
        }.await()
    }
}
