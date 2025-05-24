package com.kolee.composemusicexoplayer.presentation.component

import android.content.Intent
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kolee.composemusicexoplayer.presentation.qr.QRScannerActivity

@Composable
fun ScanQRButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    
    FloatingActionButton(
        onClick = {
            val intent = Intent(context, QRScannerActivity::class.java)
            context.startActivity(intent)
        },
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.QrCodeScanner,
            contentDescription = "Scan QR Code",
            modifier = Modifier.size(24.dp)
        )
    }
}
