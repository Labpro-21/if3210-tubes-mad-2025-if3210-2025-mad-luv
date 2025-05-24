package com.kolee.composemusicexoplayer.presentation.qr

import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
fun QRCodeScanner(
    onQRCodeScanned: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember { PreviewView(context) }
    
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
                it.setAnalyzer(executor, QRCodeAnalyzer { qrCodeUri ->
                    onQRCodeScanned(qrCodeUri)
                })
            }
        
        try {
            // Unbind all use cases before rebinding
            cameraProvider.unbindAll()
            
            // Bind use cases to camera
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
    
    Box(modifier = modifier) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
        
        // Back button
        IconButton(
            onClick = { (context as? QRScannerActivity)?.finish() },
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back"
            )
        }
    }
}

class QRCodeAnalyzer(private val onQRCodeScanned: (Uri) -> Unit) : ImageAnalysis.Analyzer {
    private val reader = MultiFormatReader()
    private var isScanning = true
    
    override fun analyze(image: ImageProxy) {
        if (!isScanning) {
            image.close()
            return
        }
        
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
                try {
                    val uri = Uri.parse(text)
                    isScanning = false
                    onQRCodeScanned(uri)
                } catch (e: Exception) {
                    Log.e("QRCodeAnalyzer", "Invalid URI format: $text", e)
                }
            }
        } catch (e: Exception) {
            // QR code not found in this frame, continue scanning
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
