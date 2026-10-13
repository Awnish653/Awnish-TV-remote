package com.example.protocol

import android.util.Base64
import com.example.data.models.ConnectionState
import com.example.data.models.RemoteKey
import com.example.data.models.TvDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class SamsungTizenProtocol : TvControlProtocol {
    private val client = OkHttpClient()
    private var socket: WebSocket? = null
    private var device: TvDevice? = null
    private val state = kotlinx.coroutines.flow.MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: kotlinx.coroutines.flow.StateFlow<ConnectionState> = state
    private val connected = kotlinx.coroutines.flow.MutableStateFlow<TvDevice?>(null)
    override val connectedDevice: kotlinx.coroutines.flow.StateFlow<TvDevice?> = connected

    override suspend fun connect(device: TvDevice): Boolean = withContext(Dispatchers.IO) {
        disconnect()
        state.value = ConnectionState.Connecting(device.name)
        this@SamsungTizenProtocol.device = device
        val latch = CountDownLatch(1)
        val error = java.util.concurrent.atomic.AtomicBoolean(false)
        val encodedName = Base64.encodeToString("Awnish TV Remote".toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        val url = "ws://" + device.ipAddress + ":8001/api/v2/channels/samsung.remote.control?name=" + URLEncoder.encode(encodedName, "UTF-8")
        socket = client.newWebSocket(Request.Builder().url(url).build(), object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                latch.countDown()
                val d = this@SamsungTizenProtocol.device ?: return
                connected.value = d.copy(isAuthorized = true)
                state.value = ConnectionState.Connected(d.copy(isAuthorized = true))
            }
            override fun onMessage(ws: WebSocket, text: String) {
                if (text.contains("ms.channel.unauthorized")) {
                    val d = this@SamsungTizenProtocol.device ?: return
                    state.value = ConnectionState.PairingRequired(d.name, "Accept the remote-control permission prompt on the Samsung TV.")
                }
            }
            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                error.set(true)
                latch.countDown()
                connected.value = null
                state.value = ConnectionState.Error("Samsung TV connection failed: " + (t.message ?: "check IP, Wi-Fi and remote-control permission"))
            }
            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                connected.value = null
                state.value = ConnectionState.Disconnected
            }
        })
        if (!latch.await(5, TimeUnit.SECONDS) || error.get()) {
            socket?.cancel(); socket = null
            if (!error.get()) state.value = ConnectionState.Error("No response from Samsung TV on port 8001. Check its IP and network-control settings.")
            return@withContext false
        }
        true
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        socket?.close(1000, "Disconnect"); socket = null; device = null
        connected.value = null; state.value = ConnectionState.Disconnected
    }

    private fun keyName(key: RemoteKey): String? = when (key) {
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
        RemoteKey.DIGIT_0 -> "KEY_0"
        RemoteKey.DIGIT_1 -> "KEY_1"
        RemoteKey.DIGIT_2 -> "KEY_2"
        RemoteKey.DIGIT_3 -> "KEY_3"
        RemoteKey.DIGIT_4 -> "KEY_4"
        RemoteKey.DIGIT_5 -> "KEY_5"
        RemoteKey.DIGIT_6 -> "KEY_6"
        RemoteKey.DIGIT_7 -> "KEY_7"
        RemoteKey.DIGIT_8 -> "KEY_8"
        RemoteKey.DIGIT_9 -> "KEY_9"
        RemoteKey.KEYBOARD_INPUT -> null
    }

    override suspend fun sendKey(key: RemoteKey): Boolean = withContext(Dispatchers.IO) {
        val ws = socket ?: return@withContext false
        val name = keyName(key) ?: return@withContext false
        ws.send(JSONObject().put("method", "ms.remote.control").put("params",
            JSONObject().put("Cmd", "Click").put("DataOfCmd", name).put("Option", "false").put("TypeOfRemote", "SendRemoteKey")
        ).toString())
    }

    override suspend fun sendTextInput(text: String): Boolean = withContext(Dispatchers.IO) {
        var allSent = true
        for (ch in text) {
            val name = if (ch == ' ') "KEY_SPACE" else if (ch.isLetterOrDigit()) "KEY_" + ch.uppercaseChar() else null
            val ws = socket ?: return@withContext false
            if (name == null || !ws.send(JSONObject().put("method", "ms.remote.control").put("params",
                JSONObject().put("Cmd", "Click").put("DataOfCmd", name).put("Option", "false").put("TypeOfRemote", "SendRemoteKey")
            ).toString())) allSent = false
        }
        allSent
    }

    override suspend fun submitPairingPin(pin: String): Boolean = state.value is ConnectionState.Connected
}