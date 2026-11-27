package com.yourapp.music.presentation.components

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import com.yourapp.music.R

@StringRes
fun appMessageResource(messageKey: String): Int =
    when (messageKey) {
        "album_not_found" -> R.string.album_not_found
        "playlist_not_found" -> R.string.playlist_not_found
        "profile_name_empty" -> R.string.profile_name_empty
        "profile_photo_read_failed" -> R.string.profile_photo_read_failed
        "search_semantic_fallback" -> R.string.search_semantic_fallback
        "search_failed" -> R.string.search_failed
        "download_started" -> R.string.download_started
        "download_failed" -> R.string.download_failed
        "delete_download_failed" -> R.string.delete_download_failed
        else -> R.string.error_generic
    }

@Composable
fun AppMessageText(
    messageKey: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.error,
    style: TextStyle = MaterialTheme.typography.bodyMedium
) {
    Text(
        text = stringResource(appMessageResource(messageKey)),
        modifier = modifier,
        color = color,
        style = style
    )
}
