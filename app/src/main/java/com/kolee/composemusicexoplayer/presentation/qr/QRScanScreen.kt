package com.kolee.composemusicexoplayer.presentation.qr

import android.Manifest
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.kolee.composemusicexoplayer.data.network.NetworkSensing
import com.kolee.composemusicexoplayer.presentation.music_screen.PlayerViewModel
import com.kolee.composemusicexoplayer.utils.DeepLinkHandler
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun QRScanScreen(
    playerViewModel: PlayerViewModel,
    navController: NavController,
    networkSensing: NetworkSensing
) {
    val context = LocalContext.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val scope = rememberCoroutineScope()

    var isProcessing by remember { mutableStateOf(false) }
    var processingMessage by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (cameraPermissionState.status.isGranted) {
            QRCodeScanner(
                onQRCodeScanned = { uri ->
                    if (!isProcessing) {
                        isProcessing = true
                        processingMessage = "Processing QR code..."

                        scope.launch {
                            handleScannedQRCode(
                                uri = uri,
                                context = context,
                                playerViewModel = playerViewModel,
                                navController = navController,
                                networkSensing = networkSensing,
                                onProcessingUpdate = { message ->
                                    processingMessage = message
                                },
                                onComplete = {
                                    isProcessing = false
                                }
                            )
                        }
                    }
                },
                onClose = { navController.popBackStack() },
                isProcessing = isProcessing,
                processingMessage = processingMessage,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "📷 Camera Permission Required",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "Please grant camera permission to scan Purrytify song QR codes",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                Button(
                    onClick = { cameraPermissionState.launchPermissionRequest() },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text("Grant Camera Permission")
                }
                TextButton(onClick = { navController.popBackStack() }) {
                    Text("Cancel")
                }
            }
        }
    }
}

private suspend fun handleScannedQRCode(
    uri: Uri,
    context: android.content.Context,
    playerViewModel: PlayerViewModel,
    navController: NavController,
    networkSensing: NetworkSensing,
    onProcessingUpdate: (String) -> Unit,
    onComplete: () -> Unit
) {
    try {
        Log.d("QRScanner", "🔍 Processing scanned QR: $uri")
        val content = uri.toString()

        onProcessingUpdate("Extracting QR code...")
        Log.d("QRScanner", "📋 QR Content: $content")

        if (!DeepLinkHandler.isValidPurrytifyQR(content)) {
            Log.w("QRScanner", "❌ Invalid Purrytify QR detected")
            Toast.makeText(
                context,
                "❌ Invalid QR code. Not a Purrytify song link.\nScanned: ${content.take(50)}...",
                Toast.LENGTH_LONG
            ).show()
            onComplete()
            return
        }

        onProcessingUpdate("Detecting song ID...")
        val songId = DeepLinkHandler.extractSongIdFromURL(content)
            ?: DeepLinkHandler.extractSongIdFromUri(uri)

        Log.d("QRScanner", "🎵 Extracted Song ID: $songId")

        if (songId != null) {
            onProcessingUpdate("Checking network...")
            if (!networkSensing.isNetworkAvailable()) {
                Toast.makeText(
                    context,
                    "🌐 No internet connection. Cannot load song from server.",
                    Toast.LENGTH_LONG
                ).show()
                onComplete()
                return
            }

            onProcessingUpdate("Loading song...")
            try {
                playerViewModel.fetchAndPlaySharedSong(songId.toString())

                onProcessingUpdate("Starting playback...")

                navController.navigate("home") {
                    popUpTo("home") { inclusive = true }
                }

                Toast.makeText(
                    context,
                    "🎵 Song loaded and playing!",
                    Toast.LENGTH_SHORT
                ).show()

                Log.d("QRScanner", "Song successfully loaded and playing: $songId")

            } catch (e: Exception) {
                Log.e("QRScanner", " Error loading song", e)
                Toast.makeText(
                    context,
                    "❌ Failed to load song: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }

        } else {
            Toast.makeText(
                context,
                "❌ Invalid song ID in QR code.",
                Toast.LENGTH_LONG
            ).show()
        }
    } catch (e: Exception) {
        Log.e("QRScanner", "💥 Error handling QR code", e)
        Toast.makeText(
            context,
            "❌ Error scanning QR code: ${e.message}",
            Toast.LENGTH_LONG
        ).show()
    } finally {
        onComplete()
    }
}
