package com.yourapp.music.presentation.library.tabs

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourapp.music.core.theme.AccentGreen
import com.yourapp.music.core.theme.ErrorRed
import com.yourapp.music.domain.model.Playlist
import com.yourapp.music.presentation.components.PlaylistRowItem
import com.yourapp.music.presentation.playlist.PlaylistOptionsSheet
import com.yourapp.music.presentation.playlist.PlaylistViewModel

@Composable
fun PlaylistsTab(
    playlists: List<Playlist>,
    onPlaylistClick: (Playlist) -> Unit,
    modifier: Modifier = Modifier,
    // Separate PlaylistViewModel instance purely for the kebab menu's
    // rename/delete actions — the visible `playlists` list itself still
    // comes from LibraryViewModel above, both already read the same live
    // Room data so they never disagree.
    playlistViewModel: PlaylistViewModel = hiltViewModel()
) {
    val menuState by playlistViewModel.uiState.collectAsState()

    LazyColumn(modifier = modifier) {
        items(playlists, key = { it.id }) { playlist ->
            PlaylistRowItem(
                playlist = playlist,
                onClick = { onPlaylistClick(playlist) },
                onMenuClick = {
                    playlistViewModel.onShowOptions(
                        playlist
                    )
                }
            )
        }
    }

    menuState.playlistForOptions?.let { playlist ->
        PlaylistOptionsSheet(
            playlist = playlist,
            onDismiss = playlistViewModel::onDismissOptions,
            onRenameClick = playlistViewModel::onRequestRename,
            onDeleteClick = playlistViewModel::onRequestDelete
        )
    }

    menuState.playlistBeingRenamed?.let {
        AlertDialog(
            onDismissRequest = playlistViewModel::onCancelRename,
            title = { Text("Rename Playlist") },
            text = {
                OutlinedTextField(
                    value = menuState.renameTitle,
                    onValueChange = playlistViewModel::onRenameTitleChange,
                    placeholder = { Text("Playlist name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = playlistViewModel::confirmRename) {
                    Text(
                        "Save",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = playlistViewModel::onCancelRename) {
                    Text(
                        "Cancel"
                    )
                }
            }
        )
    }

    menuState.playlistPendingDelete?.let { playlist ->
        AlertDialog(
            onDismissRequest = playlistViewModel::onCancelDelete,
            title = { Text("Delete Playlist") },
            text = { Text("Delete \"${playlist.title}\"? This can't be undone.") },
            confirmButton = {
                TextButton(onClick = playlistViewModel::confirmDelete) {
                    Text("Delete", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = playlistViewModel::onCancelDelete) {
                    Text(
                        "Cancel"
                    )
                }
            }
        )
    }
}
