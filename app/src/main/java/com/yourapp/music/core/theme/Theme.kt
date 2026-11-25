package com.yourapp.music.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.yourapp.music.domain.model.AppTheme

private val DarkColors = darkColorScheme(
    primary = AccentGreen,
    onPrimary = BackgroundBlack,
    background = BackgroundBlack,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    outline = Color(0xFF444444)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF526B00),
    onPrimary = Color.White,
    background = Color(0xFFF5F6EF),
    onBackground = Color(0xFF191B13),
    surface = Color.White,
    onSurface = Color(0xFF191B13),
    surfaceVariant = Color(0xFFE8EADB),
    onSurfaceVariant = Color(0xFF44483A),
    error = Color(0xFFBA1A1A),
    outline = Color(0xFF77796D)
)

@Composable
fun MusicAppTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (appTheme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }

    MaterialTheme(
        colorScheme = if (useDarkTheme) {
            DarkColors
        } else {
            LightColors
        },
        typography = MusicTypography,
        shapes = MusicShapes,
        content = content
    )
}
