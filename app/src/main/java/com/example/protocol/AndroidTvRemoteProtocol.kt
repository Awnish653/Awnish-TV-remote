package com.example.protocol

import android.content.Context
import android.util.Log
import com.example.data.models.ConnectionState
import com.example.data.models.RemoteKey
import com.example.data.models.TvDevice
import dev.haris.atvremote.protocol.ClientIdentity
import dev.haris.atvremote.protocol.PairingException
import dev.haris.atvremote.protocol.PairingSession
import dev.haris.atvremote.protocol.RemoteClient
import java.io.File
import java.security.KeyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Android TV / Google TV Remote v2 implementation.
 *
 * Pairing is a real TLS + protobuf handshake on port 6467. The TV only displays its
 * six-character hexadecimal code after the pairing request/options/configuration exchange.
 * Remote commands use the authenticated TLS session on port 6466.
 */
class AndroidTvRemoteProtocol(context: Context) : TvControlProtocol {
    private val appContext = context.applicationContext
    private val tag = "AndroidTvRemoteProtocol"
    private val identityPassword = "atvremote".toCharArray()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _connectedDevice = MutableStateFlow<TvDevice?>(null)
    override val connectedDevice: StateFlow<TvDevice?> = _connectedDevice.asStateFlow()

    private var keyStore: KeyStore? = null
    private var pairingSession: PairingSession? = null
    private var remoteClient: RemoteClient? = null
    private var pendingDevice: TvDevice? = null

    override suspend fun connect(device: TvDevice): Boolean = withContext(Dispatchers.IO) {
        _connectionState.value = ConnectionState.Connecting(device.name)
        pendingDevice = device
        closePairingSession()
        closeRemoteClient()
        _connectedDevice.value = null

        try {
            val identity = getIdentity()
            if (device.isAuthorized) {
                try {
                    val client = createRemoteClient(device, identity)
                    client.connect()
                    remoteClient = client
                    val authorized = device.copy(isAuthorized = true)
                    _connectedDevice.value = authorized
                    _connectionState.value = ConnectionState.Connected(authorized)
                    return@withContext true
                } catch (e: Exception) {
                    Log.w(tag, "Saved pairing did not reconnect; starting pairing again: ${e.message}")
                    closeRemoteClient()
                }
            }

            val session = PairingSession(
                host = device.ipAddress,
                keyStore = identity,
                password = identityPassword,
                clientName = "Awnish TV Remote"
            )
            pairingSession = session
            session.begin()
            _connectionState.value = ConnectionState.PairingRequired(
                deviceName = device.name,
                message = "A 6-character hexadecimal pairing code should now be visible on your TV. Enter it exactly as shown."
            )
            false
        } catch (e: Exception) {
            closePairingSession()
            closeRemoteClient()
            val message = e.message ?: "Could not start Android TV pairing"
            Log.e(tag, "Pairing handshake failed for ${device.ipAddress}", e)
            _connectionState.value = ConnectionState.Error(
                "Could not start pairing with ${device.name}: $message. Check that Android TV Remote Service is enabled and both devices use the same Wi-Fi."
            )
            false
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        closePairingSession()
        closeRemoteClient()
        pendingDevice = null
        _connectedDevice.value = null
        _connectionState.value = ConnectionState.Disconnected
    }

    override suspend fun sendKey(key: RemoteKey): Boolean = withContext(Dispatchers.IO) {
        val client = remoteClient ?: return@withContext false
        if (_connectionState.value !is ConnectionState.Connected || key.keyCode == 0) return@withContext false
        return@withContext try {
            client.sendKey(key.keyCode)
            true
        } catch (e: Exception) {
            Log.e(tag, "Could not send ${key.name}", e)
            _connectionState.value = ConnectionState.Error("Remote command failed: ${e.message ?: "connection lost"}")
            closeRemoteClient()
            false
        }
    }

    override suspend fun sendTextInput(text: String): Boolean = withContext(Dispatchers.IO) {
        val client = remoteClient ?: return@withContext false
        if (_connectionState.value !is ConnectionState.Connected || text.isBlank()) return@withContext false
        return@withContext try {
            client.sendText(text)
            true
        } catch (e: Exception) {
            Log.e(tag, "Could not send text input", e)
            false
        }
    }

    override suspend fun submitPairingPin(pin: String): Boolean = withContext(Dispatchers.IO) {
        val device = pendingDevice
        val session = pairingSession
        if (device == null || session == null) {
            _connectionState.value = ConnectionState.Error("Pairing session expired. Select the TV and try again.")
            return@withContext false
        }

        val code = pin.trim().uppercase()
        if (!code.matches(Regex("[0-9A-F]{6}"))) {
            _connectionState.value = ConnectionState.PairingRequired(
                device.name,
                "Enter all 6 characters shown on the TV (digits and A–F letters)."
            )
            return@withContext false
        }

        try {
            session.complete(code)
            closePairingSession()
            val client = createRemoteClient(device, getIdentity())
            client.connect()
            remoteClient = client
            val authorized = device.copy(isAuthorized = true)
            _connectedDevice.value = authorized
            _connectionState.value = ConnectionState.Connected(authorized)
            true
        } catch (e: Exception) {
            Log.e(tag, "Android TV pairing verification failed", e)
            closePairingSession()
            closeRemoteClient()
            _connectionState.value = ConnectionState.Error(
                "TV pairing failed: ${e.message ?: "code rejected or remote service unavailable"}. Select the TV and start pairing again."
            )
            false
        }
    }

    private fun getIdentity(): KeyStore {
        keyStore?.let { return it }
        val file = File(appContext.filesDir, "identity/client.p12")
        return ClientIdentity.loadOrCreate(file, identityPassword).also { keyStore = it }
    }

    private fun createRemoteClient(device: TvDevice, identity: KeyStore) = RemoteClient(
        host = device.ipAddress,
        keyStore = identity,
        password = identityPassword,
        deviceModel = android.os.Build.MODEL ?: "Android phone",
        deviceVendor = "Awnish TV Remote"
    ).also { client ->
        client.onLog = { message -> Log.d(tag, message) }
    }

    private fun closePairingSession() {
        runCatching { pairingSession?.close() }
        pairingSession = null
    }

    private fun closeRemoteClient() {
        runCatching { remoteClient?.close() }
        remoteClient = null
    }
}
