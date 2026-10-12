package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Slider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.models.ConnectionState
import com.example.data.models.RemoteKey
import com.example.data.models.TvDevice
import com.example.ui.components.ConnectionStatusBar
import com.example.ui.components.DPadControl
import com.example.ui.components.PairingDialog
import com.example.ui.components.RemoteButton
import com.example.ui.components.RemoteButtonStyle
import com.example.ui.components.RockerButton
import com.example.ui.components.TextInputDialog

@Composable
fun RemoteScreen(
    connectionState: ConnectionState,
    activeDevice: TvDevice?,
    onKeyClick: (RemoteKey) -> Unit,
    onChangeDeviceClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onSubmitPin: (String) -> Unit,
    onSendTextInput: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var touchSensitivity by remember { mutableStateOf(2f) }
    var showShortcutDialog by remember { mutableStateOf(false) }
    var shortcutName by remember { mutableStateOf("") }
    var shortcutUrl by remember { mutableStateOf("") }
    val shortcutPrefs = remember(context) {
        context.getSharedPreferences("tv_remote_shortcuts", android.content.Context.MODE_PRIVATE)
    }
    var savedShortcuts by remember {
        mutableStateOf(shortcutPrefs.getString("items", "")?.split("\\n")?.filter { it.contains("|") } ?: emptyList())
    }
    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) onSendTextInput(spoken)
        }
    }
    fun startVoiceSearch() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to search on your TV")
        }
        runCatching { voiceLauncher.launch(intent) }
    }
    var showTextInputDialog by remember { mutableStateOf(false) }
    var showChannelPad by remember { mutableStateOf(false) }

    if (connectionState is ConnectionState.PairingRequired) {
        PairingDialog(
            deviceName = connectionState.deviceName,
            message = connectionState.message,
            onSubmitPin = onSubmitPin,
            onDismiss = { /* Keep dialog active until PIN submitted or dismissed */ }
        )
    }

    if (showTextInputDialog) {
        TextInputDialog(
            deviceName = activeDevice?.name ?: "Smart TV",
            onSendText = onSendTextInput,
            onDismiss = { showTextInputDialog = false }
        )
    }

    if (showChannelPad) {
        AlertDialog(
            onDismissRequest = { showChannelPad = false },
            title = { androidx.compose.material3.Text("Channel Number Pad") },
            text = {
                Column {
                    listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("", "0", "")).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            row.forEach { digit ->
                                if (digit.isBlank()) {
                                    Spacer(modifier = Modifier.weight(1f).height(44.dp))
                                } else {
                                    Button(
                                        onClick = { onKeyClick(RemoteKey.valueOf("DIGIT_$digit")) },
                                        modifier = Modifier.weight(1f).height(44.dp)
                                    ) { androidx.compose.material3.Text(digit) }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showChannelPad = false }) {
                    androidx.compose.material3.Text("Done")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
            .testTag("remote_screen")
    ) {
        // Device Status Header
        ConnectionStatusBar(
            connectionState = connectionState,
            activeDevice = activeDevice,
            onChangeDeviceClick = onChangeDeviceClick,
            onDisconnectClick = onDisconnectClick
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Power & Top Quick Action Row
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            RemoteButton(
                icon = Icons.Default.PowerSettingsNew,
                label = "Power",
                onClick = { onKeyClick(RemoteKey.POWER) },
                style = RemoteButtonStyle.POWER,
                testTag = "remote_btn_power"
            )

            RemoteButton(
                icon = Icons.AutoMirrored.Filled.VolumeOff,
                label = "Mute",
                onClick = { onKeyClick(RemoteKey.VOLUME_MUTE) },
                testTag = "remote_btn_mute"
            )

            RemoteButton(
                icon = Icons.Default.Mic,
                label = "Search",
                onClick = { startVoiceSearch() },
                style = RemoteButtonStyle.ACCENT,
                testTag = "remote_btn_voice"
            )

            RemoteButton(
                icon = Icons.Default.Keyboard,
                label = "Keyboard",
                onClick = { showTextInputDialog = true },
                testTag = "remote_btn_keyboard"
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Voice input uses the phone's speech recognizer, then sends recognized text
        // through the TV's supported text-input channel (audio is not streamed to the TV).
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = { startVoiceSearch() }, modifier = Modifier.weight(1f)) {
                androidx.compose.material3.Icon(Icons.Default.Mic, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                androidx.compose.material3.Text("Speak to TV")
            }
            OutlinedButton(onClick = { showShortcutDialog = true }, modifier = Modifier.weight(1f)) {
                androidx.compose.material3.Text("Web shortcuts")
            }
        }

        if (showShortcutDialog) {
            AlertDialog(
                onDismissRequest = { showShortcutDialog = false },
                title = { androidx.compose.material3.Text("Add website shortcut") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = shortcutName, onValueChange = { shortcutName = it }, label = { androidx.compose.material3.Text("Shortcut name") }, singleLine = true)
                        OutlinedTextField(value = shortcutUrl, onValueChange = { shortcutUrl = it }, label = { androidx.compose.material3.Text("Website URL") }, singleLine = true, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Uri))
                        androidx.compose.material3.Text("Opening a URL on the TV depends on its browser/protocol support. Saved shortcuts are sent as text to the connected TV.")
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val name = shortcutName.trim()
                        var url = shortcutUrl.trim()
                        if (name.isNotEmpty() && url.isNotEmpty()) {
                            if (!url.startsWith("http://") && !url.startsWith("https://")) url = "https://$url"
                            val entry = "$name|$url"
                            savedShortcuts = (savedShortcuts.filterNot { it.substringBefore("|").equals(name, true) } + entry)
                            shortcutPrefs.edit().putString("items", savedShortcuts.joinToString("\\n")).apply()
                            shortcutName = ""
                            shortcutUrl = ""
                            showShortcutDialog = false
                        }
                    }) { androidx.compose.material3.Text("Save") }
                },
                dismissButton = { TextButton(onClick = { showShortcutDialog = false }) { androidx.compose.material3.Text("Cancel") } }
            )
        }

        if (savedShortcuts.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                androidx.compose.material3.Text("Saved website shortcuts", style = MaterialTheme.typography.titleSmall)
                savedShortcuts.forEach { item ->
                    val name = item.substringBefore("|")
                    val url = item.substringAfter("|")
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            androidx.compose.material3.Text(name, style = MaterialTheme.typography.bodyMedium)
                            androidx.compose.material3.Text(url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { onSendTextInput(url) }) { androidx.compose.material3.Text("Send URL") }
                        TextButton(onClick = {
                            savedShortcuts = savedShortcuts.filterNot { it == item }
                            shortcutPrefs.edit().putString("items", savedShortcuts.joinToString("\\n")).apply()
                        }) { androidx.compose.material3.Text("Delete") }
                    }
                }
            }
        }

        // Touchpad sends directional key steps; cursor mode is only possible on TVs exposing it.
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                androidx.compose.material3.Text("Touchpad", style = MaterialTheme.typography.titleMedium)
                androidx.compose.material3.Text("Speed ${touchSensitivity.toInt()}/5", style = MaterialTheme.typography.bodySmall)
            }
            Slider(value = touchSensitivity, onValueChange = { touchSensitivity = it }, valueRange = 1f..5f, steps = 3)
            Box(
                modifier = Modifier.fillMaxWidth().height(116.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp))
                    .pointerInput(touchSensitivity, onKeyClick) {
                        var dx = 0f
                        var dy = 0f
                        detectDragGestures(
                            onDragStart = { dx = 0f; dy = 0f },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dx += dragAmount.x
                                dy += dragAmount.y
                                val threshold = (42f / touchSensitivity)
                                if (kotlin.math.abs(dx) >= threshold || kotlin.math.abs(dy) >= threshold) {
                                    val steps = touchSensitivity.toInt()
                                    val key = if (kotlin.math.abs(dx) >= kotlin.math.abs(dy)) {
                                        if (dx > 0) RemoteKey.DPAD_RIGHT else RemoteKey.DPAD_LEFT
                                    } else {
                                        if (dy > 0) RemoteKey.DPAD_DOWN else RemoteKey.DPAD_UP
                                    }
                                    repeat(steps) { onKeyClick(key) }
                                    dx = 0f
                                    dy = 0f
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Text("Swipe to navigate · tap OK below", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { onKeyClick(RemoteKey.SELECT) }, modifier = Modifier.fillMaxWidth()) {
                androidx.compose.material3.Text("Touchpad Select / OK")
            }
        }

        // Large One-Handed D-Pad Controller
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            DPadControl(
                onKeyClick = onKeyClick,
                size = 250.dp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { showChannelPad = true },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
        ) {
            androidx.compose.material3.Icon(Icons.Default.Dialpad, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            androidx.compose.material3.Text("Channel Number Pad")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Navigation Row: Back, Home, Menu
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            RemoteButton(
                icon = Icons.Default.ArrowBack,
                label = "Back",
                onClick = { onKeyClick(RemoteKey.BACK) },
                size = 64.dp,
                testTag = "remote_btn_back"
            )

            RemoteButton(
                icon = Icons.Default.Home,
                label = "Home",
                onClick = { onKeyClick(RemoteKey.HOME) },
                style = RemoteButtonStyle.ACCENT,
                size = 68.dp,
                testTag = "remote_btn_home"
            )

            RemoteButton(
                icon = Icons.Default.Menu,
                label = "Menu",
                onClick = { onKeyClick(RemoteKey.MENU) },
                size = 64.dp,
                testTag = "remote_btn_menu"
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Rocker Control Row: Volume & Channel + Play/Pause
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            // Volume Rocker
            RockerButton(
                title = "VOL",
                plusLabel = "Vol +",
                minusLabel = "Vol -",
                plusIcon = Icons.AutoMirrored.Filled.VolumeUp,
                minusIcon = Icons.AutoMirrored.Filled.VolumeDown,
                onPlusClick = { onKeyClick(RemoteKey.VOLUME_UP) },
                onMinusClick = { onKeyClick(RemoteKey.VOLUME_DOWN) }
            )

            // Play / Pause Media Button
            RemoteButton(
                icon = Icons.Default.PlayArrow,
                label = "Play / Pause",
                onClick = { onKeyClick(RemoteKey.PLAY_PAUSE) },
                style = RemoteButtonStyle.ACCENT,
                size = 72.dp,
                testTag = "remote_btn_play_pause"
            )

            // Channel Rocker
            RockerButton(
                title = "CH",
                plusLabel = "Ch +",
                minusLabel = "Ch -",
                plusIcon = Icons.Default.Add,
                minusIcon = Icons.Default.Remove,
                onPlusClick = { onKeyClick(RemoteKey.CHANNEL_UP) },
                onMinusClick = { onKeyClick(RemoteKey.CHANNEL_DOWN) }
            )
        }
    }
}
