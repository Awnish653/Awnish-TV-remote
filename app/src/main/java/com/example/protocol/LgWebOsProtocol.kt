package com.example.protocol

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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class LgWebOsProtocol : TvControlProtocol {
    private val client = OkHttpClient()
    private var socket: WebSocket? = null
    private var device: TvDevice? = null
    private var nextId = 1
    private val registered = AtomicBoolean(false)
    private val state = kotlinx.coroutines.flow.MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: kotlinx.coroutines.flow.StateFlow<ConnectionState> = state
    private val connected = kotlinx.coroutines.flow.MutableStateFlow<TvDevice?>(null)
    override val connectedDevice: kotlinx.coroutines.flow.StateFlow<TvDevice?> = connected

    override suspend fun connect(device: TvDevice): Boolean = withContext(Dispatchers.IO) {
        disconnect()
        state.value = ConnectionState.Connecting(device.name)
        this@LgWebOsProtocol.device = device
        registered.set(false)
        val latch = CountDownLatch(1)
        val failed = AtomicBoolean(false)
        socket = client.newWebSocket(Request.Builder().url("ws://" + device.ipAddress + ":3000").build(), object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                latch.countDown()
                val manifest = JSONObject().put("manifestVersion", 1).put("appVersion", "1.0")
                    .put("permissions", org.json.JSONArray(listOf("CONTROL_AUDIO","CONTROL_INPUT_TEXT","CONTROL_MOUSE_AND_KEYBOARD","CONTROL_POWER","CONTROL_INPUT_MEDIA_PLAYBACK")))
                ws.send(JSONObject().put("type", "register").put("id", "register_0").put("payload", JSONObject().put("manifest", manifest)).toString())
            }
            override fun onMessage(ws: WebSocket, text: String) {
                runCatching {
                    val json = JSONObject(text)
                    if (json.optString("type") == "registered") {
                        registered.set(true)
                        val d = this@LgWebOsProtocol.device ?: return
                        connected.value = d.copy(isAuthorized = true)
                        state.value = ConnectionState.Connected(d.copy(isAuthorized = true))
                    }
                    if (json.optString("type") == "response" && json.optString("id") == "register_0" && json.optString("error").isNotBlank()) {
                        val d = this@LgWebOsProtocol.device ?: return
                        state.value = ConnectionState.PairingRequired(d.name, "Accept the remote-control permission prompt on the LG TV.")
                    }
                }
            }
            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                failed.set(true); latch.countDown(); connected.value = null
                state.value = ConnectionState.Error("LG webOS connection failed: " + (t.message ?: "check IP and TV network-control settings"))
            }
            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                connected.value = null; state.value = ConnectionState.Disconnected
            }
        })
        if (!latch.await(5, TimeUnit.SECONDS) || failed.get()) {
            socket?.cancel(); socket = null
            if (!failed.get()) state.value = ConnectionState.Error("No response from LG TV on port 3000. Check its IP and ensure the TV is awake.")
            return@withContext false
        }
        val deadline = System.currentTimeMillis() + 2500
        while (!registered.get() && !failed.get() && System.currentTimeMillis() < deadline) Thread.sleep(50)
        if (!registered.get()) {
            val d = this@LgWebOsProtocol.device ?: return@withContext false
            state.value = ConnectionState.PairingRequired(d.name, "Accept the remote-control permission prompt on the LG TV.")
            return@withContext false
        }
        true
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        socket?.close(1000, "Disconnect"); socket = null; device = null
        registered.set(false); connected.value = null; state.value = ConnectionState.Disconnected
    }

    private fun send(uri: String, payload: JSONObject = JSONObject()): Boolean {
        val ws = socket ?: return false
        if (!registered.get()) return false
        return ws.send(JSONObject().put("id", "req_" + nextId++).put("type", "request").put("uri", uri).put("payload", payload).toString())
    }

    override suspend fun sendKey(key: RemoteKey): Boolean = withContext(Dispatchers.IO) {
        val name = when (key) {
            RemoteKey.POWER -> "POWER"
            RemoteKey.DPAD_UP -> "UP"
            RemoteKey.DPAD_DOWN -> "DOWN"
            RemoteKey.DPAD_LEFT -> "LEFT"
            RemoteKey.DPAD_RIGHT -> "RIGHT"
            RemoteKey.SELECT -> "ENTER"
            RemoteKey.BACK -> "BACK"
            RemoteKey.HOME -> "HOME"
            RemoteKey.MENU -> "MENU"
            RemoteKey.VOLUME_UP -> "VOLUMEUP"
            RemoteKey.VOLUME_DOWN -> "VOLUMEDOWN"
            RemoteKey.VOLUME_MUTE -> "MUTE"
            RemoteKey.CHANNEL_UP -> "CHANNELUP"
            RemoteKey.CHANNEL_DOWN -> "CHANNELDOWN"
            RemoteKey.PLAY_PAUSE -> "PLAY"
            RemoteKey.VOICE_SEARCH -> "VOICE"
            RemoteKey.DIGIT_0 -> "0"
            RemoteKey.DIGIT_1 -> "1"
            RemoteKey.DIGIT_2 -> "2"
            RemoteKey.DIGIT_3 -> "3"
            RemoteKey.DIGIT_4 -> "4"
            RemoteKey.DIGIT_5 -> "5"
            RemoteKey.DIGIT_6 -> "6"
            RemoteKey.DIGIT_7 -> "7"
            RemoteKey.DIGIT_8 -> "8"
            RemoteKey.DIGIT_9 -> "9"
            RemoteKey.KEYBOARD_INPUT -> return@withContext false
        }
        send("ssap://com.webos.service.networkinput/sendButton", JSONObject().put("key", name))
    }

    override suspend fun sendTextInput(text: String): Boolean = withContext(Dispatchers.IO) {
        send("ssap://com.webos.service.networkinput/insertText", JSONObject().put("text", text).put("replace", 0))
    }

    override suspend fun submitPairingPin(pin: String): Boolean = registered.get()
}