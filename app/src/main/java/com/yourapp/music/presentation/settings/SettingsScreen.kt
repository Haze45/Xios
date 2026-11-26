package com.yourapp.music.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourapp.music.R
import com.yourapp.music.core.theme.ErrorRed
import com.yourapp.music.domain.model.AppLanguage
import com.yourapp.music.domain.model.AppTheme
import com.yourapp.music.domain.model.StreamingQuality
import com.yourapp.music.presentation.components.SettingsToggleItem

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

    val languageOptions =
        AppLanguage.supported.map { language ->
            val label = if (language.code == "system") {
                stringResource(R.string.language_system_default)
            } else {
                language.displayName
            }
            label to language.code
        }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = {
                showLogoutDialog = false
            },
            title = {
                Text(stringResource(R.string.logout_confirmation_title))
            },
            text = {
                Text(stringResource(R.string.logout_confirmation_message))
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
                        stringResource(R.string.logout),
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
                    Text(stringResource(R.string.cancel))
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
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium
        )

        SectionTitle(stringResource(R.string.playback_section))

        SelectorRow(
            icon = {
                Icon(
                    Icons.Default.HighQuality,
                    contentDescription = null
                )
            },
            title = stringResource(R.string.streaming_quality),
            subtitle = stringResource(R.string.preferred_audio_quality),
            selected = settings.streamingQuality.localizedLabel(),
            options = StreamingQuality.entries.map {
                it.localizedLabel() to it
            },
            onSelect = viewModel::onStreamingQualityChange
        )

        SectionTitle(stringResource(R.string.notifications_section))

        SettingsToggleItem(
            icon = Icons.Default.Notifications,
            label = stringResource(R.string.playback_notification),
            subtitle = stringResource(R.string.playback_notification_description),
            checked = settings.playbackNotificationsEnabled,
            onCheckedChange = viewModel::onPlaybackNotificationsToggle
        )

        SectionTitle(stringResource(R.string.appearance_section))

        SelectorRow(
            icon = {
                Icon(
                    Icons.Default.Palette,
                    contentDescription = null
                )
            },
            title = stringResource(R.string.theme),
            subtitle = stringResource(R.string.choose_app_appearance),
            selected = settings.theme.localizedLabel(),
            options = AppTheme.entries.map {
                it.localizedLabel() to it
            },
            onSelect = viewModel::onThemeChange
        )

        SectionTitle(stringResource(R.string.language_section))

        SelectorRow(
            icon = {
                Icon(
                    Icons.Default.Language,
                    contentDescription = null
                )
            },
            title = stringResource(R.string.app_language),
            subtitle = stringResource(R.string.choose_preferred_language),
            selected = languageOptions
                .firstOrNull { it.second == settings.language }
                ?.first
                ?: stringResource(R.string.language_system_default),
            options = languageOptions,
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
                stringResource(R.string.logout),
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

@Composable
private fun StreamingQuality.localizedLabel(): String =
    stringResource(
        when (this) {
            StreamingQuality.LOW -> R.string.quality_low
            StreamingQuality.NORMAL -> R.string.quality_normal
            StreamingQuality.HIGH -> R.string.quality_high
        }
    )

@Composable
private fun AppTheme.localizedLabel(): String =
    stringResource(
        when (this) {
            AppTheme.SYSTEM -> R.string.theme_system
            AppTheme.LIGHT -> R.string.theme_light
            AppTheme.DARK -> R.string.theme_dark
        }
    )
