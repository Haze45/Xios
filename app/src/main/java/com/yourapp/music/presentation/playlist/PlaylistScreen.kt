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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourapp.music.R
import com.yourapp.music.core.theme.ErrorRed
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
            .background(MaterialTheme.colorScheme.background)

    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            Text(
                stringResource(R.string.playlists),
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

                state.playlists.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.no_playlists_yet),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.padding(bottom = 80.dp)
                    ) {
                        items(
                            state.playlists,
                            key = { it.id }
                        ) { playlist ->
                            PlaylistRowItem(
                                playlist = playlist,
                                onClick = {
                                    onPlaylistClick(playlist)
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
            onClick = {
                viewModel.onShowCreateDialog(true)
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 20.dp,
                    bottom = 200.dp
                )
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = stringResource(R.string.create_playlist)
            )
        }
    }

    if (state.showCreateDialog) {
        AlertDialog(
            onDismissRequest = {
                viewModel.onShowCreateDialog(false)
            },
            title = {
                Text(stringResource(R.string.new_playlist))
            },
            text = {
                OutlinedTextField(
                    value = state.newPlaylistTitle,
                    onValueChange = viewModel::onNewPlaylistTitleChange,
                    placeholder = {
                        Text(stringResource(R.string.playlist_name))
                    },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::createPlaylist) {
                    Text(
                        stringResource(R.string.create),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.onShowCreateDialog(false)
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

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
            title = {
                Text(stringResource(R.string.rename_playlist))
            },
            text = {
                OutlinedTextField(
                    value = state.renameTitle,
                    onValueChange = viewModel::onRenameTitleChange,
                    placeholder = {
                        Text(stringResource(R.string.playlist_name))
                    },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmRename) {
                    Text(
                        stringResource(R.string.save),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onCancelRename) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    state.playlistPendingDelete?.let { playlist ->
        AlertDialog(
            onDismissRequest = viewModel::onCancelDelete,
            title = {
                Text(stringResource(R.string.delete_playlist))
            },
            text = {
                Text(
                    stringResource(
                        R.string.delete_playlist_confirmation,
                        playlist.title
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) {
                    Text(
                        stringResource(R.string.delete),
                        color = ErrorRed
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onCancelDelete) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
