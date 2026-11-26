package com.yourapp.music.presentation.profile

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.yourapp.music.R
import com.yourapp.music.core.theme.BackgroundBlack
import com.yourapp.music.core.theme.ErrorRed
import com.yourapp.music.core.theme.SurfaceDark
import com.yourapp.music.core.theme.TextSecondary
import kotlinx.coroutines.launch
import java.io.File

/**
 * Bottom sheet for editing the signed-in user's display name and photo,
 * opened by tapping the avatar (or its edit badge) on ProfileScreen.
 * Shares the same [ProfileViewModel] instance as the screen that opened
 * it, so a successful save updates ProfileScreen immediately.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileSheet(
    onDismiss: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val sheetState =
        rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(state.displayName) }
    var pickedPhotoUri by remember {
        mutableStateOf<Uri?>(
            null
        )
    }

    // Clear any stale error the moment the sheet opens so a previous
    // failed save doesn't show up before the user has done anything here.
    LaunchedEffect(Unit) { viewModel.clearError() }

    val photoPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                // The picker's Uri is only readable for this immediate call —
                // copy the bytes into app-internal storage so the photo still
                // loads after the sheet closes and on future app launches.
                scope.launch {
                    copyToInternalStorage(
                        context,
                        uri
                    )?.let { pickedPhotoUri = it }
                }
            }
        }

    fun dismiss() {
        viewModel.clearError()
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = ::dismiss,
        sheetState = sheetState,
        containerColor = SurfaceDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.edit_profile),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(20.dp))

            Box(contentAlignment = Alignment.BottomEnd) {
                AsyncImage(
                    model = pickedPhotoUri
                        ?: state.photoUrl,
                    contentDescription = stringResource(R.string.profile_photo),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        }
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .border(
                            3.dp,
                            SurfaceDark,
                            CircleShape
                        )
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.CameraAlt,
                        contentDescription = stringResource(
                            R.string.change_photo
                        ),
                        tint = BackgroundBlack,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.name)) },
                singleLine = true,
                isError = state.errorMessage != null,
                modifier = Modifier.fillMaxWidth()
            )

            state.errorMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    it,
                    color = ErrorRed,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    12.dp
                )
            ) {
                OutlinedButton(
                    onClick = ::dismiss,
                    enabled = !state.isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        stringResource(R.string.cancel),
                        color = TextSecondary
                    )
                }
                Button(
                    onClick = {
                        viewModel.updateProfile(
                            displayName = name,
                            photoUri = pickedPhotoUri?.toString(),
                            onSaved = onDismiss
                        )
                    },
                    enabled = !state.isSaving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = BackgroundBlack,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    }
}

private fun copyToInternalStorage(
    context: Context,
    sourceUri: Uri
): Uri? {
    return try {
        val file = File(
            context.filesDir,
            "profile_photo_${System.currentTimeMillis()}.jpg"
        )
        context.contentResolver.openInputStream(sourceUri)
            ?.use { input ->
                file.outputStream()
                    .use { output -> input.copyTo(output) }
            }
        Uri.fromFile(file)
    } catch (_: Exception) {
        null
    }
}
