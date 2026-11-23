package com.yourapp.music.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ShimmerBase = Color(0xFF242424)
private val ShimmerHighlight = Color(0xFF454545)

/**
 * Reusable animated shimmer block.
 */
@Composable
fun ShimmerBlock(
    modifier: Modifier = Modifier,
    circular: Boolean = false
) {
    val transition = rememberInfiniteTransition(
        label = "shimmer_transition"
    )

    val translateX by transition.animateFloat(
        initialValue = -400f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            ShimmerBase,
            ShimmerHighlight,
            ShimmerBase
        ),
        start = Offset(translateX, 0f),
        end = Offset(translateX + 300f, 300f)
    )

    val shape = if (circular) {
        CircleShape
    } else {
        RoundedCornerShape(10.dp)
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(ShimmerBase)
            .drawWithContent {
                drawContent()
                drawRect(brush)
            }
    )
}

/**
 * Home featured song card placeholder.
 */
@Composable
fun ShimmerSongCard(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.width(260.dp)
    ) {
        ShimmerBlock(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        )

        Spacer(Modifier.height(10.dp))

        ShimmerBlock(
            modifier = Modifier
                .width(170.dp)
                .height(14.dp)
        )

        Spacer(Modifier.height(7.dp))

        ShimmerBlock(
            modifier = Modifier
                .width(110.dp)
                .height(11.dp)
        )
    }
}

/**
 * Album card placeholder.
 */
@Composable
fun ShimmerAlbumCard(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.width(140.dp)
    ) {
        ShimmerBlock(
            modifier = Modifier.size(140.dp)
        )

        Spacer(Modifier.height(8.dp))

        ShimmerBlock(
            modifier = Modifier
                .width(115.dp)
                .height(13.dp)
        )

        Spacer(Modifier.height(6.dp))

        ShimmerBlock(
            modifier = Modifier
                .width(90.dp)
                .height(10.dp)
        )
    }
}

/**
 * Song list row placeholder.
 */
@Composable
fun ShimmerSongRow(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShimmerBlock(
            modifier = Modifier.size(56.dp)
        )

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            ShimmerBlock(
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .height(14.dp)
            )

            Spacer(Modifier.height(8.dp))

            ShimmerBlock(
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .height(11.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        ShimmerBlock(
            modifier = Modifier.size(28.dp),
            circular = true
        )
    }
}

/**
 * Multiple song rows.
 */
@Composable
fun ShimmerSongList(
    modifier: Modifier = Modifier,
    count: Int = 6
) {
    Column(modifier = modifier) {
        repeat(count) {
            ShimmerSongRow()
        }
    }
}

/**
 * Playlist row placeholder.
 */
@Composable
fun ShimmerPlaylistRow(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShimmerBlock(
            modifier = Modifier.size(64.dp)
        )

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            ShimmerBlock(
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .height(14.dp)
            )

            Spacer(Modifier.height(8.dp))

            ShimmerBlock(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(11.dp)
            )
        }

        ShimmerBlock(
            modifier = Modifier.size(24.dp),
            circular = true
        )
    }
}

/**
 * Multiple playlist rows.
 */
@Composable
fun ShimmerPlaylistList(
    modifier: Modifier = Modifier,
    count: Int = 5
) {
    Column(modifier = modifier) {
        repeat(count) {
            ShimmerPlaylistRow()
        }
    }
}

/**
 * Album/playlist detail screen placeholder.
 */
@Composable
fun ShimmerDetailScreen(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShimmerBlock(
                modifier = Modifier.size(100.dp)
            )

            Spacer(Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                ShimmerBlock(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(20.dp)
                )

                Spacer(Modifier.height(10.dp))

                ShimmerBlock(
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .height(12.dp)
                )

                Spacer(Modifier.height(8.dp))

                ShimmerBlock(
                    modifier = Modifier
                        .fillMaxWidth(0.45f)
                        .height(12.dp)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        ShimmerBlock(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        )

        Spacer(Modifier.height(16.dp))

        ShimmerSongList(count = 5)
    }
}

/**
 * Profile statistics placeholder.
 */
@Composable
fun ShimmerStatsRow(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        repeat(3) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ShimmerBlock(
                    modifier = Modifier
                        .width(42.dp)
                        .height(20.dp)
                )

                Spacer(Modifier.height(8.dp))

                ShimmerBlock(
                    modifier = Modifier
                        .width(65.dp)
                        .height(11.dp)
                )
            }
        }
    }
}
