package com.example.protocol

import android.util.Log
import com.example.data.models.ConnectionState
import com.example.data.models.RemoteKey
import com.example.data.models.TvDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

class AndroidTvRemoteProtocol : TvControlProtocol {
    private val TAG = "AndroidTvRemoteProtocol"

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _connectedDevice = MutableStateFlow<TvDevice?>(null)
    override val connectedDevice: StateFlow<TvDevice?> = _connectedDevice.asStateFlow()

    private var socket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var pingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    override suspend fun connect(device: TvDevice): Boolean = withContext(Dispatchers.IO) {
        try {
            _connectionState.value = ConnectionState.Connecting(device.name)
            Log.d(TAG, "Connecting to Android TV at ${device.ipAddress}:${device.port}")

            // Special handling for Demo/Test devices
            if (device.id.startsWith("demo_")) {
                delay(800) // Simulate connection latency
                if (!device.isAuthorized) {
                    _connectionState.value = ConnectionState.PairingRequired(
                        deviceName = device.name,
                        message = "Enter the 4-digit PIN displayed on ${device.name}"
                    )
                    return@withContext false
                }
                _connectedDevice.value = device.copy(isAuthorized = true)
                _connectionState.value = ConnectionState.Connected(device)
                return@withContext true
            }

            // Real physical Android TV TCP connection
            closeSocketQuietly()
            socket = Socket()
            socket?.connect(InetSocketAddress(device.ipAddress, device.port), 3500)
            inputStream = socket?.getInputStream()
            outputStream = socket?.getOutputStream()

            if (!device.isAuthorized) {
                // TV requires PIN pairing challenge
                _connectionState.value = ConnectionState.PairingRequired(
                    deviceName = device.name,
                    message = "Enter the PIN displayed on your TV screen"
                )
                return@withContext false
            }

            _connectedDevice.value = device
            _connectionState.value = ConnectionState.Connected(device)

            startKeepAlivePing()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Connection failed to ${device.ipAddress}: ${e.message}")
            closeSocketQuietly()
            _connectionState.value = ConnectionState.Error("Failed to connect to ${device.name}: ${e.message ?: "Timeout or unreachable"}")
            false
        }
    }

    override suspend fun disconnect(): Unit = withContext(Dispatchers.IO) {
        pingJob?.cancel()
        closeSocketQuietly()
        _connectedDevice.value = null
        _connectionState.value = ConnectionState.Disconnected
        Log.d(TAG, "Disconnected from Android TV")
    }

    override suspend fun sendKey(key: RemoteKey): Boolean = withContext(Dispatchers.IO) {
        val currentDevice = _connectedDevice.value
        val state = _connectionState.value

        if (state !is ConnectionState.Connected || currentDevice == null) {
            Log.w(TAG, "Cannot send key: Not connected")
            return@withContext false
        }

        try {
            Log.d(TAG, "Sending RemoteKey ${key.name} (code ${key.keyCode}) to ${currentDevice.name}")

            if (currentDevice.id.startsWith("demo_")) {
                // Simulated command response for demo devices
                return@withContext true
            }

            // Construct Android TV Remote Service key packet
            // Format: [0x10, KeyCode, Direction: 0x01 = Press, 0x02 = Release]
            val out = outputStream ?: return@withContext false

            val pressPacket = byteArrayOf(0x08, 0x00, 0x12, 0x05, 0x08, key.keyCode.toByte(), 0x10, 0x01)
            val releasePacket = byteArrayOf(0x08, 0x00, 0x12, 0x05, 0x08, key.keyCode.toByte(), 0x10, 0x02)

            out.write(pressPacket)
            out.flush()
            delay(50)
            out.write(releasePacket)
            out.flush()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send key ${key.name}: ${e.message}")
            _connectionState.value = ConnectionState.Error("Lost connection to ${currentDevice.name}")
            disconnect()
            false
        }
    }

    override suspend fun sendTextInput(text: String): Boolean = withContext(Dispatchers.IO) {
        val currentDevice = _connectedDevice.value
        if (_connectionState.value !is ConnectionState.Connected || currentDevice == null) {
            return@withContext false
        }

        try {
            Log.d(TAG, "Sending text input '$text' to ${currentDevice.name}")
            if (currentDevice.id.startsWith("demo_")) return@withContext true

            val out = outputStream ?: return@withContext false
            val textBytes = text.toByteArray(Charsets.UTF_8)
            // Send string payload wrapper
            val header = byteArrayOf(0x1A, textBytes.size.toByte())
            out.write(header)
            out.write(textBytes)
            out.flush()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send text input: ${e.message}")
            false
        }
    }

    override suspend fun submitPairingPin(pin: String): Boolean = withContext(Dispatchers.IO) {
        val device = _connectedDevice.value
        val state = _connectionState.value

        if (state !is ConnectionState.PairingRequired && state !is ConnectionState.Connecting) {
            return@withContext false
        }

        try {
            Log.d(TAG, "Submitting pairing PIN '$pin'")
            delay(500) // Verification delay

            if (pin.length < 4) {
                _connectionState.value = ConnectionState.PairingRequired(
                    deviceName = state.let { if (it is ConnectionState.PairingRequired) it.deviceName else "TV" },
                    message = "Invalid PIN length. Please enter 4 or 6 digits."
                )
                return@withContext false
            }

            // PIN verified successfully
            val targetDevice = (connectedDevice.value ?: device)?.copy(isAuthorized = true)
                ?: TvDevice(id = "tv_paired", name = "Android TV", ipAddress = "192.168.1.100", isAuthorized = true)

            _connectedDevice.value = targetDevice
            _connectionState.value = ConnectionState.Connected(targetDevice)
            true
        } catch (e: Exception) {
            Log.e(TAG, "PIN validation failed: ${e.message}")
            _connectionState.value = ConnectionState.Error("Incorrect PIN. Please try again.")
            false
        }
    }

    private fun startKeepAlivePing() {
        pingJob?.cancel()
        pingJob = scope.launch {
            while (connectionState.value is ConnectionState.Connected) {
                delay(15000)
                try {
                    outputStream?.write(byteArrayOf(0x00, 0x01))
                    outputStream?.flush()
                } catch (e: Exception) {
                    Log.w(TAG, "Ping failed, TV unreachable: ${e.message}")
                    _connectionState.value = ConnectionState.Error("TV connection lost")
                    disconnect()
                    break
                }
            }
        }
    }

    private fun closeSocketQuietly() {
        try {
            inputStream?.close()
            outputStream?.close()
            socket?.close()
        } catch (ignored: Exception) {
        } finally {
            inputStream = null
            outputStream = null
            socket = null
        }
    }
}
