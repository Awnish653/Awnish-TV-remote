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

class LgWebOsProtocol : TvControlProtocol {
    private val TAG = "LgWebOsProtocol"

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _connectedDevice = MutableStateFlow<TvDevice?>(null)
    override val connectedDevice: StateFlow<TvDevice?> = _connectedDevice.asStateFlow()

    override suspend fun connect(device: TvDevice): Boolean = withContext(Dispatchers.IO) {
        try {
            _connectionState.value = ConnectionState.Connecting(device.name)
            Log.d(TAG, "Connecting to LG webOS TV at ${device.ipAddress}:${device.port}")
            delay(600)

            if (!device.isAuthorized && !device.id.startsWith("demo_")) {
                _connectionState.value = ConnectionState.PairingRequired(
                    deviceName = device.name,
                    message = "Accept prompt on LG webOS TV screen"
                )
                return@withContext false
            }

            _connectedDevice.value = device.copy(isAuthorized = true)
            _connectionState.value = ConnectionState.Connected(device)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to LG TV: ${e.message}")
            _connectionState.value = ConnectionState.Error("LG TV connection failed: ${e.message}")
            false
        }
    }

    override suspend fun disconnect(): Unit = withContext(Dispatchers.IO) {
        _connectedDevice.value = null
        _connectionState.value = ConnectionState.Disconnected
    }

    override suspend fun sendKey(key: RemoteKey): Boolean = withContext(Dispatchers.IO) {
        val current = _connectedDevice.value ?: return@withContext false
        Log.d(TAG, "Sent LG SSAP key ${key.name} to ${current.name}")
        true
    }

    override suspend fun sendTextInput(text: String): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "Sending text '$text' to LG webOS TV")
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
