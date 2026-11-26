package com.yourapp.music.presentation.queue

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.yourapp.music.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import com.yourapp.music.domain.model.Song

@UnstableApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueBottomSheet(
    onDismiss: () -> Unit,
    viewModel: QueueViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 20.dp,
                        vertical = 8.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = stringResource(R.string.playing_queue),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(Modifier.height(8.dp))

            if (state.queue.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.queue_is_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .padding(
                            horizontal = 12.dp,
                            vertical = 8.dp
                        )
                ) {
                    itemsIndexed(
                        items = state.queue,
                        key = { index, song -> "$index-${song.id}" }
                    ) { index, song ->

                        val isCurrent =
                            index == state.currentIndex
                        val isActivelyPlaying =
                            isCurrent && state.isPlaying

                        QueueRow(
                            song = song,
                            isCurrent = isCurrent,
                            isActivelyPlaying = isActivelyPlaying,
                            onClick = {
                                viewModel.playAt(index)
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun QueueRow(
    song: Song,
    isCurrent: Boolean,
    isActivelyPlaying: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isCurrent) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.albumArtUrl,
            contentDescription = stringResource(R.string.queue_song_artwork, song.title),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
        )

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isCurrent) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Unspecified
                },
                maxLines = 1
            )

            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        // The animated bars appear only while this queue item is playing.
        if (isActivelyPlaying) {
            Spacer(Modifier.width(12.dp))
            PlayingBars()
        }
    }
}

@Composable
private fun PlayingBars(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(
        label = "playing_bars"
    )

    val bar1 by transition.animateFloat(
        initialValue = 6f,
        targetValue = 21f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 430,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "playing_bar_1"
    )

    val bar2 by transition.animateFloat(
        initialValue = 18f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 520,
                easing = LinearEasing,
                delayMillis = 100
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "playing_bar_2"
    )

    val bar3 by transition.animateFloat(
        initialValue = 8f,
        targetValue = 23f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 470,
                easing = LinearEasing,
                delayMillis = 180
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "playing_bar_3"
    )

    Row(
        modifier = modifier
            .width(24.dp)
            .height(28.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlayingBar(height = bar1)
        PlayingBar(height = bar2)
        PlayingBar(height = bar3)
    }
}

@Composable
private fun PlayingBar(
    height: Float
) {
    Box(
        modifier = Modifier
            .width(4.dp)
            .height(height.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.primary)
    )
}