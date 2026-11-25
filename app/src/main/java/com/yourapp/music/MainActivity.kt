package com.yourapp.music

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
import androidx.compose.ui.Modifier
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.yourapp.music.core.navigation.NavGraph
import com.yourapp.music.core.theme.MusicAppTheme
import com.yourapp.music.presentation.settings.AppSettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val appSettingsViewModel: AppSettingsViewModel =
                hiltViewModel()

            val settings by appSettingsViewModel.settings
                .collectAsStateWithLifecycle()

            LaunchedEffect(settings.language) {
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
            }

            MusicAppTheme(
                appTheme = settings.theme
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground
                ) {
                    val navController =
                        rememberNavController()

                    NavGraph(
                        navController = navController
                    )
                }
            }
        }
    }
}
