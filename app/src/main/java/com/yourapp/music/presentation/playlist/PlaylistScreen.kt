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
import com.yourapp.music.core.theme.TextSecondary
import com.yourapp.music.domain.model.Playlist
import com.yourapp.music.presentation.components.PlaylistRowItem

@Composable
fun PlaylistScreen(
    onPlaylistClick: (Playlist) -> Unit,
    viewModel: PlaylistViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(BackgroundBlack)) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(16.dp))
            Text("Playlists", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))

            when {
                state.isLoading -> Box(
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                state.playlists.isEmpty() -> Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No playlists yet — tap + to create your first one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                else -> LazyColumn(modifier = Modifier.padding(bottom = 80.dp)) {
                    items(state.playlists, key = { it.id }) { playlist ->
                        PlaylistRowItem(
                            playlist = playlist,
                            onClick = { onPlaylistClick(playlist) }
                        )
                    }
                }
            }

            state.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
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
            Icon(Icons.Filled.Add, contentDescription = "Create playlist")
        }
    }

    if (state.showCreateDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onShowCreateDialog(false) },
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
                TextButton(onClick = viewModel::createPlaylist) { Text("Create", color = AccentGreen) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onShowCreateDialog(false) }) { Text("Cancel") }
            }
        )
    }
}
