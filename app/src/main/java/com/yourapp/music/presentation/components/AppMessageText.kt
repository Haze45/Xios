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
        "auth_fields_required" -> R.string.auth_fields_required
        "auth_name_required" -> R.string.auth_name_required
        "auth_invalid_email" -> R.string.auth_invalid_email
        "auth_invalid_credentials" -> R.string.auth_invalid_credentials
        "auth_email_already_in_use" -> R.string.auth_email_already_in_use
        "auth_weak_password" -> R.string.auth_weak_password
        "auth_password_mismatch" -> R.string.auth_password_mismatch
        "auth_too_many_requests" -> R.string.auth_too_many_requests
        "auth_network_error" -> R.string.auth_network_error
        "auth_user_disabled" -> R.string.auth_user_disabled
        "auth_provider_disabled" -> R.string.auth_provider_disabled
        "auth_account_exists_different_provider" -> R.string.auth_account_exists_different_provider
        "auth_password_required" -> R.string.auth_password_required
        "auth_login_failed" -> R.string.auth_login_failed
        "auth_register_failed" -> R.string.auth_register_failed
        "auth_failed" -> R.string.auth_failed
        "google_sign_in_failed" -> R.string.google_sign_in_failed
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
