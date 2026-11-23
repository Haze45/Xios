package com.yourapp.music.presentation.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourapp.music.core.theme.AccentGreen
import com.yourapp.music.core.theme.BackgroundBlack
import com.yourapp.music.core.theme.ErrorRed
import com.yourapp.music.core.theme.TextSecondary
import com.yourapp.music.domain.model.Playlist
import com.yourapp.music.presentation.components.PlaylistRowItem
import com.yourapp.music.presentation.components.ShimmerPlaylistList

@Composable
fun PlaylistScreen(
    onPlaylistClick: (Playlist) -> Unit,
    viewModel: PlaylistViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBlack)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(16.dp))
            Text(
                "Playlists",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(16.dp))

            when {
                state.isLoading -> {
                    ShimmerPlaylistList(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 80.dp),
                        count = 5
                    )
                }

                state.playlists.isEmpty() -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No playlists yet — tap + to create your first one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                else -> LazyColumn(
                    modifier = Modifier.padding(
                        bottom = 80.dp
                    )
                ) {
                    items(
                        state.playlists,
                        key = { it.id }) { playlist ->
                        PlaylistRowItem(
                            playlist = playlist,
                            onClick = {
                                onPlaylistClick(
                                    playlist
                                )
                            },
                            onMenuClick = {
                                viewModel.onShowOptions(
                                    playlist
                                )
                            }
                        )
                    }
                }
            }

            state.errorMessage?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        FloatingActionButton(
            onClick = { viewModel.onShowCreateDialog(true) },
            containerColor = AccentGreen,
            contentColor = BackgroundBlack,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = "Create playlist"
            )
        }
    }

    if (state.showCreateDialog) {
        AlertDialog(
            onDismissRequest = {
                viewModel.onShowCreateDialog(
                    false
                )
            },
            title = { Text("New Playlist") },
            text = {
                OutlinedTextField(
                    value = state.newPlaylistTitle,
                    onValueChange = viewModel::onNewPlaylistTitleChange,
                    placeholder = { Text("Playlist name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::createPlaylist) {
                    Text(
                        "Create",
                        color = AccentGreen
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.onShowCreateDialog(
                        false
                    )
                }) { Text("Cancel") }
            }
        )
    }

    // Kebab menu — cover art + name, then Rename / Delete.
    state.playlistForOptions?.let { playlist ->
        PlaylistOptionsSheet(
            playlist = playlist,
            onDismiss = viewModel::onDismissOptions,
            onRenameClick = viewModel::onRequestRename,
            onDeleteClick = viewModel::onRequestDelete
        )
    }

    state.playlistBeingRenamed?.let {
        AlertDialog(
            onDismissRequest = viewModel::onCancelRename,
            title = { Text("Rename Playlist") },
            text = {
                OutlinedTextField(
                    value = state.renameTitle,
                    onValueChange = viewModel::onRenameTitleChange,
                    placeholder = { Text("Playlist name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmRename) {
                    Text(
                        "Save",
                        color = AccentGreen
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onCancelRename) {
                    Text(
                        "Cancel"
                    )
                }
            }
        )
    }

    state.playlistPendingDelete?.let { playlist ->
        AlertDialog(
            onDismissRequest = viewModel::onCancelDelete,
            title = { Text("Delete Playlist") },
            text = { Text("Delete \"${playlist.title}\"? This can't be undone.") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) {
                    Text("Delete", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onCancelDelete) {
                    Text(
                        "Cancel"
                    )
                }
            }
        )
    }
}
