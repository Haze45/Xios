package com.yourapp.music.presentation.favorites

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import com.yourapp.music.R
import com.yourapp.music.domain.model.Song
import com.yourapp.music.presentation.components.SongListItem
import com.yourapp.music.presentation.player.MiniPlayer
import com.yourapp.music.presentation.player.PlayerViewModel
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
@Composable
fun FavoritesScreen(
    onBackClick: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onExpandPlayer: () -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val playerState by playerViewModel.uiState.collectAsState()

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
                        contentDescription = stringResource(R.string.back)
                    )
                }
                Text(
                    stringResource(R.string.liked_songs),
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Spacer(Modifier.height(12.dp))

            if (state.songs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.no_liked_songs),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn {
                    items(
                        state.songs,
                        key = { it.id }) { song ->
                        SwipeToRemoveFavorite(
                            song = song,
                            onRemove = {
                                viewModel.removeFavorite(
                                    song.id
                                )
                            }
                        ) {
                            SongListItem(
                                song = song,
                                onClick = {
                                    onSongClick(
                                        song,
                                        state.songs
                                    )
                                },
                                onPlayClick = {
                                    if (playerState.currentSong?.id == song.id) {
                                        playerViewModel.togglePlayPause()
                                    } else {
                                        playerViewModel.playSong(
                                            song,
                                            state.songs
                                        )
                                    }
                                },
                                isCurrentSong = playerState.currentSong?.id == song.id,
                                isActivelyPlaying =
                                    playerState.currentSong?.id == song.id &&
                                            playerState.isPlaying,
                                showPlayButton = false
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
private fun SwipeToRemoveFavorite(
    song: Song,
    onRemove: () -> Unit,
    content: @Composable () -> Unit
) {
    var dragOffset by remember(song.id) {
        mutableFloatStateOf(0f)
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
                imageVector = Icons.Filled.Delete,
                contentDescription = stringResource(R.string.remove_favorite),
                tint = MaterialTheme.colorScheme.onPrimary
            )

            Spacer(Modifier.width(6.dp))

            Text(
                text = stringResource(R.string.remove),
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset {
                    IntOffset(dragOffset.roundToInt(), 0)
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
                }
        ) {
            content()
        }
    }
}
