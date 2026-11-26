package com.yourapp.music.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.yourapp.music.R
import com.yourapp.music.domain.model.Song

@Composable
fun SongListItem(
    song: Song,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCurrentSong: Boolean = false,
    isActivelyPlaying: Boolean = false,
    showPlayButton: Boolean = true
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
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isCurrentSong) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.typography.titleMedium.color
                },
                maxLines = 1
            )

            Text(
                text = stringResource(
                    R.string.by_artist,
                    song.artist
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        when {
            isActivelyPlaying -> {
                PlayingBars()
            }

            showPlayButton -> {
                IconButton(onClick = onPlayClick) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = stringResource(
                            R.string.play
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayingBars() {
    val transition =
        rememberInfiniteTransition(label = "song_playing_bars")

    val height1 by transition.animateFloat(
        initialValue = 6f,
        targetValue = 21f,
        animationSpec = infiniteRepeatable(
            animation = tween(430, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar_one"
    )

    val height2 by transition.animateFloat(
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
        label = "bar_two"
    )

    val height3 by transition.animateFloat(
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
        label = "bar_three"
    )

    Row(
        modifier = Modifier
            .width(24.dp)
            .height(28.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlayingBar(height1)
        PlayingBar(height2)
        PlayingBar(height3)
    }
}

@Composable
private fun PlayingBar(height: Float) {
    Box(
        modifier = Modifier
            .width(4.dp)
            .height(height.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.primary)
    )
}
