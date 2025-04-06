package com.kolee.composemusicexoplayer.ui.theme

import androidx.compose.material.Colors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import kotlin.math.abs

val Purple200 = Color(0xFFBB86FC)
val Purple500 = Color(0xFF6200EE)
val Purple700 = Color(0xFF3700B3)
val Teal200 = Color(0xFF03DAC5)
val Green700 = Color(0xFF1DB954)

val Colors.TextDefaultColor: Color
    @Composable get() =  Color(0xFFEEEEEE)
val Colors.TintDefaultColor: Color
    @Composable get() = Color(0xFFEEEEEE)

fun getRandomColor(seed: String): Color {
    val colors = listOf(
        Color(0xFFE57373), // Red
        Color(0xFF81C784), // Green
        Color(0xFF64B5F6), // Blue
        Color(0xFFFFB74D), // Orange
        Color(0xFF9575CD), // Purple
        Color(0xFF4DB6AC), // Teal
        Color(0xFFF06292), // Pink
        Color(0xFFAED581), // Light Green
        Color(0xFF4DD0E1)  // Cyan
    )
    val index = abs(seed.hashCode()) % colors.size
    return colors[index]
}

