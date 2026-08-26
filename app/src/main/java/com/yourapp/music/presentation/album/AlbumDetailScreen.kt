package com.yourapp.music.presentation.album

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.yourapp.music.core.theme.AccentGreen
import com.yourapp.music.core.theme.BackgroundBlack
import com.yourapp.music.core.theme.TextSecondary
import com.yourapp.music.domain.model.Song
import com.yourapp.music.presentation.components.SongListItem

@Composable
fun AlbumDetailScreen(
    onBackClick: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    viewModel: AlbumDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val album = state.album

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBlack)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Album", style = MaterialTheme.typography.titleMedium)
        }

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (album == null) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text(
                    state.errorMessage ?: "Album not found",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = album.coverUrl,
                    contentDescription = album.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(album.title, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${album.artistName} • ${album.songCount} Songs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { onPlayAll(state.songs) },
                enabled = state.songs.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = BackgroundBlack),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Play All")
            }

            Spacer(Modifier.height(16.dp))

            LazyColumn {
                items(state.songs, key = { it.id }) { song ->
                    SongListItem(
                        song = song,
                        onClick = { onSongClick(song, state.songs) },
                        onPlayClick = { onSongClick(song, state.songs) }
                    )
                }
            }
        }
    }
}
