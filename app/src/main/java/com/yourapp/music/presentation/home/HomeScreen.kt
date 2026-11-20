package com.yourapp.music.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.yourapp.music.core.theme.BackgroundBlack
import com.yourapp.music.core.theme.SurfaceElevated
import com.yourapp.music.core.theme.TextSecondary
import com.yourapp.music.domain.model.Song
import com.yourapp.music.presentation.components.AlbumCard
import com.yourapp.music.presentation.components.FeaturedPlaylistCard
import com.yourapp.music.presentation.components.FilterPill

@Composable
fun HomeScreen(
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onAlbumClick: (String) -> Unit,
    onSongPlay: (Song, List<Song>) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBlack)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        // Top bar: profile avatar (tap -> Profile screen) + search/favorites icons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state.photoUrl != null) {
                AsyncImage(
                    model = state.photoUrl,
                    contentDescription = "My Profile",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onProfileClick)
                )
            } else {
                // No profile photo set yet — still tappable, shows a plain circle placeholder.
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SurfaceElevated)
                        .clickable(onClick = onProfileClick)
                )
            }

            Row {
                IconButton(onClick = onSearchClick) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = "Search"
                    )
                }
                IconButton(onClick = onFavoritesClick) {
                    Icon(
                        Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorites"
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            "Hi, ${state.userName}",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(16.dp))

        // Filter chips: All / New Release / Trending / Top
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(
                8.dp
            )
        ) {
            items(HomeFilter.entries.toList()) { filter ->
                FilterPill(
                    label = filter.label(),
                    selected = state.selectedFilter == filter,
                    onClick = {
                        viewModel.onFilterSelected(
                            filter
                        )
                    }
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Text(
                "Curated & trending",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(
                    12.dp
                )
            ) {
                items(state.songs.take(5)) { song ->
                    FeaturedPlaylistCard(
                        title = song.title,
                        description = "By ${song.artist}",
                        imageUrl = song.albumArtUrl,
                        onPlayClick = {
                            onSongPlay(
                                song,
                                state.songs.take(5)
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Albums",
                    style = MaterialTheme.typography.titleLarge
                )
                TextButton(onClick = { /* navigate to full list */ }) {
                    Text("See all", color = TextSecondary)
                }
            }

            Spacer(Modifier.height(12.dp))

            if (state.albums.isEmpty()) {
                Text(
                    "No albums yet — songs need an album tag to show up here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(
                        12.dp
                    )
                ) {
                    items(
                        state.albums,
                        key = { it.id }) { album ->
                        AlbumCard(
                            album = album,
                            onClick = { onAlbumClick(album.id) })
                    }
                }
            }
        }

        state.errorMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

private fun HomeFilter.label(): String = when (this) {
    HomeFilter.ALL -> "All"
    HomeFilter.NEW_RELEASE -> "New Release"
}
