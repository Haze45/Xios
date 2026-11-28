package com.yourapp.music.presentation.auth.login

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
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.isLoginSuccessful) {
        if (state.isLoginSuccessful) {
            onLoginSuccess()
        }
    }

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
                        singleLine = true,
                        enabled = !state.isLoading,
                        visualTransformation =
                            PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password
                        ),
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
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription =
                stringResource(R.string.xios_logo_description),
            modifier = Modifier.size(180.dp)
        )

        Spacer(Modifier.height(20.dp))

        Text(
            stringResource(R.string.welcome_back),
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(Modifier.height(8.dp))

        Text(
            stringResource(R.string.login_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(Modifier.height(28.dp))

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

        Spacer(Modifier.height(14.dp))

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

        state.errorMessage?.let { message ->
            Spacer(Modifier.height(12.dp))

            AppMessageText(
                message,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (state.canResendVerificationEmail) {
            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick = viewModel::resendVerificationEmail,
                enabled = !state.isLoading
            ) {
                Text("Resend verification email")
            }
        }

        if (state.verificationEmailSent) {
            Spacer(Modifier.height(8.dp))

            Text(
                text = "Verification email sent. Please check your inbox and spam folder.",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.height(22.dp))

        Button(
            onClick = viewModel::login,
            enabled = !state.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )

                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.signing_in))
            } else {
                Text(stringResource(R.string.log_in))
            }
        }

        Spacer(Modifier.height(18.dp))

        AuthOrDivider()

        Spacer(Modifier.height(18.dp))

        GoogleSignInButton(
            enabled = !state.isLoading,
            isLoading = state.isLoading,
            onLoadingChange = viewModel::setGoogleLoading,
            onTokenReceived = viewModel::loginWithGoogle,
            onError = viewModel::onGoogleError
        )

        Spacer(Modifier.height(18.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.no_account_prompt),
                color = TextSecondary
            )

            TextButton(
                onClick = onNavigateToRegister,
                enabled = !state.isLoading
            ) {
                Text(
                    stringResource(R.string.sign_up),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun AuthOrDivider() {
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
}
