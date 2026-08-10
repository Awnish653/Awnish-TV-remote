package com.example.protocol

import com.example.data.models.ConnectionState
import com.example.data.models.RemoteKey
import com.example.data.models.TvDevice
import kotlinx.coroutines.flow.StateFlow

interface TvControlProtocol {
    val connectionState: StateFlow<ConnectionState>
    val connectedDevice: StateFlow<TvDevice?>

    suspend fun connect(device: TvDevice): Boolean
    suspend fun disconnect()
    suspend fun sendKey(key: RemoteKey): Boolean
    suspend fun sendTextInput(text: String): Boolean
    suspend fun submitPairingPin(pin: String): Boolean
}
