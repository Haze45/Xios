package com.yourapp.music.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yourapp.music.core.theme.BackgroundBlack
import com.yourapp.music.domain.model.Genre

/** Colored genre tile, used in Search's "Browse by Genre" grid. */
@Composable
fun GenreTile(
    genre: Genre,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = runCatching { Color(android.graphics.Color.parseColor(genre.colorHex)) }
        .getOrDefault(BackgroundBlack)

    Box(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Text(
            genre.name,
            style = MaterialTheme.typography.titleMedium,
            color = BackgroundBlack
        )
    }
}
