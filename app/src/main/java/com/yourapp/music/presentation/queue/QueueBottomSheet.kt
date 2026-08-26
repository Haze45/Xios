package com.yourapp.music.presentation.queue

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.QueueMusic
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import com.yourapp.music.core.theme.AccentGreen
import com.yourapp.music.core.theme.CardDark
import com.yourapp.music.core.theme.ErrorRed
import com.yourapp.music.core.theme.SurfaceDark
import com.yourapp.music.core.theme.TextSecondary
import com.yourapp.music.domain.model.Song

/**
 * Modal bottom sheet showing the current playback queue.
 * - Tap a song → jumps playback to it
 * - Swipe a song away → removes it from the queue
 * - The currently playing song is highlighted with an equalizer glyph
 */
@UnstableApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueBottomSheet(
    onDismiss: () -> Unit,
    viewModel: QueueViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceDark
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.QueueMusic, contentDescription = null, tint = TextSecondary)
                Spacer(Modifier.width(8.dp))
                Text("Playing Queue", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(8.dp))

            if (state.queue.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Queue is empty", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    itemsIndexed(
                        items = state.queue,
                        key = { index, song -> "$index-${song.id}" }
                    ) { index, song ->
                        QueueRow(
                            song = song,
                            isCurrent = index == state.currentIndex,
                            isPlaying = state.isPlaying,
                            onClick = { viewModel.playAt(index) },
                            onRemove = { viewModel.removeAt(index) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueRow(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            val dismissed = value == SwipeToDismissBoxValue.EndToStart ||
                value == SwipeToDismissBoxValue.StartToEnd
            if (dismissed) onRemove()
            dismissed
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ErrorRed.copy(alpha = 0.85f))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = Color.White)
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isCurrent) CardDark else Color.Transparent)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = song.albumArtUrl,
                    contentDescription = song.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                if (isCurrent && isPlaying) {
                    Icon(
                        Icons.Filled.Equalizer,
                        contentDescription = "Now playing",
                        tint = AccentGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isCurrent) AccentGreen else Color.Unspecified,
                    maxLines = 1
                )
                Text(
                    song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}
