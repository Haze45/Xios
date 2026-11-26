package com.yourapp.music.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.yourapp.music.R
import com.yourapp.music.domain.model.Playlist
import com.yourapp.music.presentation.components.ArtistAvatarItem
import com.yourapp.music.presentation.components.PlaylistGridItem
import com.yourapp.music.presentation.components.ShimmerStatsRow

@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showEditSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
    ) {
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
                stringResource(R.string.my_profile),
                style = MaterialTheme.typography.titleLarge
            )
            IconButton(onClick = onSettingsClick) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.settings_title)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Avatar + name, centered. Tapping the photo (or its green edit
        // badge) opens the Edit Profile sheet to change photo/name.
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                AsyncImage(
                    model = state.photoUrl,
                    contentDescription = state.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .clickable { showEditSheet = true }
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .border(
                            2.dp,
                            MaterialTheme.colorScheme.background,
                            CircleShape
                        )
                        .clickable { showEditSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = stringResource(
                            R.string.edit_profile
                        ),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                state.displayName,
                style = MaterialTheme.typography.headlineMedium
            )
        }

        Spacer(Modifier.height(20.dp))

        // Stats row: Following / Followers / Playlists
        if (state.isLoading) {
            ShimmerStatsRow()
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = state.favoriteArtists.size.toString(),
                    label = stringResource(R.string.following)
                )

                StatItem(
                    value = formatCount(state.stats.followersCount),
                    label = stringResource(R.string.followers)
                )

                StatItem(
                    value = state.playlists.size.toString(),
                    label = stringResource(R.string.playlists)
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                stringResource(R.string.my_playlists),
                style = MaterialTheme.typography.titleLarge
            )
            TextButton(onClick = { /* navigate to full playlists list */ }) {
                Text(
                    stringResource(R.string.see_all),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(
                12.dp
            )
        ) {
            items(
                state.playlists,
                key = { it.id }) { playlist ->
                PlaylistGridItem(
                    playlist = playlist,
                    onClick = { onPlaylistClick(playlist) })
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            stringResource(R.string.my_favorite_artists),
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(
                16.dp
            )
        ) {
            items(
                state.favoriteArtists,
                key = { it.id }) { artist ->
                ArtistAvatarItem(
                    artist = artist,
                    onFollowToggle = {
                        viewModel.toggleFollow(
                            artist.id,
                            artist.isFollowing
                        )
                    }
                )
            }
        }

        state.errorMessage?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.height(24.dp))
    }

    if (showEditSheet) {
        EditProfileSheet(
            onDismiss = { showEditSheet = false },
            viewModel = viewModel
        )
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Renders large counts like Instagram-style "1.2k" instead of "1200". */
private fun formatCount(count: Int): String = when {
    count >= 1000 -> "%.1fk".format(count / 1000.0)
    else -> count.toString()
}
