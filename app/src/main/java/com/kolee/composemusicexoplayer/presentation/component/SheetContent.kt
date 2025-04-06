package com.kolee.composemusicexoplayer.presentation.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.kolee.composemusicexoplayer.data.roomdb.MusicEntity
import com.kolee.composemusicexoplayer.presentation.music_screen.PlayerEvent
import com.kolee.composemusicexoplayer.presentation.music_screen.PlayerViewModel
import com.kolee.composemusicexoplayer.utils.Constants
import com.kolee.composemusicexoplayer.utils.move
import com.kolee.composemusicexoplayer.utils.swap
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

@Composable
fun SheetContent(
    isExpanded: Boolean,
    playerVM: PlayerViewModel,
    onBack: () -> Unit,
    onExpandPlayer: () -> Unit
) {
    val musicUiState by playerVM.uiState.collectAsState()
    val musicList = remember { mutableStateListOf<MusicEntity>() }

    LaunchedEffect(musicUiState.musicList) {
        musicList.swap(musicUiState.musicList)
    }

    val reorderableState = rememberReorderableLazyListState(
        onMove = { from, to ->
            musicList.swap(musicList.move(from.index, to.index))
        },
        onDragEnd = { from, to ->
            playerVM.onEvent(PlayerEvent.updateMusicList(musicList.move(from, to)))
        }
    )

    BackHandler(isExpanded) {
        onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.surface)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(Constants.BOTTOM_SHEET_PEAK_HEIGHT)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.2f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colors.primary)
            )
        }

        LazyColumn(
            state = reorderableState.listState,
            modifier = Modifier
                .reorderable(reorderableState)
                .detectReorderAfterLongPress(reorderableState)
        ) {
            items(items = musicList, key = { it.hashCode() }) { music ->
                ReorderableItem(reorderableState, key = music.hashCode()) { isDragging ->
                    val elevation by animateDpAsState(
                        targetValue = if (isDragging) 4.dp else 0.dp,
                        label = ""
                    )

                    val currentAudioId = musicUiState.currentPlayedMusic.audioId
                    SheetMusicItem(
                        music = music,
                        selected = music.audioId == currentAudioId,
                        elevation = elevation,
                        onClick = {
                            playerVM.onEvent(PlayerEvent.Play(music))
                            onExpandPlayer()
                        }
                    )
                }
            }
        }
    }
}
