package com.kolee.composemusicexoplayer.presentation.MusicPlayerSheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.kolee.composemusicexoplayer.presentation.audio.DeviceSelectionDialog
import com.kolee.composemusicexoplayer.presentation.component.MotionContent
import com.kolee.composemusicexoplayer.presentation.component.ShareButton
import com.kolee.composemusicexoplayer.presentation.music_screen.PlayerViewModel
import com.kolee.composemusicexoplayer.utils.currentFraction
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MusicPlayerSheet(
    playerVM: PlayerViewModel,
    navController: NavHostController,
    onCollapse: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberBottomSheetState(initialValue = BottomSheetValue.Collapsed)
    )

    val fraction = scaffoldState.currentFraction()
    val musicUiState by playerVM.uiState.collectAsStateWithLifecycle()

    var showDeviceDialog by remember { mutableStateOf(false) }
    val currentDevice by playerVM.currentDeviceFlow.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        MotionContent(
            playerVM = playerVM,
            fraction = fraction,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .zIndex(0f),
            onBack = {
                scope.launch {
                    scaffoldState.bottomSheetState.collapse()
                    onCollapse()
                }
            }
        )

        // Top action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .zIndex(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Device selection button
            IconButton(
                onClick = { showDeviceDialog = true },
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(
                    Icons.Default.Devices,
                    contentDescription = "Select Audio Device",
                    tint = Color.White
                )
            }

            // Share button
            ShareButton(
                songId = musicUiState.currentPlayedMusic.audioId,
                modifier = Modifier
            )
        }

        // Current device indicator at bottom
        currentDevice?.let { device ->
            Text(
                text = "Playing on: ${device.name}",
                style = MaterialTheme.typography.caption,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
        }
    }

    if (showDeviceDialog) {
        DeviceSelectionDialog(
            viewModel = playerVM,
            onDismiss = { showDeviceDialog = false }
        )
    }
}