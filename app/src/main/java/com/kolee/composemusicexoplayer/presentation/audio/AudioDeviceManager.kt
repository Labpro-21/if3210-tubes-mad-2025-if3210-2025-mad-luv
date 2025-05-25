package com.kolee.composemusicexoplayer.presentation.audio

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@RequiresApi(Build.VERSION_CODES.S)
class AudioDeviceManager(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _availableDevices = MutableStateFlow<List<AudioDevice>>(emptyList())
    val availableDevices: StateFlow<List<AudioDevice>> = _availableDevices.asStateFlow()

    private val _currentDevice = MutableStateFlow<AudioDevice?>(null)
    val currentDevice: StateFlow<AudioDevice?> = _currentDevice.asStateFlow()

    private val _deviceChangeError = MutableStateFlow<String?>(null)
    val deviceChangeError: StateFlow<String?> = _deviceChangeError.asStateFlow()

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            Log.d("AudioDeviceManager", "Devices added")
            updateAvailableDevices()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            Log.d("AudioDeviceManager", "Devices removed")
            updateAvailableDevices()
        }
    }

    private val connectionReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.S)
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    if (_currentDevice.value?.bluetoothAddress == device?.address) {
                        selectInternalSpeaker()
                    }
                    updateAvailableDevices()
                }
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    updateAvailableDevices()
                }
                AudioManager.ACTION_HEADSET_PLUG -> {
                    val state = intent.getIntExtra("state", -1)
                    if (state == 0) { // Headset unplugged
                        if (_currentDevice.value?.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                            _currentDevice.value?.type == AudioDeviceInfo.TYPE_WIRED_HEADSET) {
                            selectInternalSpeaker()
                        }
                    }
                    updateAvailableDevices()
                }
                AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED -> {
                    updateAvailableDevices()
                }
                AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                    // Headphones were disconnected
                    selectInternalSpeaker()
                }
            }
        }
    }

    init {
        registerListeners()
        updateAvailableDevices()
         selectInternalSpeaker()
    }

    private fun registerListeners() {
        audioManager.registerAudioDeviceCallback(deviceCallback, null)

        val intentFilter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(AudioManager.ACTION_HEADSET_PLUG)
            addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        }
        context.registerReceiver(connectionReceiver, intentFilter)
    }

    fun updateAvailableDevices() {
        val devices = mutableListOf<AudioDevice>()
        val currentlySelectedId = _currentDevice.value?.id

        // Add internal speaker
        devices.add(AudioDevice(
            id = "internal_speaker",
            name = "Speaker",
            type = AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
            isActive = currentlySelectedId == "internal_speaker",
            deviceInfo = null,
            bluetoothAddress = null
        ))

        // Add external devices
        val outputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        outputDevices.forEach { deviceInfo ->
            when (deviceInfo.type) {
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> {
                    devices.add(AudioDevice(
                        id = "bluetooth_${deviceInfo.id}",
                        name = deviceInfo.productName?.toString() ?: "Bluetooth Speaker",
                        type = deviceInfo.type,
                        isActive = currentlySelectedId == "bluetooth_${deviceInfo.id}",
                        deviceInfo = deviceInfo,
                        bluetoothAddress = deviceInfo.address
                    ))
                }
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> {
                    devices.add(AudioDevice(
                        id = "bluetooth_sco_${deviceInfo.id}",
                        name = "Bluetooth Headset",
                        type = deviceInfo.type,
                        isActive = currentlySelectedId == "bluetooth_sco_${deviceInfo.id}",
                        deviceInfo = deviceInfo,
                        bluetoothAddress = deviceInfo.address
                    ))
                }
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_WIRED_HEADSET -> {
                    devices.add(AudioDevice(
                        id = "wired_${deviceInfo.id}",
                        name = "Headphones",
                        type = deviceInfo.type,
                        isActive = currentlySelectedId == "wired_${deviceInfo.id}",
                        deviceInfo = deviceInfo,
                        bluetoothAddress = null
                    ))
                }
                AudioDeviceInfo.TYPE_USB_DEVICE,
                AudioDeviceInfo.TYPE_USB_HEADSET -> {
                    devices.add(AudioDevice(
                        id = "usb_${deviceInfo.id}",
                        name = deviceInfo.productName?.toString() ?: "USB Audio",
                        type = deviceInfo.type,
                        isActive = currentlySelectedId == "usb_${deviceInfo.id}",
                        deviceInfo = deviceInfo,
                        bluetoothAddress = null
                    ))
                }
            }
        }

        _availableDevices.value = devices
        updateCurrentDeviceStatus()
    }

    private fun updateCurrentDeviceStatus() {
        _currentDevice.value?.let { current ->
            val updatedCurrent = _availableDevices.value.find { it.id == current.id }
            if (updatedCurrent != null) {
                _currentDevice.value = updatedCurrent
            } else {
                selectInternalSpeaker()
            }
        } ?: run {
            selectInternalSpeaker()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun selectDevice(device: AudioDevice): Boolean {
        return try {
            Log.d("AudioDeviceManager", "Attempting to select device: ${device.name}")

            val success = when {
                device.id == "internal_speaker" -> selectInternalSpeaker()
                device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> selectBluetoothA2DP(device)
                device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> selectBluetoothSCO(device)
                device.deviceInfo != null -> selectWiredDevice(device)
                else -> false
            }

            if (success) {
                _currentDevice.value = device.copy(isActive = true)
                _deviceChangeError.value = null
                updateAvailableDevices()
                true
            } else {
                _deviceChangeError.value = "Failed to switch to ${device.name}"
                false
            }
        } catch (e: Exception) {
            Log.e("AudioDeviceManager", "Error selecting device", e)
            _deviceChangeError.value = "Error: ${e.localizedMessage}"
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun selectInternalSpeaker(): Boolean {
        return try {
            // Stop Bluetooth SCO if active
            if (audioManager.isBluetoothScoOn) {
                audioManager.stopBluetoothSco()
                audioManager.isBluetoothScoOn = false
            }

            // Set speakerphone on
            audioManager.isSpeakerphoneOn = true

            // Set audio mode to normal
            audioManager.mode = AudioManager.MODE_NORMAL

            // For Android 8.0+, clear communication device
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioManager.clearCommunicationDevice()
            }

            _currentDevice.value = AudioDevice(
                id = "internal_speaker",
                name = "Speaker",
                type = AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
                isActive = true
            )
            true
        } catch (e: Exception) {
            Log.e("AudioDeviceManager", "Error switching to speaker", e)
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun selectBluetoothA2DP(device: AudioDevice): Boolean {
        return try {
            // 1. Matikan speaker dan mode sebelumnya
            audioManager.isSpeakerphoneOn = false
            audioManager.mode = AudioManager.MODE_NORMAL


            // 3. Untuk Android 8.0+ (API 26+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                device.deviceInfo?.let { audioManager.setCommunicationDevice(it) }
            }

            true
        } catch (e: Exception) {
            Log.e("AudioDeviceManager", "Error switching to Bluetooth A2DP", e)
            false
        }
    }


    private fun selectBluetoothSCO(device: AudioDevice): Boolean {
        return try {
            // Turn off speakerphone
            audioManager.isSpeakerphoneOn = false

            // Start Bluetooth SCO
            audioManager.startBluetoothSco()
            audioManager.isBluetoothScoOn = true

            // Set audio mode for communication
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

            // For Android 8.0+, set communication device
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && device.deviceInfo != null) {
                audioManager.setCommunicationDevice(device.deviceInfo)
            }

            true
        } catch (e: Exception) {
            Log.e("AudioDeviceManager", "Error switching to Bluetooth SCO", e)
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun selectWiredDevice(device: AudioDevice): Boolean {
        return try {
            audioManager.isSpeakerphoneOn = false
            audioManager.mode = AudioManager.MODE_NORMAL

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                device.deviceInfo?.let { audioManager.setCommunicationDevice(it) }
            }

            true
        } catch (e: Exception) {
            Log.e("AudioDeviceManager", "Error switching to wired device", e)
            false
        }
    }

    fun cleanup() {
        try {
            audioManager.unregisterAudioDeviceCallback(deviceCallback)
            context.unregisterReceiver(connectionReceiver)
            audioManager.mode = AudioManager.MODE_NORMAL
        } catch (e: Exception) {
            Log.e("AudioDeviceManager", "Error during cleanup", e)
        }
    }

    data class AudioDevice(
        val id: String,
        val name: String,
        val type: Int,
        val isActive: Boolean,
        val deviceInfo: AudioDeviceInfo? = null,
        val bluetoothAddress: String? = null
    ) {
        fun isBluetooth(): Boolean {
            return type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
        }
    }
}