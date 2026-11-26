package com.yourapp.music

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.yourapp.music.core.navigation.NavGraph
import com.yourapp.music.core.theme.MusicAppTheme
import com.yourapp.music.presentation.settings.AppSettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_OPEN_PLAYER =
            "open_player_from_notification"
    }

    private var openPlayerFromNotification by mutableStateOf(
        false
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        openPlayerFromNotification =
            intent?.getBooleanExtra(
                EXTRA_OPEN_PLAYER,
                false
            ) == true

        enableEdgeToEdge()

        setContent {
            val appSettingsViewModel: AppSettingsViewModel =
                hiltViewModel()

            val settings by appSettingsViewModel.settings
                .collectAsStateWithLifecycle()

            var localeReady by remember {
                mutableStateOf(false)
            }

            LaunchedEffect(settings.language) {
                val desiredLocales =
                    if (settings.language == "system") {
                        ""
                    } else {
                        settings.language
                    }

                val currentLocales =
                    AppCompatDelegate.getApplicationLocales()
                        .toLanguageTags()

                if (currentLocales != desiredLocales) {
                    val locales =
                        if (settings.language == "system") {
                            LocaleListCompat.getEmptyLocaleList()
                        } else {
                            LocaleListCompat.forLanguageTags(
                                settings.language
                            )
                        }

                    AppCompatDelegate.setApplicationLocales(
                        locales
                    )
                } else {
                    localeReady = true
                }
            }

            MusicAppTheme(
                appTheme = settings.theme
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground
                ) {
                    if (localeReady) {
                        val navController =
                            rememberNavController()

                        NavGraph(
                            navController = navController,
                            openPlayerFromNotification =
                                openPlayerFromNotification,
                            onPlayerNavigationHandled = {
                                openPlayerFromNotification =
                                    false

                                val updatedIntent =
                                    Intent(intent)
                                updatedIntent.removeExtra(
                                    EXTRA_OPEN_PLAYER
                                )
                                intent = updatedIntent
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        setIntent(intent)

        openPlayerFromNotification =
            intent.getBooleanExtra(EXTRA_OPEN_PLAYER, false)
    }
}
