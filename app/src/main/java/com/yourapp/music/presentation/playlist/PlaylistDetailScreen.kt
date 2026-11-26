package com.yourapp.music.presentation.playlist

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import com.yourapp.music.R
import com.yourapp.music.core.theme.BackgroundBlack
import com.yourapp.music.domain.model.Song
import com.yourapp.music.presentation.components.ShimmerDetailScreen
import com.yourapp.music.presentation.components.SongListItem
import com.yourapp.music.presentation.player.MiniPlayer
import com.yourapp.music.presentation.player.PlayerViewModel
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
@Composable
fun PlaylistDetailScreen(
    onBackClick: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    onExpandPlayer: () -> Unit,
    viewModel: PlaylistDetailViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val playerState by playerViewModel.uiState.collectAsState()
    val playlist = state.playlist

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(
                            R.string.back
                        )
                    )
                }
                Text(
                    stringResource(R.string.playlist),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (state.isLoading) {
                ShimmerDetailScreen(
                    modifier = Modifier.padding(top = 16.dp)
                )
            } else if (playlist == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        state.errorMessage
                            ?: stringResource(R.string.playlist_not_found),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Spacer(Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = playlist.coverUrl,
                        contentDescription = playlist.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )

                    Spacer(Modifier.width(16.dp))

                    Column {
                        Text(
                            playlist.title,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(
                                R.string.playlist_owner_and_song_count,
                                playlist.ownerName,
                                playlist.songCount
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        playlist.description?.let {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = { onPlayAll(state.songs) },
                    enabled = state.songs.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = BackgroundBlack
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.play_all))
                }

                Spacer(Modifier.height(16.dp))

                if (state.songs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.no_songs_in_playlist),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn {
                        items(
                            state.songs,
                            key = { it.id }) { song ->
                            SwipeToRemoveSong(
                                song = song,
                                isCurrentSong = playerState.currentSong?.id == song.id,
                                isActivelyPlaying = playerState.currentSong?.id == song.id &&
                                        playerState.isPlaying,
                                onClick = {
                                    onSongClick(
                                        song,
                                        state.songs
                                    )
                                },
                                onRemove = {
                                    viewModel.removeSong(
                                        song.id
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        MiniPlayer(
            onExpandClick = onExpandPlayer,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

@Composable
private fun SwipeToRemoveSong(
    song: Song,
    isCurrentSong: Boolean,
    isActivelyPlaying: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    var dragOffset by remember(song.id) {
        mutableFloatStateOf(
            0f
        )
    }
    val density = LocalDensity.current
    val revealWidth = with(density) { 112.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary)
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(112.dp)
                .fillMaxHeight()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = stringResource(R.string.remove_song),
                tint = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.remove),
                color = BackgroundBlack,
                fontWeight = FontWeight.SemiBold
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset {
                    IntOffset(
                        dragOffset.roundToInt(),
                        0
                    )
                }
                .background(MaterialTheme.colorScheme.background)
                .pointerInput(song.id) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            dragOffset =
                                (dragOffset + amount)
                                    .coerceIn(
                                        0f,
                                        revealWidth
                                    )
                        },
                        onDragEnd = {
                            if (dragOffset >= revealWidth * 0.65f) {
                                onRemove()
                            }
                            dragOffset = 0f
                        }
                    )
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            SongListItem(
                song = song,
                onClick = onClick,
                onPlayClick = onClick,
                modifier = Modifier.weight(1f),
                isCurrentSong = isCurrentSong,
                isActivelyPlaying = isActivelyPlaying,
                showPlayButton = false
            )
        }
    }
}
