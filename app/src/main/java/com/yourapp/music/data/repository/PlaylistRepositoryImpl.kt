package com.yourapp.music.data.repository

import com.yourapp.music.core.util.Resource
import com.yourapp.music.data.local.dao.PlaylistDao
import com.yourapp.music.data.mapper.toDomain
import com.yourapp.music.data.mapper.toEntity
import com.yourapp.music.data.remote.supabase.PlaylistApi
import com.yourapp.music.data.remote.supabase.dto.PlaylistDto
import com.yourapp.music.domain.model.Playlist
import com.yourapp.music.domain.repository.PlaylistRepository
import com.yourapp.music.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val playlistApi: PlaylistApi,
    private val playlistDao: PlaylistDao,
    private val songRepository: SongRepository
) : PlaylistRepository {

    override fun getAllPlaylists(): Flow<List<Playlist>> =
        playlistDao.getAllPlaylists().map { list -> list.map { it.toDomain() } }

    override fun observeUserPlaylists(userId: String): Flow<List<Playlist>> =
        playlistDao.getPlaylistsByOwner(userId).map { list -> list.map { it.toDomain() } }

    override fun observePlaylistById(id: String): Flow<Playlist?> =
        playlistDao.getPlaylistByIdFlow(id).map { it?.toDomain() }

    override suspend fun getTopDailyPlaylists(): Resource<List<Playlist>> = try {
        Resource.Success(playlistApi.getTopDailyPlaylists().map { it.toDomain() })
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Couldn't load daily playlists", e)
    }

    override suspend fun getUserPlaylists(userId: String): Resource<List<Playlist>> = try {
        val remote = playlistApi.getUserPlaylists(userId)
        // Hydrate the Room cache so observeUserPlaylists(userId) has something to
        // filter from and stays live afterward (create/add-song/delete all write
        // straight to Room too, so once this cache exists it never goes stale).
        playlistDao.upsertPlaylists(remote.map { it.toEntity() })
        Resource.Success(remote.map { it.toDomain() })
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Couldn't load your playlists", e)
    }

    override suspend fun getPlaylistById(id: String): Playlist? =
        playlistDao.getPlaylistById(id)?.toDomain()

    override suspend fun createPlaylist(title: String, ownerId: String): Resource<Playlist> = try {
        val dto = PlaylistDto(
            id = UUID.randomUUID().toString(),
            title = title,
            owner_id = ownerId,
            owner_name = "",
            cover_url = "",
            song_count = 0
        )
        val created = playlistApi.createPlaylist(dto)
        playlistDao.upsertPlaylists(listOf(created.toEntity()))
        Resource.Success(created.toDomain())
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Couldn't create playlist", e)
    }

    override suspend fun addSongToPlaylist(playlistId: String, songId: String): Resource<Unit> {
        return try {
            val remotePlaylist = playlistApi.getPlaylistById(playlistId)
                ?: return Resource.Error("Playlist not found")

            if (songId in remotePlaylist.song_ids) {
                // Already in this playlist — nothing to do, not an error.
                return Resource.Success(Unit)
            }

            val updatedSongIds = remotePlaylist.song_ids + songId
            applyUpdatedSongIds(playlistId, remotePlaylist, updatedSongIds)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Couldn't add song to playlist", e)
        }
    }

    override suspend fun removeSongFromPlaylist(playlistId: String, songId: String): Resource<Unit> {
        return try {
            val remotePlaylist = playlistApi.getPlaylistById(playlistId)
                ?: return Resource.Error("Playlist not found")

            if (songId !in remotePlaylist.song_ids) {
                // Wasn't in there anyway — nothing to do, not an error.
                return Resource.Success(Unit)
            }

            val updatedSongIds = remotePlaylist.song_ids - songId
            applyUpdatedSongIds(playlistId, remotePlaylist, updatedSongIds)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Couldn't remove song from playlist", e)
        }
    }

    /**
     * Shared by add/remove: writes the new song_ids list to Supabase and
     * Room, and always recomputes the cover as the LAST song's artwork
     * in the resulting list — so adding a song makes it the new cover,
     * and removing the song that was the cover falls back to whichever
     * song is now last, or blank if the playlist becomes empty.
     */
    private suspend fun applyUpdatedSongIds(
        playlistId: String,
        remotePlaylist: PlaylistDto,
        updatedSongIds: List<String>
    ) {
        val newCoverUrl = if (updatedSongIds.isEmpty()) {
            "" // no songs left at all — nothing to show a cover for
        } else {
            updatedSongIds.last()
                .let { songRepository.getSongById(it) }
                ?.albumArtUrl
                ?.takeIf { it.isNotBlank() }
                ?: remotePlaylist.cover_url // last song's own art is missing — keep whatever cover it already had
        }

        playlistApi.updatePlaylistSongIds(playlistId, updatedSongIds, updatedSongIds.size, newCoverUrl)

        val updatedEntity = remotePlaylist.toEntity().copy(
            songIds = updatedSongIds,
            songCount = updatedSongIds.size,
            coverUrl = newCoverUrl
        )
        playlistDao.upsertPlaylists(listOf(updatedEntity))
    }

    override suspend fun renamePlaylist(playlistId: String, newTitle: String): Resource<Unit> = try {
        playlistApi.renamePlaylist(playlistId, newTitle)
        // Keep the Room cache in sync so the new name shows up immediately everywhere.
        val existing = playlistDao.getPlaylistById(playlistId)
        if (existing != null) {
            playlistDao.upsertPlaylists(listOf(existing.copy(title = newTitle)))
        }
        Resource.Success(Unit)
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Couldn't rename playlist", e)
    }

    override suspend fun deletePlaylist(playlistId: String): Resource<Unit> = try {
        playlistApi.deletePlaylist(playlistId)
        playlistDao.deletePlaylist(playlistId)
        Resource.Success(Unit)
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Couldn't delete playlist", e)
    }

    override suspend fun refreshPlaylists(): Resource<Unit> = try {
        val remote = playlistApi.getAllPlaylists()
        playlistDao.upsertPlaylists(remote.map { it.toEntity() })
        Resource.Success(Unit)
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Couldn't refresh playlists", e)
    }
}
