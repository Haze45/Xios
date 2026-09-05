package com.yourapp.music.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.yourapp.music.core.theme.AccentGreen
import com.yourapp.music.core.theme.TextSecondary
import com.yourapp.music.domain.model.Song

@Composable
fun SongListItem(
    song: Song,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** True when this song is the one currently loaded in the player (playing OR paused) — colors the title green. */
    isCurrentSong: Boolean = false,
    /** True only when this song is both current AND actually playing right now — swaps the icon to Pause. */
    isActivelyPlaying: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.albumArtUrl,
            contentDescription = song.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                song.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isCurrentSong) AccentGreen else MaterialTheme.typography.titleMedium.color,
                maxLines = 1
            )
            Text("By ${song.artist}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, maxLines = 1)
        }
        IconButton(onClick = onPlayClick) {
            Icon(
                if (isActivelyPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isActivelyPlaying) "Pause" else "Play"
            )
        }
    }
}
