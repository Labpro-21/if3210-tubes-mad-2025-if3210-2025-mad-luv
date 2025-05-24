package com.kolee.composemusicexoplayer.presentation.qr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.kolee.composemusicexoplayer.MainActivity
import com.kolee.composemusicexoplayer.utils.DeepLinkHandler

class QRScannerActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            setContent {
                QRScannerScreen(
                    onQRCodeScanned = { uri -> handleScannedQRCode(uri) },
                    onClose = { finish() }
                )
            }
        } else {
            Toast.makeText(
                this,
                "Camera permission is required to scan QR codes",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                setContent {
                    QRScannerScreen(
                        onQRCodeScanned = { uri -> handleScannedQRCode(uri) },
                        onClose = { finish() }
                    )
                }
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun handleScannedQRCode(uri: Uri) {
        try {
            Log.d("QRScanner", "Scanned URI: $uri")
            val content = uri.toString()
            if (!DeepLinkHandler.isValidPurrytifyQR(content)) {
                Toast.makeText(
                    this,
                    "Invalid QR code. Not a Purrytify song link.",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
            val songId = DeepLinkHandler.extractSongIdFromUri(uri)
                ?: DeepLinkHandler.extractSongIdFromURL(content)

            if (songId != null) {
                val intent = Intent(this, MainActivity::class.java).apply {
                    data = uri
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(
                    this,
                    "Invalid QR code. Not a Purrytify song link.",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            Log.e("QRScanner", "Error handling QR code", e)
            Toast.makeText(
                this,
                "Error scanning QR code: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun QRScannerScreen(
    onQRCodeScanned: (Uri) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (cameraPermissionState.status.isGranted) {
            QRCodeScanner(
                onQRCodeScanned = onQRCodeScanned,
                onClose = onClose,
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
                    text = "Camera permission is required to scan QR codes",
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
                TextButton(onClick = onClose) {
                    Text("Cancel")
                }
            }
        }
    }
}
