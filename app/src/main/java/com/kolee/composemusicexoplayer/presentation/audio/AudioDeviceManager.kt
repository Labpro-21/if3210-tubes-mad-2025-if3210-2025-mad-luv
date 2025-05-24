import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class AudioDeviceManager(context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val _availableDevices = MutableLiveData<List<AudioDevice>>()
    val availableDevices: LiveData<List<AudioDevice>> = _availableDevices

    private val _currentDevice = MutableLiveData<AudioDevice>()
    val currentDevice: LiveData<AudioDevice> = _currentDevice

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            updateAvailableDevices()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            updateAvailableDevices()
        }
    }

    private val connectionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    // Check if the disconnected device was our current device
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    if (currentDevice.value?.id == device?.address) {
                        // Fall back to internal speaker
                        selectDevice(AudioDevice(
                            id = "internal",
                            name = "Speaker Internal",
                            type = AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
                            isActive = true
                        ))

                        // Show error message (you'll need to handle this in your UI)
                        // viewModel.showError("Device disconnected, switched to internal speaker")
                    }
                }
            }
        }
    }

    init {
        audioManager.registerAudioDeviceCallback(deviceCallback, null)
        updateAvailableDevices()
        _currentDevice.value = AudioDevice(
            id = "internal",
            name = "Speaker Internal",
            type = AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
            isActive = true
        )
    }

    fun updateAvailableDevices() {
        val devices = mutableListOf<AudioDevice>()

        devices.add(AudioDevice(
            id = "internal",
            name = "Speaker Internal",
            type = AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
            isActive = _currentDevice.value?.id == "internal"
        ))

        audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).forEach { deviceInfo ->
            if (deviceInfo.type != AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                devices.add(AudioDevice(
                    id = deviceInfo.id.toString(),
                    name = when (deviceInfo.type) {
                        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth Speaker (${deviceInfo.productName})"
                        AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth Headset (${deviceInfo.productName})"
                        AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Headphones"
                        AudioDeviceInfo.TYPE_USB_DEVICE -> "USB Device (${deviceInfo.productName})"
                        else -> "External Device (${deviceInfo.productName})"
                    },
                    type = deviceInfo.type,
                    isActive = _currentDevice.value?.id == deviceInfo.id.toString()
                ))
            }
        }

        _availableDevices.value = devices
    }

    fun selectDevice(device: AudioDevice) {
        _currentDevice.value = device
        updateAvailableDevices()

        // Handle Bluetooth device connection if needed
        if (device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
            // You might need to handle Bluetooth connection here
        }
    }

    fun cleanup() {
        audioManager.unregisterAudioDeviceCallback(deviceCallback)
    }

    data class AudioDevice(
        val id: String,
        val name: String,
        val type: Int,
        val isActive: Boolean
    )
}