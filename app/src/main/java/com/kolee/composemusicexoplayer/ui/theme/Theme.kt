package com.kolee.composemusicexoplayer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController

private val SpotifyColorPalette = darkColors(
    primary = Color(0xFF1DB954),
    primaryVariant = Color(0xFF1ED760),
    secondary = Color.Black,
    background = Color.Black,
    surface = Color.Black,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun ComposeMusicExoPlayerTheme(content: @Composable () -> Unit) {
    val systemUiController = rememberSystemUiController()

    systemUiController.setSystemBarsColor(color = Color.Black)
    systemUiController.setStatusBarColor(color = Color.Black)

    MaterialTheme(
        colors = SpotifyColorPalette,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
