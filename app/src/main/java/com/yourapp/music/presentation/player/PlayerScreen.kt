package com.yourapp.music.presentation.player

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import com.yourapp.music.core.theme.AccentGreen
import com.yourapp.music.core.theme.BackgroundBlack
import com.yourapp.music.core.theme.SurfaceElevated
import com.yourapp.music.core.theme.TextSecondary
import com.yourapp.music.presentation.playlist.AddToPlaylistSheet
import com.yourapp.music.presentation.queue.QueueBottomSheet

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    onBackClick: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val song = state.currentSong
    var showQueueSheet by remember { mutableStateOf(false) }
    var showAddToPlaylistSheet by remember {
        mutableStateOf(
            false
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBlack)
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Text(
                "Now Playing",
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = viewModel::toggleFavorite) {
                Icon(
                    if (state.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (state.isFavorite) AccentGreen else TextSecondary
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = song?.albumArtUrl,
                contentDescription = song?.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(280.dp)
                    .clip(CircleShape)
            )
        }

        // Takes up all the leftover vertical space between the album art
        // and the seek bar/controls below, and centers the title/artist
        // inside it — so they sit in the middle of the screen instead of
        // being pinned directly under the artwork.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    song?.title.orEmpty(),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    song?.artist.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }

        // "Add to Playlist" sits just above the slider, right-aligned —
        // a quick action tied to the current song without crowding the
        // main transport controls below.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = { showAddToPlaylistSheet = true },
                enabled = song != null
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.PlaylistAdd,
                    contentDescription = "Add to Playlist",
                    tint = TextSecondary
                )
            }
        }

        // Thin track + small dot thumb, matching the reference design
        // (Material3's default Slider has a much thicker track and a
        // bigger "stadium" thumb, so both are swapped out here via the
        // track/thumb slot APIs available in this project's Material3
        // version — 1.3+-only Track params like thumbTrackGapSize /
        // trackInsideCornerSize / drawStopIndicator are intentionally
        // NOT used since this project's Compose BOM bundles Material3
        // ~1.2.1, which doesn't have them yet).
        Slider(
            value = state.positionMs.toFloat(),
            onValueChange = { viewModel.seekTo(it.toLong()) },
            valueRange = 0f..(state.durationMs.takeIf { it > 0 }
                ?.toFloat() ?: 1f),
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(4.dp),
                    colors = SliderDefaults.colors(
                        activeTrackColor = AccentGreen,
                        inactiveTrackColor = SurfaceElevated
                    )
                )
            },
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = remember { MutableInteractionSource() },
                    colors = SliderDefaults.colors(
                        thumbColor = AccentGreen
                    ),
                    thumbSize = DpSize(14.dp, 14.dp)
                )
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                formatMs(state.positionMs),
                style = MaterialTheme.typography.labelSmall
            )
            // Remaining time shown as a countdown (e.g. "-2:15"), matching the reference.
            Text(
                "-" + formatMs(
                    (state.durationMs - state.positionMs).coerceAtLeast(
                        0
                    )
                ),
                style = MaterialTheme.typography.labelSmall
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = viewModel::toggleShuffle) {
                Icon(
                    Icons.Filled.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (state.isShuffleEnabled) AccentGreen else TextSecondary
                )
            }
            IconButton(onClick = viewModel::skipToPrevious) {
                Icon(
                    Icons.Filled.SkipPrevious,
                    contentDescription = "Previous",
                    modifier = Modifier.size(36.dp)
                )
            }
            IconButton(
                onClick = viewModel::togglePlayPause,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(AccentGreen)
            ) {
                Icon(
                    if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = BackgroundBlack,
                    modifier = Modifier.size(32.dp)
                )
            }
            IconButton(onClick = viewModel::skipToNext) {
                Icon(
                    Icons.Filled.SkipNext,
                    contentDescription = "Next",
                    modifier = Modifier.size(36.dp)
                )
            }
            IconButton(onClick = {
                showQueueSheet = true
            }) {
                Icon(
                    Icons.Filled.QueueMusic,
                    contentDescription = "Queue"
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    if (showQueueSheet) {
        QueueBottomSheet(onDismiss = {
            showQueueSheet = false
        })
    }

    if (showAddToPlaylistSheet && song != null) {
        AddToPlaylistSheet(
            songId = song.id,
            onDismiss = { showAddToPlaylistSheet = false }
        )
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
