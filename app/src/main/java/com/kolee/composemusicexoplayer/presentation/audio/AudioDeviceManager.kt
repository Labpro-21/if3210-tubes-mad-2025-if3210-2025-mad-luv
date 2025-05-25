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
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@RequiresApi(Build.VERSION_CODES.S)
class AudioDeviceManager(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val handler = Handler(Looper.getMainLooper())

    private val _availableDevices = MutableStateFlow<List<AudioDevice>>(emptyList())
    val availableDevices: StateFlow<List<AudioDevice>> = _availableDevices.asStateFlow()

    private val _currentDevice = MutableStateFlow<AudioDevice?>(null)
    val currentDevice: StateFlow<AudioDevice?> = _currentDevice.asStateFlow()

    private val _deviceChangeError = MutableStateFlow<String?>(null)
    val deviceChangeError: StateFlow<String?> = _deviceChangeError.asStateFlow()

    private val deviceCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        object : AudioDeviceCallback() {
            @RequiresApi(Build.VERSION_CODES.S)
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                Log.d("AudioDeviceManager", "Devices added")
                handler.postDelayed({
                    updateAvailableDevices()
                    // Auto-select bluetooth device
                    addedDevices?.forEach { device ->
                        if (device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP) {
                            val bluetoothDevice = AudioDevice(
                                id = "bluetooth_${device.id}",
                                name = device.productName?.toString() ?: "Bluetooth Speaker",
                                type = device.type,
                                isActive = false,
                                deviceInfo = device,
                                bluetoothAddress = device.address
                            )
                            Log.d("AudioDeviceManager", "Auto-selecting Bluetooth A2DP: ${bluetoothDevice.name}")
                            selectDevice(bluetoothDevice)
                        }
                    }
                }, 500) // Delay untuk memastikan device sudah siap
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                Log.d("AudioDeviceManager", "Devices removed")
                handler.postDelayed({
                    updateAvailableDevices()
                }, 200)
            }
        }
    } else null

    private val connectionReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.S)
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    Log.d("AudioDeviceManager", "Bluetooth ACL disconnected")
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    }

                    if (_currentDevice.value?.bluetoothAddress == device?.address) {
                        handler.postDelayed({
                            selectInternalSpeaker()
                        }, 200)
                    }
                    handler.postDelayed({
                        updateAvailableDevices()
                    }, 300)
                }
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    Log.d("AudioDeviceManager", "Bluetooth ACL connected")
                    handler.postDelayed({
                        updateAvailableDevices()

                        val outputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                        outputDevices.forEach { deviceInfo ->
                            if (deviceInfo.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP) {
                                val bluetoothDevice = AudioDevice(
                                    id = "bluetooth_${deviceInfo.id}",
                                    name = deviceInfo.productName?.toString() ?: "Bluetooth Speaker",
                                    type = deviceInfo.type,
                                    isActive = false,
                                    deviceInfo = deviceInfo,
                                    bluetoothAddress = deviceInfo.address
                                )
                                Log.d("AudioDeviceManager", "Auto-selecting connected Bluetooth device: ${bluetoothDevice.name}")
                                selectDevice(bluetoothDevice)
                            }
                        }
                    }, 1000)
                }
                AudioManager.ACTION_HEADSET_PLUG -> {
                    val state = intent.getIntExtra("state", -1)
                    Log.d("AudioDeviceManager", "Headset plug state: $state")
                    if (state == 0) { // Headset unplugged
                        if (_currentDevice.value?.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                            _currentDevice.value?.type == AudioDeviceInfo.TYPE_WIRED_HEADSET) {
                            handler.postDelayed({
                                selectInternalSpeaker()
                            }, 200)
                        }
                    } else if (state == 1) {
                        handler.postDelayed({
                            updateAvailableDevices()

                            val outputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                            outputDevices.forEach { deviceInfo ->
                                if (deviceInfo.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                                    deviceInfo.type == AudioDeviceInfo.TYPE_WIRED_HEADSET) {
                                    val wiredDevice = AudioDevice(
                                        id = "wired_${deviceInfo.id}",
                                        name = "Headphones",
                                        type = deviceInfo.type,
                                        isActive = false,
                                        deviceInfo = deviceInfo,
                                        bluetoothAddress = null
                                    )
                                    selectDevice(wiredDevice)
                                }
                            }
                        }, 300)
                    }
                    handler.postDelayed({
                        updateAvailableDevices()
                    }, 400)
                }
                AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED -> {
                    Log.d("AudioDeviceManager", "SCO audio state updated")
                    handler.postDelayed({
                        updateAvailableDevices()
                    }, 200)
                }
                AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                    Log.d("AudioDeviceManager", "Audio becoming noisy")

                    handler.postDelayed({
                        selectInternalSpeaker()
                    }, 200)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && deviceCallback != null) {
            audioManager.registerAudioDeviceCallback(deviceCallback, null)
        }

        val intentFilter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(AudioManager.ACTION_HEADSET_PLUG)
            addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        }

        try {
            context.registerReceiver(connectionReceiver, intentFilter)
        } catch (e: Exception) {
            Log.e("AudioDeviceManager", "Error registering receiver", e)
        }
    }

    fun updateAvailableDevices() {
        Log.d("AudioDeviceManager", "Updating available devices")
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
        val addedBluetoothAddresses = mutableSetOf<String>()

        outputDevices.forEach { deviceInfo ->
            Log.d("AudioDeviceManager", "Found device: ${deviceInfo.productName}, type: ${deviceInfo.type}")
            when (deviceInfo.type) {
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> {
                    // Only add A2DP devices, skip SCO for the same address
                    val address = deviceInfo.address
                    if (address != null && !addedBluetoothAddresses.contains(address)) {
                        addedBluetoothAddresses.add(address)
                        val deviceName = deviceInfo.productName?.toString() ?: "Bluetooth Speaker"
                        Log.d("AudioDeviceManager", "Adding Bluetooth A2DP device: $deviceName")
                        devices.add(AudioDevice(
                            id = "bluetooth_${deviceInfo.id}",
                            name = deviceName,
                            type = deviceInfo.type,
                            isActive = currentlySelectedId == "bluetooth_${deviceInfo.id}",
                            deviceInfo = deviceInfo,
                            bluetoothAddress = address
                        ))
                    }
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
        Log.d("AudioDeviceManager", "Available devices updated: ${devices.size} devices")
        devices.forEach { device ->
            Log.d("AudioDeviceManager", "Device: ${device.name}, ID: ${device.id}, Active: ${device.isActive}")
        }
        updateCurrentDeviceStatus()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun updateCurrentDeviceStatus() {
        val currentDeviceId = _currentDevice.value?.id
        if (currentDeviceId != null) {
            val updatedDevices = _availableDevices.value.map { device ->
                device.copy(isActive = device.id == currentDeviceId)
            }
            _availableDevices.value = updatedDevices

            val updatedCurrent = updatedDevices.find { it.id == currentDeviceId }
            if (updatedCurrent != null) {
                _currentDevice.value = updatedCurrent
            } else {
                selectInternalSpeaker()
            }
        } else {
            selectInternalSpeaker()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun selectDevice(device: AudioDevice): Boolean {
        return try {
            Log.d("AudioDeviceManager", "Attempting to select device: ${device.name} (${device.id})")

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

                val updatedDevices = _availableDevices.value.map {
                    it.copy(isActive = it.id == device.id)
                }
                _availableDevices.value = updatedDevices

                Log.d("AudioDeviceManager", "Successfully selected device: ${device.name}")
                true
            } else {
                _deviceChangeError.value = "Failed to switch to ${device.name}"
                Log.e("AudioDeviceManager", "Failed to select device: ${device.name}")
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
            Log.d("AudioDeviceManager", "Selecting internal speaker")

            if (audioManager.isBluetoothScoOn) {
                audioManager.stopBluetoothSco()
                audioManager.isBluetoothScoOn = false
            }

            audioManager.isSpeakerphoneOn = true

            audioManager.mode = AudioManager.MODE_NORMAL

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    audioManager.clearCommunicationDevice()
                } catch (e: Exception) {
                    Log.w("AudioDeviceManager", "Could not clear communication device", e)
                }
            }

            val speakerDevice = AudioDevice(
                id = "internal_speaker",
                name = "Speaker",
                type = AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
                isActive = true
            )
            _currentDevice.value = speakerDevice

            val updatedDevices = _availableDevices.value.map {
                it.copy(isActive = it.id == "internal_speaker")
            }
            _availableDevices.value = updatedDevices

            true
        } catch (e: Exception) {
            Log.e("AudioDeviceManager", "Error switching to speaker", e)
            false
        }
    }

    private fun selectBluetoothA2DP(device: AudioDevice): Boolean {
        return try {
            Log.d("AudioDeviceManager", "Selecting Bluetooth A2DP: ${device.name}")


            audioManager.isSpeakerphoneOn = false

            // Stop Bluetooth SCO
            if (audioManager.isBluetoothScoOn) {
                audioManager.stopBluetoothSco()
                audioManager.isBluetoothScoOn = false
            }


            audioManager.mode = AudioManager.MODE_NORMAL


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && device.deviceInfo != null) {
                try {
                    val result = audioManager.setCommunicationDevice(device.deviceInfo)
                    Log.d("AudioDeviceManager", "setCommunicationDevice result: $result")
                } catch (e: Exception) {
                    Log.w("AudioDeviceManager", "Could not set communication device", e)
                }
            }


            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {

                val oldMode = audioManager.mode
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION


                handler.postDelayed({
                    audioManager.mode = AudioManager.MODE_NORMAL
                }, 100)
            }

            true
        } catch (e: Exception) {
            Log.e("AudioDeviceManager", "Error switching to Bluetooth A2DP", e)
            false
        }
    }

    private fun selectBluetoothSCO(device: AudioDevice): Boolean {
        return try {
            Log.d("AudioDeviceManager", "Selecting Bluetooth SCO: ${device.name}")

            audioManager.isSpeakerphoneOn = false

            audioManager.startBluetoothSco()
            audioManager.isBluetoothScoOn = true

            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && device.deviceInfo != null) {
                try {
                    audioManager.setCommunicationDevice(device.deviceInfo)
                } catch (e: Exception) {
                    Log.w("AudioDeviceManager", "Could not set communication device", e)
                }
            }

            true
        } catch (e: Exception) {
            Log.e("AudioDeviceManager", "Error switching to Bluetooth SCO", e)
            false
        }
    }

    private fun selectWiredDevice(device: AudioDevice): Boolean {
        return try {
            Log.d("AudioDeviceManager", "Selecting wired device: ${device.name}")

            audioManager.isSpeakerphoneOn = false
            audioManager.mode = AudioManager.MODE_NORMAL

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && device.deviceInfo != null) {
                try {
                    audioManager.setCommunicationDevice(device.deviceInfo)
                } catch (e: Exception) {
                    Log.w("AudioDeviceManager", "Could not set communication device", e)
                }
            }

            true
        } catch (e: Exception) {
            Log.e("AudioDeviceManager", "Error switching to wired device", e)
            false
        }
    }

    fun cleanup() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && deviceCallback != null) {
                audioManager.unregisterAudioDeviceCallback(deviceCallback)
            }
            context.unregisterReceiver(connectionReceiver)
            audioManager.mode = AudioManager.MODE_NORMAL
            handler.removeCallbacksAndMessages(null)
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