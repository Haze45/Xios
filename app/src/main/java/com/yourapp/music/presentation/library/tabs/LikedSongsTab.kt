package com.yourapp.music.presentation.library.tabs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.yourapp.music.core.theme.TextSecondary
import com.yourapp.music.domain.model.Song
import com.yourapp.music.presentation.components.SongListItem

@Composable
fun LikedSongsTab(
    songs: List<Song>,
    onSongClick: (Song, List<Song>) -> Unit,
    modifier: Modifier = Modifier
) {
    if (songs.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No liked songs yet — tap the heart on any song to save it here.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
        return
    }

    LazyColumn(modifier = modifier) {
        items(songs, key = { it.id }) { song ->
            SongListItem(
                song = song,
                onClick = { onSongClick(song, songs) },
                onPlayClick = { onSongClick(song, songs) }
            )
        }
    }
}
