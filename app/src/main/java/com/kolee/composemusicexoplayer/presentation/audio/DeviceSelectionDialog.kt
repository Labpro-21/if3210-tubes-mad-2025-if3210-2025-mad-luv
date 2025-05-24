package com.kolee.composemusicexoplayer.presentation.audio

import android.media.AudioDeviceInfo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.kolee.composemusicexoplayer.presentation.music_screen.PlayerViewModel

@Composable
fun DeviceSelectionDialog(
    viewModel: PlayerViewModel,
    onDismiss: () -> Unit
) {
   
    val availableDevices by viewModel.availableDevicesFlow.collectAsState()
    val currentDevice by viewModel.currentDeviceFlow.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Audio Output") },
        text = {
            Column {
                if (availableDevices.isEmpty()) {
                    Text(
                        text = "No audio devices available",
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    availableDevices.forEach { device ->
                        DeviceItem(
                            device = device,
                            isSelected = currentDevice?.id == device.id,
                            onClick = {
                                viewModel.selectAudioDevice(device)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun DeviceItem(
    device: AudioDeviceManager.AudioDevice,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon: ImageVector = when (device.type) {
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> Icons.Default.Bluetooth
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> Icons.Default.Headset
            AudioDeviceInfo.TYPE_USB_DEVICE -> Icons.Default.Usb
            else -> Icons.Default.Speaker
        }

        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = device.name,
            modifier = Modifier.weight(1f)
        )

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colors.primary
            )
        }
    }
}