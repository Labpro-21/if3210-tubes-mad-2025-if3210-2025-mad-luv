package com.kolee.composemusicexoplayer.presentation.component

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.kolee.composemusicexoplayer.presentation.music_screen.PlayerEvent
import com.kolee.composemusicexoplayer.ui.theme.TextDefaultColor
import kotlin.time.Duration.Companion.milliseconds
import com.kolee.composemusicexoplayer.presentation.music_screen.PlayerViewModel

@OptIn(UnstableApi::class)
@Composable
fun PlayingProgress(
    playerVM: PlayerViewModel,
    maxDuration: Long,
    currentDuration: Long,
    isPlaying: Boolean,
    onChangeFinished: (Float) -> Unit
) {
    val musicUiState by playerVM.uiState.collectAsState()

    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isUserInteracting by remember { mutableStateOf(false) }
    var previousSongId by remember { mutableStateOf(musicUiState.currentPlayedMusic.audioId) }

    LaunchedEffect(musicUiState.currentPlayedMusic.audioId) {
        if (musicUiState.currentPlayedMusic.audioId != previousSongId) {
            sliderPosition = 0f
            previousSongId = musicUiState.currentPlayedMusic.audioId
        }
    }

    LaunchedEffect(currentDuration, maxDuration, isUserInteracting) {
        if (!isUserInteracting && maxDuration > 0) {
            sliderPosition = (currentDuration.toFloat() / maxDuration).coerceIn(0f, 1f)
        }
    }

    val displayCurrentDuration = if (isUserInteracting) {
        (sliderPosition * maxDuration).toLong()
    } else {
        currentDuration
    }

    val currentMinutes = displayCurrentDuration.milliseconds.inWholeMinutes
    val currentSeconds = displayCurrentDuration.milliseconds.inWholeSeconds % 60
    val currentDurationString = String.format("%02d:%02d", currentMinutes, currentSeconds)

    val maxMinutes = maxDuration.milliseconds.inWholeMinutes
    val maxSeconds = maxDuration.milliseconds.inWholeSeconds % 60
    val maxDurationString = String.format("%02d:%02d", maxMinutes, maxSeconds)

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = sliderPosition,
            onValueChange = { newValue ->
                sliderPosition = newValue
                isUserInteracting = true
            },
            onValueChangeFinished = {
                val seekPosition = (sliderPosition * maxDuration).toLong()
                onChangeFinished(sliderPosition)
                playerVM.onEvent(PlayerEvent.UpdateProgress(seekPosition))
                isUserInteracting = false
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color.Green,
                activeTrackColor = Color.Green,
                inactiveTrackColor = Color.DarkGray
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = currentDurationString,
                style = MaterialTheme.typography.caption.copy(
                    color = MaterialTheme.colors.TextDefaultColor
                )
            )
            Text(
                text = maxDurationString,
                style = MaterialTheme.typography.caption.copy(
                    color = MaterialTheme.colors.TextDefaultColor
                )
            )
        }
    }
}