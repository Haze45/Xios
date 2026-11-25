package com.yourapp.music.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.yourapp.music.core.theme.ErrorRed
import com.yourapp.music.domain.model.AppLanguage
import com.yourapp.music.domain.model.AppTheme
import com.yourapp.music.domain.model.StreamingQuality

@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val settings = state.settings

    var showLogoutDialog by remember {
        mutableStateOf(false)
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = {
                showLogoutDialog = false
            },
            title = {
                Text("Log out?")
            },
            text = {
                Text("Are you sure you want to log out of your account?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                        onLogout()
                    }
                ) {
                    Text(
                        "Log out",
                        color = ErrorRed
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(20.dp))

        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium
        )

        SectionTitle("Playback")

        SelectorRow(
            icon = {
                Icon(
                    Icons.Default.HighQuality,
                    contentDescription = null
                )
            },
            title = "Streaming quality",
            subtitle = "Preferred audio quality",
            selected = settings.streamingQuality.label(),
            options = StreamingQuality.entries.map {
                it.label() to it
            },
            onSelect = viewModel::onStreamingQualityChange
        )

        SectionTitle("Notifications")

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = null
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    "Playback notification",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    "Show the currently playing song in notifications",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = settings.playbackNotificationsEnabled,
                onCheckedChange =
                    viewModel::onPlaybackNotificationsToggle,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        SectionTitle("Appearance")

        SelectorRow(
            icon = {
                Icon(
                    Icons.Default.Palette,
                    contentDescription = null
                )
            },
            title = "Theme",
            subtitle = "Choose your app appearance",
            selected = settings.theme.label(),
            options = AppTheme.entries.map {
                it.label() to it
            },
            onSelect = viewModel::onThemeChange
        )

        SectionTitle("Language")

        SelectorRow(
            icon = {
                Icon(
                    Icons.Default.Language,
                    contentDescription = null
                )
            },
            title = "App language",
            subtitle = "Choose your preferred language",
            selected = AppLanguage.supported
                .firstOrNull { it.code == settings.language }
                ?.displayName ?: "System default",
            options = AppLanguage.supported.map {
                it.displayName to it.code
            },
            onSelect = viewModel::onLanguageChange
        )

        Spacer(Modifier.height(28.dp))

        TextButton(
            onClick = {
                showLogoutDialog = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                tint = ErrorRed
            )

            Spacer(Modifier.width(8.dp))

            Text(
                "Log out",
                color = ErrorRed
            )
        }

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(
            top = 22.dp,
            bottom = 8.dp
        )
    )
}

@Composable
private fun <T> SelectorRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    selected: String,
    options: List<Pair<String, T>>,
    onSelect: (T) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box {
            TextButton(
                onClick = {
                    expanded = true
                }
            ) {
                Text(
                    selected,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                }
            ) {
                options.forEach { (label, value) ->
                    DropdownMenuItem(
                        text = {
                            Text(label)
                        },
                        onClick = {
                            onSelect(value)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

private fun StreamingQuality.label(): String =
    when (this) {
        StreamingQuality.LOW -> "Low"
        StreamingQuality.NORMAL -> "Normal"
        StreamingQuality.HIGH -> "High"
    }

private fun AppTheme.label(): String =
    when (this) {
        AppTheme.SYSTEM -> "System"
        AppTheme.LIGHT -> "Light"
        AppTheme.DARK -> "Dark"
    }
