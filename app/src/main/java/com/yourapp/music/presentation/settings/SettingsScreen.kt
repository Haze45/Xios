package com.yourapp.music.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourapp.music.core.theme.AccentGreen
import com.yourapp.music.core.theme.BackgroundBlack
import com.yourapp.music.core.theme.ErrorRed
import com.yourapp.music.core.theme.TextSecondary
import com.yourapp.music.domain.model.StreamingQuality
import com.yourapp.music.presentation.components.SettingsNavItem
import com.yourapp.music.presentation.components.SettingsSectionHeader
import com.yourapp.music.presentation.components.SettingsToggleItem

@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val settings = state.settings

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBlack)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {

            SettingsNavItem(icon = Icons.Filled.Tune, label = "General", onClick = { })
            SettingsToggleItem(
                icon = Icons.Filled.Notifications,
                label = "Notifications",
                checked = settings.generalNotificationsEnabled,
                onCheckedChange = viewModel::onGeneralNotificationsToggle
            )
            SettingsNavItem(icon = Icons.Filled.MusicNote, label = "Tracks", onClick = { })

            SettingsSectionHeader("Playbacks")

            QualityDropdownRow(
                selected = settings.streamingQuality,
                onSelect = viewModel::onStreamingQualityChange
            )

            SettingsToggleItem(
                icon = Icons.Filled.VolumeUp,
                label = "Volume normalization",
                checked = settings.volumeNormalizationEnabled,
                onCheckedChange = viewModel::onVolumeNormalizationToggle
            )

            SettingsSectionHeader("Notifications")

            SettingsToggleItem(
                icon = Icons.Filled.NotificationsActive,
                label = "Playback notifications",
                checked = settings.playbackNotificationsEnabled,
                onCheckedChange = viewModel::onPlaybackNotificationsToggle
            )
            SettingsToggleItem(
                icon = Icons.Filled.NotificationsNone,
                label = "Notifications",
                subtitle = "Granular options and controls",
                checked = settings.granularNotificationsEnabled,
                onCheckedChange = viewModel::onGranularNotificationsToggle
            )

            Spacer(Modifier.height(24.dp))

            TextButton(
                onClick = { viewModel.logout(); onLogout() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Log out", tint = ErrorRed)
                Spacer(Modifier.width(8.dp))
                Text("Log Out", color = ErrorRed)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun QualityDropdownRow(
    selected: StreamingQuality,
    onSelect: (StreamingQuality) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.HighQuality, contentDescription = "Quality", tint = TextSecondary)
            Spacer(Modifier.width(16.dp))
            Text("Quality setting", style = MaterialTheme.typography.bodyLarge)
        }

        Box {
            TextButton(onClick = { expanded = true }) {
                Text(selected.label(), color = AccentGreen)
                Icon(Icons.Filled.ArrowDropDown, contentDescription = "Choose quality", tint = AccentGreen)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                StreamingQuality.entries.forEach { quality ->
                    DropdownMenuItem(
                        text = { Text(quality.label()) },
                        onClick = { onSelect(quality); expanded = false }
                    )
                }
            }
        }
    }
}

private fun StreamingQuality.label(): String = when (this) {
    StreamingQuality.LOW -> "Low"
    StreamingQuality.NORMAL -> "Normal"
    StreamingQuality.HIGH -> "High"
}
