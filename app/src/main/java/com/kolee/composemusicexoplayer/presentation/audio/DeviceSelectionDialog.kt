package com.kolee.composemusicexoplayer.presentation.audio

import android.media.AudioDeviceInfo
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kolee.composemusicexoplayer.presentation.music_screen.PlayerViewModel
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun DeviceSelectionDialog(
    viewModel: PlayerViewModel,
    onDismiss: () -> Unit
) {
    val availableDevices by viewModel.availableDevicesFlow.collectAsState()
    val currentDevice by viewModel.currentDeviceFlow.collectAsState()
    var isSelecting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Select Audio Output",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                if (availableDevices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Loading audio devices...")
                        }
                    }
                } else {
                    // Refresh button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { viewModel.refreshAudioDevices() },
                            enabled = !isSelecting
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Refresh")
                        }
                    }

                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))

                    // Device list
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        items(availableDevices) { device ->
                            DeviceItem(
                                device = device,
                                isSelected = currentDevice?.id == device.id,
                                isSelecting = isSelecting && currentDevice?.id != device.id,
                                onClick = {
                                    if (!isSelecting && currentDevice?.id != device.id) {
                                        isSelecting = true
                                        viewModel.selectAudioDevice(device)

                                        // Reset selecting state after a delay
                                        kotlinx.coroutines.GlobalScope.launch {
                                            kotlinx.coroutines.delay(1000)
                                            isSelecting = false
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSelecting
            ) {
                Text("Close")
            }
        },
        dismissButton = {
            if (currentDevice != null) {
                TextButton(
                    onClick = { viewModel.refreshAudioDevices() },
                    enabled = !isSelecting
                ) {
                    Text("Refresh")
                }
            }
        }
    )
}

@Composable
fun DeviceItem(
    device: AudioDeviceManager.AudioDevice,
    isSelected: Boolean,
    isSelecting: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = !isSelecting && !isSelected,
                onClick = onClick
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon: ImageVector = when (device.type) {
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> Icons.Default.BluetoothAudio
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> Icons.Default.Bluetooth
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> Icons.Default.Headset
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_USB_HEADSET -> Icons.Default.Usb
            else -> Icons.Default.Speaker
        }

        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (isSelected) MaterialTheme.colors.primary
            else MaterialTheme.colors.onSurface
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.name,
                style = MaterialTheme.typography.body1,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colors.primary
                else MaterialTheme.colors.onSurface
            )

            val typeText = when (device.type) {
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth Audio"
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth Call"
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired"
                AudioDeviceInfo.TYPE_USB_DEVICE -> "USB"
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Built-in"
                else -> "External"
            }

            Text(
                text = typeText,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        }

        when {
            isSelecting -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            }
            isSelected -> {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Not selected",
                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}