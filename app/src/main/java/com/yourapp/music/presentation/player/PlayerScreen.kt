package com.yourapp.music.presentation.player

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import com.yourapp.music.R
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

    var showQueueSheet by remember {
        mutableStateOf(false)
    }

    var showAddToPlaylistSheet by remember {
        mutableStateOf(false)
    }

    // Local slider state prevents playback updates from
    // interfering with the user's dragging gesture.
    var isSeeking by remember {
        mutableStateOf(false)
    }

    var seekPosition by remember {
        mutableFloatStateOf(0f)
    }

    val context = LocalContext.current
    val storagePermissionMessage =
        stringResource(R.string.storage_permission_download)

    // Android 10+ writes to Downloads via MediaStore.
    // Android 9 and below needs WRITE_EXTERNAL_STORAGE.
    val storagePermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                viewModel.downloadCurrentSong()
            } else {
                Toast.makeText(
                    context,
                    storagePermissionMessage,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    val onDownloadClick = {
        val needsLegacyPermission =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

        val hasPermission =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED

        if (needsLegacyPermission && !hasPermission) {
            storagePermissionLauncher.launch(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        } else {
            viewModel.downloadCurrentSong()
        }
    }

    // Show download message once.
    LaunchedEffect(state.downloadMessage) {
        state.downloadMessage?.let {
            Toast.makeText(
                context,
                it,
                Toast.LENGTH_SHORT
            ).show()

            viewModel.consumeDownloadMessage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(horizontal = 24.dp)
    ) {

        Spacer(Modifier.height(16.dp))

        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }

            Text(
                stringResource(R.string.now_playing),
                style = MaterialTheme.typography.titleMedium
            )

            IconButton(onClick = viewModel::toggleFavorite) {
                Icon(
                    if (state.isFavorite) {
                        Icons.Filled.Favorite
                    } else {
                        Icons.Filled.FavoriteBorder
                    },
                    contentDescription = stringResource(R.string.favorite),
                    tint = if (state.isFavorite) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // Album artwork
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

        // Song title and artist
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    song?.title.orEmpty(),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    song?.artist.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Download and playlist actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {


            IconButton(
                onClick = onDownloadClick,
                enabled = song != null &&
                        !state.isDownloaded &&
                        !state.isDownloading
            ) {
                when {
                    state.isDownloading -> {

                        val progress =
                            state.downloadProgressFraction

                        if (progress != null) {
                            CircularProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.size(26.dp),
                                strokeWidth = 3.dp,
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = 0.25f
                                )
                            )
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier.size(26.dp),
                                strokeWidth = 3.dp,
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = 0.25f
                                )
                            )
                        }
                    }

                    state.isDownloaded -> {
                        Icon(
                            Icons.Filled.DownloadDone,
                            contentDescription = stringResource(
                                R.string.downloaded
                            ),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    else -> {
                        Icon(
                            Icons.Filled.Download,
                            contentDescription = stringResource(
                                R.string.download
                            ),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }


            IconButton(
                onClick = {
                    showAddToPlaylistSheet = true
                },
                enabled = song != null
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.PlaylistAdd,
                    contentDescription = stringResource(R.string.add_to_playlist),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ----------------------------------------
        // OPTIMIZED PLAYER SLIDER
        // ----------------------------------------

        val sliderDuration =
            state.durationMs.takeIf { it > 0 }?.toFloat()
                ?: 1f

        val displayedPosition = if (isSeeking) {
            seekPosition.coerceIn(0f, sliderDuration)
        } else {
            state.positionMs.toFloat()
                .coerceIn(0f, sliderDuration)
        }

        Slider(
            value = displayedPosition,

            onValueChange = { newPosition ->
                isSeeking = true
                seekPosition = newPosition
            },

            // Seek only after the user releases the slider.
            onValueChangeFinished = {
                viewModel.seekTo(seekPosition.toLong())
                isSeeking = false
            },

            valueRange = 0f..sliderDuration,

            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(4.dp),
                    colors = SliderDefaults.colors(
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            },

            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = remember {
                        MutableInteractionSource()
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary
                    ),
                    thumbSize = DpSize(14.dp, 14.dp)
                )
            }
        )

        // Playback timestamps
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Text(
                formatMs(
                    if (isSeeking) {
                        seekPosition.toLong()
                    } else {
                        state.positionMs
                    }
                ),
                style = MaterialTheme.typography.labelSmall
            )

            Text(
                "-" + formatMs(
                    (
                            state.durationMs -
                                    if (isSeeking) {
                                        seekPosition.toLong()
                                    } else {
                                        state.positionMs
                                    }
                            ).coerceAtLeast(0)
                ),
                style = MaterialTheme.typography.labelSmall
            )
        }

        Spacer(Modifier.height(16.dp))

        // Playback controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(onClick = viewModel::toggleShuffle) {
                Icon(
                    Icons.Filled.Shuffle,
                    contentDescription = stringResource(R.string.shuffle),
                    tint = if (state.isShuffleEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            IconButton(onClick = viewModel::skipToPrevious) {
                Icon(
                    Icons.Filled.SkipPrevious,
                    contentDescription = stringResource(R.string.previous),
                    modifier = Modifier.size(36.dp)
                )
            }

            IconButton(
                onClick = viewModel::togglePlayPause,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    if (state.isPlaying) {
                        Icons.Filled.Pause
                    } else {
                        Icons.Filled.PlayArrow
                    },
                    contentDescription = stringResource(R.string.play_pause),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            IconButton(onClick = viewModel::skipToNext) {
                Icon(
                    Icons.Filled.SkipNext,
                    contentDescription = stringResource(R.string.next),
                    modifier = Modifier.size(36.dp)
                )
            }

            IconButton(
                onClick = {
                    showQueueSheet = true
                }
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = stringResource(R.string.queue)
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    // Queue sheet
    if (showQueueSheet) {
        QueueBottomSheet(
            onDismiss = {
                showQueueSheet = false
            }
        )
    }

    // Add to playlist sheet
    if (showAddToPlaylistSheet && song != null) {
        AddToPlaylistSheet(
            songId = song.id,
            onDismiss = {
                showAddToPlaylistSheet = false
            }
        )
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return "%d:%02d".format(minutes, seconds)
}
