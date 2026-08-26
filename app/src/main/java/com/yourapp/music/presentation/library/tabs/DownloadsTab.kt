package com.yourapp.music.presentation.library.tabs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.yourapp.music.core.theme.TextSecondary

/** Placeholder until offline download/caching (Media3 + Room blob refs) is built. */
@Composable
fun DownloadsTab(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            "Downloads coming soon.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}
