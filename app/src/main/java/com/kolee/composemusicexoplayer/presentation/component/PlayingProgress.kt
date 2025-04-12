package com.kolee.composemusicexoplayer.presentation.component

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.kolee.composemusicexoplayer.presentation.music_screen.PlayerEvent
import com.kolee.composemusicexoplayer.ui.theme.TextDefaultColor
import kotlin.math.min
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

    var sliderPosition by rememberSaveable { mutableStateOf(0f) }
    var internalCurrentDuration by rememberSaveable { mutableStateOf(musicUiState.currentDuration) }
    var isUserInteracting by remember { mutableStateOf(false) }
    var previousSongId by rememberSaveable { mutableStateOf(musicUiState.currentPlayedMusic.audioId) }

    LaunchedEffect(isPlaying) {
        while (isPlaying && maxDuration > 0 && !isUserInteracting) {
            kotlinx.coroutines.delay(100)
            if(internalCurrentDuration>maxDuration){
                sliderPosition = 0f
                internalCurrentDuration = 0L
            }
            internalCurrentDuration += 100
            sliderPosition = internalCurrentDuration.toFloat() / maxDuration
        }
    }

    LaunchedEffect(currentDuration) {
        internalCurrentDuration = currentDuration
        if (!isUserInteracting) {
            sliderPosition = internalCurrentDuration.toFloat() / maxDuration
        }
    }

    LaunchedEffect(musicUiState.currentPlayedMusic.audioId) {
        if (musicUiState.currentPlayedMusic.audioId != previousSongId) {
            sliderPosition = 0f
            internalCurrentDuration = 0L
            previousSongId = musicUiState.currentPlayedMusic.audioId
        }
    }

    val currentMinutes = internalCurrentDuration.milliseconds.inWholeMinutes
    val currentSeconds = internalCurrentDuration.milliseconds.inWholeSeconds % 60
    val currentDurationString = String.format("%02d:%02d", currentMinutes, currentSeconds)

    val maxMinutes = maxDuration.milliseconds.inWholeMinutes
    val maxSeconds = maxDuration.milliseconds.inWholeSeconds % 60
    val maxDurationString = String.format("%02d:%02d", maxMinutes, maxSeconds)

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = sliderPosition.coerceIn(0f, 1f),
            onValueChange = {
                sliderPosition = it
                isUserInteracting = true
                internalCurrentDuration = (it * maxDuration).toLong()
            },
            onValueChangeFinished = {
                onChangeFinished(sliderPosition)
                isUserInteracting = false
                playerVM.onEvent(PlayerEvent.UpdateProgress(internalCurrentDuration))
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
