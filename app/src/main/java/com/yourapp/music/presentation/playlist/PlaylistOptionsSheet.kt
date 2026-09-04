package com.yourapp.music.presentation.playlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.yourapp.music.core.theme.ErrorRed
import com.yourapp.music.core.theme.SurfaceDark
import com.yourapp.music.core.theme.TextSecondary
import com.yourapp.music.domain.model.Playlist

/**
 * Bottom sheet opened from a playlist's kebab (⋮) menu — shows the
 * playlist's cover art + name up top, then Rename and Delete actions.
 * Each action just closes this sheet and hands off to the caller; the
 * actual rename/delete confirmation UI lives in PlaylistScreen so this
 * sheet stays a simple menu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistOptionsSheet(
    playlist: Playlist,
    onDismiss: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceDark
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = playlist.coverUrl,
                    contentDescription = playlist.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(playlist.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Text(
                        "${playlist.songCount} Songs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = TextSecondary.copy(alpha = 0.2f))
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onRenameClick)
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null, tint = TextSecondary)
                Spacer(Modifier.width(16.dp))
                Text("Rename", style = MaterialTheme.typography.bodyLarge)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onDeleteClick)
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null, tint = ErrorRed)
                Spacer(Modifier.width(16.dp))
                Text("Delete", style = MaterialTheme.typography.bodyLarge, color = ErrorRed)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
