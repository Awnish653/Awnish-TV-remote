package com.example.protocol

import android.util.Log
import com.example.data.database.DeviceDao
import com.example.data.database.DeviceEntity
import com.example.data.models.ConnectionState
import com.example.data.models.RemoteKey
import com.example.data.models.TvDevice
import com.example.data.models.TvType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TvRemoteManager(private val deviceDao: DeviceDao) {
    private val TAG = "TvRemoteManager"
    private val scope = CoroutineScope(Dispatchers.IO)

    private val androidTvProtocol = AndroidTvRemoteProtocol()
    private val samsungTizenProtocol = SamsungTizenProtocol()
    private val lgWebOsProtocol = LgWebOsProtocol()

    private var activeProtocol: TvControlProtocol = androidTvProtocol

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _activeDevice = MutableStateFlow<TvDevice?>(null)
    val activeDevice: StateFlow<TvDevice?> = _activeDevice.asStateFlow()

    init {
        // Collect state from protocols
        scope.launch {
            androidTvProtocol.connectionState.collect { updateStateIfActive(androidTvProtocol, it) }
        }
        scope.launch {
            samsungTizenProtocol.connectionState.collect { updateStateIfActive(samsungTizenProtocol, it) }
        }
        scope.launch {
            lgWebOsProtocol.connectionState.collect { updateStateIfActive(lgWebOsProtocol, it) }
        }
    }

    private fun updateStateIfActive(protocol: TvControlProtocol, state: ConnectionState) {
        if (protocol == activeProtocol) {
            _connectionState.value = state
            _activeDevice.value = protocol.connectedDevice.value

            if (state is ConnectionState.Connected) {
                scope.launch {
                    val dev = state.device
                    deviceDao.insertOrUpdateDevice(
                        DeviceEntity(
                            id = dev.id,
                            name = dev.name,
                            ipAddress = dev.ipAddress,
                            port = dev.port,
                            tvType = dev.type.name,
                            isAuthorized = true,
                            lastConnectedTimestamp = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }

    fun selectProtocol(type: TvType): TvControlProtocol {
        return when (type) {
            TvType.ANDROID_TV -> androidTvProtocol
            TvType.SAMSUNG_TIZEN -> samsungTizenProtocol
            TvType.LG_WEBOS -> lgWebOsProtocol
            TvType.GENERIC_SMART_TV -> androidTvProtocol
        }
    }

    suspend fun connectToDevice(device: TvDevice): Boolean {
        if (_connectionState.value is ConnectionState.Connected && _activeDevice.value?.id == device.id) {
            return true
        }

        // Disconnect previous protocol if different
        activeProtocol.disconnect()
        activeProtocol = selectProtocol(device.type)

        val success = activeProtocol.connect(device)
        _activeDevice.value = device
        return success
    }

    suspend fun disconnect() {
        activeProtocol.disconnect()
        _activeDevice.value = null
        _connectionState.value = ConnectionState.Disconnected
    }

    suspend fun sendKey(key: RemoteKey): Boolean {
        return activeProtocol.sendKey(key)
    }

    suspend fun sendTextInput(text: String): Boolean {
        return activeProtocol.sendTextInput(text)
    }

    suspend fun submitPairingPin(pin: String): Boolean {
        val success = activeProtocol.submitPairingPin(pin)
        if (success) {
            _activeDevice.value?.let { current ->
                val authorized = current.copy(isAuthorized = true)
                _activeDevice.value = authorized
                deviceDao.insertOrUpdateDevice(
                    DeviceEntity(
                        id = authorized.id,
                        name = authorized.name,
                        ipAddress = authorized.ipAddress,
                        port = authorized.port,
                        tvType = authorized.type.name,
                        isAuthorized = true,
                        lastConnectedTimestamp = System.currentTimeMillis()
                    )
                )
            }
        }
        return success
    }

    suspend fun autoConnectLastDevice() {
        try {
            val lastDeviceEntity = deviceDao.getLastConnectedDevice() ?: return
            val lastDevice = TvDevice(
                id = lastDeviceEntity.id,
                name = lastDeviceEntity.name,
                ipAddress = lastDeviceEntity.ipAddress,
                port = lastDeviceEntity.port,
                type = try { TvType.valueOf(lastDeviceEntity.tvType) } catch (e: Exception) { TvType.ANDROID_TV },
                isAuthorized = lastDeviceEntity.isAuthorized,
                lastConnectedTimestamp = lastDeviceEntity.lastConnectedTimestamp
            )

            Log.d(TAG, "Auto-connecting to last device: ${lastDevice.name} (${lastDevice.ipAddress})")
            connectToDevice(lastDevice)
        } catch (e: Exception) {
            Log.e(TAG, "Auto-connect failed: ${e.message}")
        }
    }
}
