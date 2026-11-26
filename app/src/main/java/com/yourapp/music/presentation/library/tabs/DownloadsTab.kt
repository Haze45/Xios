package com.yourapp.music.presentation.library.tabs

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import com.yourapp.music.R
import com.yourapp.music.core.theme.TextSecondary
import com.yourapp.music.domain.model.DownloadProgress
import com.yourapp.music.domain.model.Song
import com.yourapp.music.presentation.components.SongListItem
import com.yourapp.music.presentation.player.PlayerViewModel
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
@Composable
fun DownloadsTab(
    songs: List<Song>,
    downloadingSongs: List<DownloadProgress>,
    onSongClick: (Song, List<Song>) -> Unit,
    onPauseDownload: (String) -> Unit,
    onResumeDownload: (String) -> Unit,
    onCancelDownload: (String) -> Unit,
    onDeleteDownloadedSong: (String) -> Unit,
    modifier: Modifier = Modifier,
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val playerState by playerViewModel.uiState.collectAsState()

    var songPendingDeletion by remember {
        mutableStateOf<Song?>(null)
    }

    if (songPendingDeletion != null) {
        val song = songPendingDeletion!!

        AlertDialog(
            onDismissRequest = {
                songPendingDeletion = null
            },
            title = {
                Text(stringResource(R.string.delete_downloaded_song_title))
            },
            text = {
                Text(
                    stringResource(R.string.delete_downloaded_song_message, song.title)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteDownloadedSong(song.id)
                        songPendingDeletion = null
                    }
                ) {
                    Text(
                        stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        songPendingDeletion = null
                    }
                ) {
                    Text(stringResource(R.string.keep_song))
                }
            }
        )
    }

    if (songs.isEmpty() && downloadingSongs.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                stringResource(R.string.no_downloads_yet),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (downloadingSongs.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.downloads),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(
                        top = 8.dp,
                        bottom = 4.dp
                    )
                )
            }

            items(
                items = downloadingSongs,
                key = { "downloading-${it.song.id}" }
            ) { progress ->
                DownloadProgressRow(
                    progress = progress,
                    onPause = {
                        onPauseDownload(progress.song.id)
                    },
                    onResume = {
                        onResumeDownload(progress.song.id)
                    },
                    onCancel = {
                        onCancelDownload(progress.song.id)
                    }
                )
            }
        }

        if (songs.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.downloaded),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(
                        top = 12.dp,
                        bottom = 4.dp
                    )
                )
            }

            items(
                items = songs,
                key = { it.id }
            ) { song ->

                var dragOffset by remember(song.id) {
                    mutableFloatStateOf(0f)
                }

                val density = LocalDensity.current
                val revealWidth = with(density) {
                    112.dp.toPx()
                }

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
                            contentDescription = stringResource(R.string.delete_downloaded_song),
                            tint = Color(0xFF191B13)
                        )

                        Spacer(Modifier.width(6.dp))

                        Text(
                            text = stringResource(R.string.remove),
                            color = Color(0xFF191B13),
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
                            .background(MaterialTheme.colorScheme.surface)
                            .pointerInput(song.id) {
                                detectHorizontalDragGestures(
                                    onHorizontalDrag = { change, amount ->
                                        change.consume()

                                        dragOffset = (
                                                dragOffset + amount
                                                ).coerceIn(
                                                0f,
                                                revealWidth
                                            )
                                    },
                                    onDragEnd = {
                                        if (
                                            dragOffset >= revealWidth * 0.65f
                                        ) {
                                            songPendingDeletion =
                                                song
                                        }

                                        dragOffset = 0f
                                    },
                                    onDragCancel = {
                                        dragOffset = 0f
                                    }
                                )
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SongListItem(
                            song = song,
                            onClick = {
                                onSongClick(song, songs)
                            },
                            onPlayClick = {
                                if (
                                    playerState.currentSong?.id == song.id
                                ) {
                                    playerViewModel.togglePlayPause()
                                } else {
                                    playerViewModel.playSong(
                                        song,
                                        songs
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            isCurrentSong =
                                playerState.currentSong?.id == song.id,
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
}

@Composable
private fun DownloadProgressRow(
    progress: DownloadProgress,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    val fraction = progress.fraction

    val statusText = when {
        progress.isPaused -> stringResource(R.string.paused)
        progress.isFailed -> stringResource(R.string.download_failed)
        else -> stringResource(R.string.downloading)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = progress.song.albumArtUrl,
                contentDescription = progress.song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = progress.song.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1
                )

                Text(
                    text = progress.song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = progress.percentage?.let {
                            "$it%"
                        } ?: formatDownloadedBytes(
                            progress.downloadedBytes
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (fraction == null) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        8.dp
                    )
                ) {
                    Button(
                        onClick = {
                            if (progress.isPaused || progress.isFailed) {
                                onResume()
                            } else {
                                onPause()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            when {
                                progress.isPaused -> stringResource(R.string.resume)
                                progress.isFailed -> stringResource(R.string.retry)
                                else -> stringResource(R.string.pause)
                            }
                        )
                    }

                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        }
    }
}

private fun formatDownloadedBytes(bytes: Long): String {
    val megabytes = bytes / (1024f * 1024f)

    return if (megabytes >= 1f) {
        String.format(Locale.US, "%.1f MB", megabytes)
    } else {
        "${bytes / 1024} KB"
    }
}
