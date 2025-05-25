package com.kolee.composemusicexoplayer.presentation.qr

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.kolee.composemusicexoplayer.utils.DeepLinkHandler
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
fun QRCodeScanner(
    onQRCodeScanned: (Uri) -> Unit,
    onClose: () -> Unit,
    isProcessing: Boolean = false,
    processingMessage: String = "",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember { PreviewView(context) }
    var isScanning by remember { mutableStateOf(true) }

    LaunchedEffect(key1 = previewView) {
        val cameraProvider = suspendCoroutine<ProcessCameraProvider> { continuation ->
            cameraProviderFuture.addListener(
                {
                    continuation.resume(cameraProviderFuture.get())
                },
                ContextCompat.getMainExecutor(context)
            )
        }

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(executor, QRCodeAnalyzer(
                    onQRCodeScanned = { qrCodeUri ->
                        if (isScanning && !isProcessing) {
                            isScanning = false
                            onQRCodeScanned(qrCodeUri)
                        }
                    },
                    onInvalidQR = { content ->
                        if (isScanning && !isProcessing) {
                            Toast.makeText(
                                context,
                                "❌ Invalid QR code. Not a Purrytify song link.\nScanned: ${content.take(50)}...",
                                Toast.LENGTH_LONG
                            ).show()

                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                kotlinx.coroutines.delay(1000)
                                isScanning = true
                            }
                        }
                    }
                ))
            }

        try {
            cameraProvider.unbindAll()

            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis
            )
        } catch (e: Exception) {
            Log.e("QRScanner", "Use case binding failed", e)
        }
    }

    LaunchedEffect(isProcessing) {
        if (!isProcessing) {
            kotlinx.coroutines.delay(1000)
            isScanning = true
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .align(Alignment.Center)
                    .border(
                        width = 1.dp, // Border sangat tipis
                        color = if (isProcessing) Color.Yellow else Color.White.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .background(Color.Transparent)
            ) {
                val cornerColor = if (isProcessing) Color.Yellow else Color.Green
                val cornerSize = 15.dp // Lebih kecil
                val cornerWidth = 2.dp // Lebih tipis

                Box(
                    modifier = Modifier
                        .size(cornerSize)
                        .align(Alignment.TopStart)
                        .border(
                            width = cornerWidth,
                            color = cornerColor,
                            shape = RoundedCornerShape(topStart = 8.dp)
                        )
                )
                Box(
                    modifier = Modifier
                        .size(cornerSize)
                        .align(Alignment.TopEnd)
                        .border(
                            width = cornerWidth,
                            color = cornerColor,
                            shape = RoundedCornerShape(topEnd = 8.dp)
                        )
                )
                Box(
                    modifier = Modifier
                        .size(cornerSize)
                        .align(Alignment.BottomStart)
                        .border(
                            width = cornerWidth,
                            color = cornerColor,
                            shape = RoundedCornerShape(bottomStart = 8.dp)
                        )
                )
                Box(
                    modifier = Modifier
                        .size(cornerSize)
                        .align(Alignment.BottomEnd)
                        .border(
                            width = cornerWidth,
                            color = cornerColor,
                            shape = RoundedCornerShape(bottomEnd = 8.dp)
                        )
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = "Scan Purrytify QR Code",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(48.dp)) // Balance the back button
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isProcessing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.Yellow,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = processingMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Yellow,
                            textAlign = TextAlign.Center
                        )
                    }

                    Text(
                        text = "Please wait...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Yellow.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else {
                    Text(
                        text = "Point your camera at a Purrytify QR code",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "The song will automatically start playing",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

class QRCodeAnalyzer(
    private val onQRCodeScanned: (Uri) -> Unit,
    private val onInvalidQR: (String) -> Unit
) : ImageAnalysis.Analyzer {
    private val reader = MultiFormatReader()

    override fun analyze(image: ImageProxy) {
        val buffer = image.planes[0].buffer
        val data = buffer.toByteArray()
        val width = image.width
        val height = image.height

        val source = PlanarYUVLuminanceSource(
            data,
            width,
            height,
            0,
            0,
            width,
            height,
            false
        )

        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

        try {
            val result = reader.decode(binaryBitmap)
            val text = result.text

            if (text.isNotEmpty()) {
                Log.d("QRCodeAnalyzer", " QR Code detected: $text")

                Log.d("QRCodeAnalyzer", "Extracting QR to URL: $text")

                try {
                    if (DeepLinkHandler.isValidPurrytifyQR(text)) {
                        val uri = Uri.parse(text)
                        Log.d("QRCodeAnalyzer", "Valid Purrytify QR detected, processing...")
                        Log.d("QRCodeAnalyzer", "Extracted URI: $uri")
                        onQRCodeScanned(uri)
                    } else {
                        Log.w("QRCodeAnalyzer", "❌ Invalid Purrytify QR detected: $text")
                        onInvalidQR(text)
                    }
                } catch (e: Exception) {
                    Log.e("QRCodeAnalyzer", "Error processing QR: $text", e)
                    onInvalidQR(text)
                }
            }
        } catch (e: Exception) {
        } finally {
            image.close()
        }
    }

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()
        val data = ByteArray(remaining())
        get(data)
        return data
    }
}
