package com.kolee.composemusicexoplayer.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kolee.composemusicexoplayer.R
import com.kolee.composemusicexoplayer.ui.theme.TintDefaultColor

@Composable
fun PlayerControlButtons(
    isPlaying: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                painter = painterResource(id = R.drawable.ic_previous_filled_rounded),
                contentDescription = "Previous",
                tint = MaterialTheme.colors.TintDefaultColor
            )
        }

        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colors.primary,
            modifier = Modifier
                .size(64.dp)
                .clickable(onClick = onPlayPause)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    painter = painterResource(
                        id = if (isPlaying) R.drawable.ic_pause_filled_rounded else R.drawable.ic_play_filled_rounded
                    ),
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colors.onPrimary
                )
            }
        }

        IconButton(onClick = onNext) {
            Icon(
                painter = painterResource(id = R.drawable.ic_next_filled_rounded),
                contentDescription = "Next",
                tint = MaterialTheme.colors.TintDefaultColor
            )
        }
    }
}
