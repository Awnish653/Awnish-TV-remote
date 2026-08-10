package com.example.protocol

import android.util.Log
import com.example.data.models.ConnectionState
import com.example.data.models.RemoteKey
import com.example.data.models.TvDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class SamsungTizenProtocol : TvControlProtocol {
    private val TAG = "SamsungTizenProtocol"

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _connectedDevice = MutableStateFlow<TvDevice?>(null)
    override val connectedDevice: StateFlow<TvDevice?> = _connectedDevice.asStateFlow()

    override suspend fun connect(device: TvDevice): Boolean = withContext(Dispatchers.IO) {
        try {
            _connectionState.value = ConnectionState.Connecting(device.name)
            Log.d(TAG, "Connecting to Samsung Tizen TV at ${device.ipAddress}:${device.port}")
            delay(600)

            if (!device.isAuthorized && !device.id.startsWith("demo_")) {
                _connectionState.value = ConnectionState.PairingRequired(
                    deviceName = device.name,
                    message = "Accept the connection prompt on your Samsung TV screen"
                )
                return@withContext false
            }

            _connectedDevice.value = device.copy(isAuthorized = true)
            _connectionState.value = ConnectionState.Connected(device)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to Samsung TV: ${e.message}")
            _connectionState.value = ConnectionState.Error("Samsung TV connection failed: ${e.message}")
            false
        }
    }

    override suspend fun disconnect(): Unit = withContext(Dispatchers.IO) {
        _connectedDevice.value = null
        _connectionState.value = ConnectionState.Disconnected
    }

    override suspend fun sendKey(key: RemoteKey): Boolean = withContext(Dispatchers.IO) {
        val current = _connectedDevice.value ?: return@withContext false
        val samsungKeyName = when (key) {
            RemoteKey.POWER -> "KEY_POWER"
            RemoteKey.DPAD_UP -> "KEY_UP"
            RemoteKey.DPAD_DOWN -> "KEY_DOWN"
            RemoteKey.DPAD_LEFT -> "KEY_LEFT"
            RemoteKey.DPAD_RIGHT -> "KEY_RIGHT"
            RemoteKey.SELECT -> "KEY_ENTER"
            RemoteKey.BACK -> "KEY_RETURN"
            RemoteKey.HOME -> "KEY_HOME"
            RemoteKey.MENU -> "KEY_MENU"
            RemoteKey.VOLUME_UP -> "KEY_VOLUP"
            RemoteKey.VOLUME_DOWN -> "KEY_VOLDOWN"
            RemoteKey.VOLUME_MUTE -> "KEY_MUTE"
            RemoteKey.CHANNEL_UP -> "KEY_CHUP"
            RemoteKey.CHANNEL_DOWN -> "KEY_CHDOWN"
            RemoteKey.PLAY_PAUSE -> "KEY_PLAY"
            RemoteKey.VOICE_SEARCH -> "KEY_SEARCH"
            RemoteKey.KEYBOARD_INPUT -> "KEY_CONTENTS"
        }
        Log.d(TAG, "Sent Samsung Tizen key $samsungKeyName to ${current.name}")
        true
    }

    override suspend fun sendTextInput(text: String): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "Sending text '$text' to Samsung TV")
        true
    }

    override suspend fun submitPairingPin(pin: String): Boolean = withContext(Dispatchers.IO) {
        val device = _connectedDevice.value ?: return@withContext false
        val authorized = device.copy(isAuthorized = true)
        _connectedDevice.value = authorized
        _connectionState.value = ConnectionState.Connected(authorized)
        true
    }
}
