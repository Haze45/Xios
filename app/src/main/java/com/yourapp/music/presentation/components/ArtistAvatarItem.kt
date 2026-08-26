package com.yourapp.music.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.yourapp.music.core.theme.AccentGreen
import com.yourapp.music.core.theme.BackgroundBlack
import com.yourapp.music.core.theme.SurfaceElevated
import com.yourapp.music.core.theme.TextSecondary
import com.yourapp.music.domain.model.Artist

/** Vertical avatar + name + Follow/Following pill, used in Profile's "My Favorite Artists" row. */
@Composable
fun ArtistAvatarItem(
    artist: Artist,
    onFollowToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.width(72.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = artist.avatarUrl,
            contentDescription = artist.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            artist.name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
        Spacer(Modifier.height(6.dp))
        Surface(
            onClick = onFollowToggle,
            shape = RoundedCornerShape(50),
            color = if (artist.isFollowing) SurfaceElevated else AccentGreen,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
        ) {
            Text(
                text = if (artist.isFollowing) "Following" else "Follow",
                style = MaterialTheme.typography.labelSmall,
                color = if (artist.isFollowing) TextSecondary else BackgroundBlack,
                modifier = Modifier
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}
