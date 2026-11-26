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
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourapp.music.R
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
            title = { Text(stringResource(R.string.rename_playlist)) },
            text = {
                OutlinedTextField(
                    value = menuState.renameTitle,
                    onValueChange = playlistViewModel::onRenameTitleChange,
                    placeholder = { Text(stringResource(R.string.playlist_name)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = playlistViewModel::confirmRename) {
                    Text(
                        stringResource(R.string.save),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = playlistViewModel::onCancelRename) {
                    Text(
                        stringResource(R.string.cancel)
                    )
                }
            }
        )
    }

    menuState.playlistPendingDelete?.let { playlist ->
        AlertDialog(
            onDismissRequest = playlistViewModel::onCancelDelete,
            title = { Text(stringResource(R.string.delete_playlist)) },
            text = { Text(stringResource(R.string.delete_playlist_confirmation, playlist.title)) },
            confirmButton = {
                TextButton(onClick = playlistViewModel::confirmDelete) {
                    Text(stringResource(R.string.delete), color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = playlistViewModel::onCancelDelete) {
                    Text(
                        stringResource(R.string.cancel)
                    )
                }
            }
        )
    }
}
