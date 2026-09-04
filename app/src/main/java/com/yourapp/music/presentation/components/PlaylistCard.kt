package com.yourapp.music.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.yourapp.music.core.theme.BackgroundBlack
import com.yourapp.music.core.theme.CardDark
import com.yourapp.music.core.theme.TextSecondary
import com.yourapp.music.domain.model.Playlist

/** Small square cover used in Profile's "My Playlists" grid. */
@Composable
fun PlaylistGridItem(
    playlist: Playlist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.width(90.dp)) {
        AsyncImage(
            model = playlist.coverUrl,
            contentDescription = playlist.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(90.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onClick)
        )
        Spacer(Modifier.height(6.dp))
        Text(playlist.title, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

/** Small row-style playlist item, used in "Top daily playlists" list on Home. */
@Composable
fun PlaylistRowItem(
    playlist: Playlist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onMenuClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = playlist.coverUrl,
            contentDescription = playlist.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(playlist.title, style = MaterialTheme.typography.titleMedium)
            Text(
                "By ${playlist.ownerName} • ${playlist.songCount} Songs",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
        // Only rendered where the caller actually wants a menu available
        // right in the list (e.g. the Playlists tab) — Home/Library keep
        // showing this row without it, unaffected.
        if (onMenuClick != null) {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Playlist options")
            }
        }
    }
}

/** Wide featured card, used for "Discover weekly" style banners on Home. */
@Composable
fun FeaturedPlaylistCard(
    title: String,
    description: String,
    imageUrl: String,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(260.dp)
            .height(150.dp)
            .clip(RoundedCornerShape(20.dp))
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(description, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
            }
            IconButton(
                onClick = onPlayClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(50))
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = BackgroundBlack)
            }
        }
    }
}
