package com.yourapp.music.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.statusBarsPadding
import coil.compose.AsyncImage
import com.yourapp.music.core.theme.SurfaceElevated
import com.yourapp.music.core.theme.TextSecondary
import com.yourapp.music.domain.model.Song
import com.yourapp.music.presentation.components.AlbumCard
import com.yourapp.music.presentation.components.FeaturedPlaylistCard
import com.yourapp.music.presentation.components.FilterPill
import com.yourapp.music.presentation.components.ShimmerAlbumCard
import com.yourapp.music.presentation.components.ShimmerSongCard

@Composable
fun HomeScreen(
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onAlbumClick: (String) -> Unit,
    currentlyPlayingSongId: String?,
    isPlaying: Boolean,
    onSongPlay: (Song, List<Song>) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            // Keep content scrollable above the floating bottom controls.
            .padding(bottom = 160.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        // Top bar
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

        // Home filters
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(HomeFilter.entries.toList()) { filter ->
                FilterPill(
                    label = filter.label(),
                    selected = state.selectedFilter == filter,
                    onClick = {
                        viewModel.onFilterSelected(filter)
                    }
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Picked for You
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Picked for You",
                style = MaterialTheme.typography.titleLarge
            )

            TextButton(
                onClick = viewModel::refreshPersonalizedRecommendations,
                enabled = !state.isRecommendationsLoading
            ) {
                Text("Refresh", color = TextSecondary)
            }
        }

        Spacer(Modifier.height(12.dp))

        when {
            state.isRecommendationsLoading -> {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(
                        12.dp
                    ),
                    userScrollEnabled = false
                ) {
                    items(3) {
                        ShimmerSongCard()
                    }
                }
            }

            state.recommendedSongs.isNotEmpty() -> {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(
                        12.dp
                    )
                ) {
                    items(
                        items = state.recommendedSongs,
                        key = { it.id }
                    ) { song ->
                        FeaturedPlaylistCard(
                            title = song.title,
                            description = "By ${song.artist}",
                            imageUrl = song.albumArtUrl,
                            isPlaying = currentlyPlayingSongId == song.id &&
                                    isPlaying,
                            onClick = {
                                onSongPlay(
                                    song,
                                    state.recommendedSongs
                                )
                            }
                        )
                    }
                }
            }

            state.recommendationMessage != null -> {
                Text(
                    text = state.recommendationMessage!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Recently Played
        if (state.recentlyPlayedSongs.isNotEmpty()) {
            Text(
                "Recently Played",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(
                    12.dp
                )
            ) {
                items(
                    items = state.recentlyPlayedSongs,
                    key = { it.id }
                ) { song ->
                    FeaturedPlaylistCard(
                        title = song.title,
                        description = "By ${song.artist}",
                        imageUrl = song.albumArtUrl,
                        isPlaying = currentlyPlayingSongId == song.id &&
                                isPlaying,
                        onClick = {
                            onSongPlay(
                                song,
                                state.recentlyPlayedSongs
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }

        // Albums
        Text(
            "Albums",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(12.dp))

        when {
            state.isLoading -> {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(
                        12.dp
                    ),
                    userScrollEnabled = false
                ) {
                    items(3) {
                        ShimmerAlbumCard()
                    }
                }
            }

            state.albums.isEmpty() -> {
                Text(
                    "No albums yet — songs need an album tag to show up here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            else -> {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(
                        12.dp
                    )
                ) {
                    items(
                        items = state.albums,
                        key = { it.id }
                    ) { album ->
                        AlbumCard(
                            album = album,
                            onClick = {
                                onAlbumClick(album.id)
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        state.errorMessage?.let { message ->
            Text(
                message,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

private fun HomeFilter.label(): String = when (this) {
    HomeFilter.ALL -> "All"
    HomeFilter.NEW_RELEASE -> "New Release"
}
