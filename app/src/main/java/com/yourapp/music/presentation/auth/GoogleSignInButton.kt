package com.yourapp.music.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.yourapp.music.R
import kotlinx.coroutines.launch

@Composable
fun GoogleSignInButton(
    enabled: Boolean,
    isLoading: Boolean,
    onLoadingChange: (Boolean) -> Unit,
    onTokenReceived: (String, String) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager =
        remember(context) { CredentialManager.create(context) }

    Button(
        onClick = {
            scope.launch {
                onLoadingChange(true)
                var handedOffToAuth = false
                try {
                    val googleIdOption =
                        GetGoogleIdOption.Builder()
                            .setServerClientId(
                                context.getString(
                                    R.string.default_web_client_id
                                )
                            )
                            .setFilterByAuthorizedAccounts(
                                false
                            )
                            .setAutoSelectEnabled(false)
                            .build()
                    val request =
                        GetCredentialRequest.Builder()
                            .addCredentialOption(
                                googleIdOption
                            )
                            .build()
                    val result =
                        credentialManager.getCredential(
                            context = context,
                            request = request
                        )
                    val credential = result.credential
                    if (credential is CustomCredential &&
                        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                    ) {
                        val googleCredential =
                            GoogleIdTokenCredential.createFrom(
                                credential.data
                            )
                        val token = googleCredential.idToken
                        val googleEmail =
                            googleCredential.id
                        handedOffToAuth = true
                        onTokenReceived(token, googleEmail)
                    } else {
                        onError("google_sign_in_failed")
                    }
                } catch (_: GetCredentialCancellationException) {
                    // Cancellation is intentional; do not show an authentication error.
                } catch (_: Exception) {
                    onError("google_sign_in_failed")
                } finally {
                    // Once the token is handed off, the ViewModel owns the loading state.
                    if (!handedOffToAuth) onLoadingChange(
                        false
                    )
                }
            }
        },
        enabled = enabled && !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.ic_google_logo),
                    contentDescription = stringResource(R.string.google_logo_description),
                    modifier = Modifier.size(30.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.continue_with_google),
                    modifier = Modifier
                )
            }
        }
    }
}
