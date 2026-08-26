package com.yourapp.music.presentation.library.tabs

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.yourapp.music.domain.model.Song
import com.yourapp.music.presentation.components.SongListItem

@Composable
fun AllTab(
    songs: List<Song>,
    onSongClick: (Song, List<Song>) -> Unit,
    modifier: Modifier = Modifier
) {
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
