package com.yourapp.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.yourapp.music.core.navigation.NavGraph
import com.yourapp.music.core.theme.MusicAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MusicAppTheme {
                // safeDrawingPadding() keeps every screen's content (including
                // top app bar back buttons) below the status bar / away from
                // cutouts and the nav bar. Without this, Android 15+ (which
                // forces edge-to-edge for apps targeting SDK 35) draws our
                // content underneath the system bars — the back arrow icon
                // can end up visually rendered where the status bar overlaps
                // it, so what you see and what's actually tappable fall out
                // of alignment and taps land on nothing.
                Surface(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                    val navController = rememberNavController()
                    NavGraph(navController = navController)
                }
            }
        }
    }
}
