package com.yourapp.music.presentation.auth.register

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourapp.music.R
import com.yourapp.music.presentation.components.AppMessageText
import com.yourapp.music.core.theme.BackgroundBlack
import com.yourapp.music.core.theme.TextSecondary
import com.yourapp.music.presentation.auth.GoogleSignInButton

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.isRegisterSuccessful) {
        if (state.isRegisterSuccessful) {
            onRegisterSuccess()
        }
    }

    // Email verification confirmation
    if (state.isVerificationEmailSent) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundBlack)
                .systemBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Verify your email",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "We've sent a verification link to ${state.email}. " +
                        "Please open your inbox and verify your email address. " +
                        "After verification, return to the login screen.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onNavigateToLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Go to login")
            }
        }

        return
    }

    // Google account linking dialog
    if (state.showGoogleLinkDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!state.isLoading) {
                    viewModel.dismissGoogleLinkDialog()
                }
            },
            title = {
                Text(
                    stringResource(R.string.link_google_account_title)
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(
                        12.dp
                    )
                ) {
                    Text(
                        stringResource(
                            R.string.link_google_account_message,
                            state.pendingGoogleEmail.orEmpty()
                        )
                    )

                    OutlinedTextField(
                        value = state.googleLinkPassword,
                        onValueChange =
                            viewModel::onGoogleLinkPasswordChange,
                        label = {
                            Text(stringResource(R.string.password))
                        },
                        visualTransformation =
                            PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password
                        ),
                        singleLine = true,
                        enabled = !state.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )

                    state.errorMessage?.let { message ->
                        AppMessageText(
                            message,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmGoogleAccountLink,
                    enabled = !state.isLoading
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(stringResource(R.string.link_account))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = viewModel::dismissGoogleLinkDialog,
                    enabled = !state.isLoading
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBlack)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription =
                stringResource(R.string.xios_logo_description),
            modifier = Modifier.size(130.dp)
        )

        Spacer(Modifier.height(14.dp))

        Text(
            stringResource(R.string.create_account),
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(Modifier.height(6.dp))

        Text(
            stringResource(R.string.register_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(Modifier.height(22.dp))

        OutlinedTextField(
            value = state.displayName,
            onValueChange = viewModel::onNameChange,
            label = { Text(stringResource(R.string.name)) },
            singleLine = true,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            label = { Text(stringResource(R.string.email)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email
            ),
            singleLine = true,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = { Text(stringResource(R.string.password)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            singleLine = true,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = state.confirmPassword,
            onValueChange = viewModel::onConfirmPasswordChange,
            label = {
                Text(stringResource(R.string.confirm_password))
            },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            singleLine = true,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        )

        state.errorMessage?.let { message ->
            Spacer(Modifier.height(10.dp))

            AppMessageText(
                message,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(18.dp))

        Button(
            onClick = viewModel::register,
            enabled = !state.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )

                Spacer(Modifier.width(10.dp))

                Text(
                    stringResource(R.string.creating_account_loading)
                )
            } else {
                Text(stringResource(R.string.sign_up))
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            HorizontalDivider(
                Modifier.weight(1f),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Text(
                stringResource(R.string.or),
                modifier = Modifier.padding(horizontal = 14.dp),
                color = TextSecondary
            )

            HorizontalDivider(
                Modifier.weight(1f),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }

        Spacer(Modifier.height(16.dp))

        GoogleSignInButton(
            enabled = !state.isLoading,
            isLoading = state.isLoading,
            onLoadingChange = viewModel::setGoogleLoading,
            onTokenReceived = { token, email ->
                viewModel.registerWithGoogle(
                    idToken = token,
                    googleEmail = email
                )
            },
            onError = viewModel::onGoogleError
        )

        Spacer(Modifier.height(14.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.have_account_prompt),
                color = TextSecondary
            )

            TextButton(
                onClick = onNavigateToLogin,
                enabled = !state.isLoading
            ) {
                Text(
                    stringResource(R.string.log_in),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
