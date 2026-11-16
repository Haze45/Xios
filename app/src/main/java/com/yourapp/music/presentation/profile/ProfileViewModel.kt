package com.yourapp.music.presentation.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourapp.music.core.util.Resource
import com.yourapp.music.domain.usecase.artist.FollowArtistUseCase
import com.yourapp.music.domain.usecase.artist.GetFollowedArtistsUseCase
import com.yourapp.music.domain.usecase.artist.RefreshFollowedArtistsUseCase
import com.yourapp.music.domain.usecase.artist.UnfollowArtistUseCase
import com.yourapp.music.domain.usecase.auth.GetCurrentUserUseCase
import com.yourapp.music.domain.usecase.auth.LogoutUseCase
import com.yourapp.music.domain.usecase.playlist.GetUserPlaylistsUseCase
import com.yourapp.music.domain.usecase.playlist.ObserveUserPlaylistsUseCase
import com.yourapp.music.domain.usecase.profile.GetUserStatsUseCase
import com.yourapp.music.domain.usecase.profile.SyncProfileUseCase
import com.yourapp.music.domain.usecase.profile.UpdateProfileUseCase
import com.yourapp.music.domain.usecase.profile.UploadProfilePhotoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getUserStatsUseCase: GetUserStatsUseCase,
    private val getUserPlaylistsUseCase: GetUserPlaylistsUseCase,
    private val observeUserPlaylistsUseCase: ObserveUserPlaylistsUseCase,
    private val getFollowedArtistsUseCase: GetFollowedArtistsUseCase,
    private val refreshFollowedArtistsUseCase: RefreshFollowedArtistsUseCase,
    private val followArtistUseCase: FollowArtistUseCase,
    private val unfollowArtistUseCase: UnfollowArtistUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val uploadProfilePhotoUseCase: UploadProfilePhotoUseCase,
    private val syncProfileUseCase: SyncProfileUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val userId: String? get() = getCurrentUserUseCase()?.uid

    init {
        val user = getCurrentUserUseCase()
        _uiState.update {
            it.copy(
                displayName = user?.displayName ?: user?.email?.substringBefore("@").orEmpty(),
                photoUrl = user?.photoUrl
            )
        }
        loadStats()
        loadPlaylists()
        observeFollowedArtists()
        refreshFollowedArtists()
        syncProfileCountsOnChange()
    }

    /**
     * Keeps Supabase's profiles row honest: this app only ever has real
     * data for playlist count and followed-artist count locally, so
     * that's what gets pushed up — following_count here means "artists
     * you follow", not a user-to-user follow feature (this app doesn't
     * have one yet). Fires once with the initial state, then again any
     * time either list's size actually changes.
     */
    private fun syncProfileCountsOnChange() {
        val uid = userId ?: return
        viewModelScope.launch {
            _uiState
                .map { it.playlists.size to it.favoriteArtists.size }
                .distinctUntilChanged()
                .collect { (playlistCount, followingCount) ->
                    syncProfileUseCase(uid, followingCount, playlistCount)
                }
        }
    }

    private fun loadStats() {
        val uid = userId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = getUserStatsUseCase(uid)) {
                is Resource.Success -> _uiState.update { it.copy(stats = result.data, isLoading = false) }
                is Resource.Error -> _uiState.update { it.copy(errorMessage = result.message, isLoading = false) }
                is Resource.Loading -> Unit
            }
        }
    }

    private fun loadPlaylists() {
        val uid = userId ?: return
        viewModelScope.launch {
            // One-shot hydration from Supabase into Room...
            when (val result = getUserPlaylistsUseCase(uid)) {
                is Resource.Error -> _uiState.update { it.copy(errorMessage = result.message) }
                else -> Unit
            }
            // ...then observe Room live from then on, so this grid updates
            // instantly when a song is added to a playlist anywhere in the app.
            observeUserPlaylistsUseCase(uid).collect { playlists ->
                _uiState.update { it.copy(playlists = playlists) }
            }
        }
    }

    private fun observeFollowedArtists() {
        val uid = userId ?: return
        viewModelScope.launch {
            getFollowedArtistsUseCase(uid).collect { artists ->
                _uiState.update { it.copy(favoriteArtists = artists) }
            }
        }
    }

    private fun refreshFollowedArtists() {
        val uid = userId ?: return
        viewModelScope.launch { refreshFollowedArtistsUseCase(uid) }
    }

    fun toggleFollow(artistId: String, isCurrentlyFollowing: Boolean) {
        val uid = userId ?: return
        viewModelScope.launch {
            if (isCurrentlyFollowing) unfollowArtistUseCase(artistId, uid) else followArtistUseCase(artistId, uid)
        }
    }

    /**
     * Saves a new display name and/or photo from the edit-profile sheet.
     * [photoUri] should be a URI string pointing at a local, durable copy
     * of the image (see EditProfileSheet) — pass null to leave the photo
     * unchanged. If a photo is provided, it's uploaded to Supabase
     * Storage first (a local file:// path only exists on the device it
     * was picked on — Firebase's photoUrl needs to be a real public URL
     * so every device shows the same photo, not just the one it was set
     * from). [onSaved] fires only on success, so the sheet knows when
     * it's safe to dismiss itself.
     */
    fun updateProfile(displayName: String, photoUri: String?, onSaved: () -> Unit) {
        val trimmedName = displayName.trim()
        if (trimmedName.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Name can't be empty") }
            return
        }
        val uid = userId
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            var resolvedPhotoUrl = photoUri
            if (photoUri != null && uid != null) {
                val bytes = try {
                    appContext.contentResolver.openInputStream(android.net.Uri.parse(photoUri))?.use { it.readBytes() }
                } catch (e: Exception) {
                    null
                }
                if (bytes == null) {
                    _uiState.update { it.copy(isSaving = false, errorMessage = "Couldn't read the selected photo") }
                    return@launch
                }
                when (val uploadResult = uploadProfilePhotoUseCase(uid, bytes)) {
                    is Resource.Success -> resolvedPhotoUrl = uploadResult.data
                    is Resource.Error -> {
                        _uiState.update { it.copy(isSaving = false, errorMessage = uploadResult.message) }
                        return@launch
                    }
                    is Resource.Loading -> Unit
                }
            }

            when (val result = updateProfileUseCase(trimmedName, resolvedPhotoUrl)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            displayName = result.data.displayName ?: trimmedName,
                            photoUrl = result.data.photoUrl ?: it.photoUrl,
                            isSaving = false
                        )
                    }
                    onSaved()
                }
                is Resource.Error -> _uiState.update { it.copy(errorMessage = result.message, isSaving = false) }
                is Resource.Loading -> Unit
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    fun logout() = logoutUseCase()
}
