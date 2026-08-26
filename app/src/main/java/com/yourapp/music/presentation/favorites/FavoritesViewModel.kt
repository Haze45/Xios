package com.yourapp.music.presentation.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourapp.music.core.sync.FavoriteSyncManager
import com.yourapp.music.domain.usecase.auth.GetCurrentUserUseCase
import com.yourapp.music.domain.usecase.favorite.GetFavoritesUseCase
import com.yourapp.music.domain.usecase.favorite.RemoveFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val getFavoritesUseCase: GetFavoritesUseCase,
    private val removeFavoriteUseCase: RemoveFavoriteUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val favoriteSyncManager: FavoriteSyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    private val userId: String? get() = getCurrentUserUseCase()?.uid

    init {
        val uid = userId
        if (uid != null) {
            viewModelScope.launch {
                getFavoritesUseCase(uid).collect { songs ->
                    _uiState.update { it.copy(songs = songs) }
                }
            }
        }
        // Reconcile with Supabase in case a favorite was added/removed on another device.
        favoriteSyncManager.enqueueSync()
    }

    /** Unlike a song directly from this screen (tap the heart again to remove it). */
    fun removeFavorite(songId: String) {
        val uid = userId ?: return
        viewModelScope.launch {
            removeFavoriteUseCase(songId, uid)
            favoriteSyncManager.enqueueSync()
        }
    }
}
