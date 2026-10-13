package com.example.ui.screens

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
import java.util.Locale

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
    var showTextInputDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val voiceInputLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spokenText = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (result.resultCode == android.app.Activity.RESULT_OK && !spokenText.isNullOrBlank()) {
            onSendTextInput(spokenText)
        }
    }

    fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to enter text on your TV")
        }
        try {
            voiceInputLauncher.launch(intent)
        } catch (_: Exception) {
            // Speech recognition is not installed on every Android device.
        }
    }

    if (connectionState is ConnectionState.PairingRequired) {
        PairingDialog(
            deviceName = connectionState.deviceName,
            message = connectionState.message,
            onSubmitPin = onSubmitPin,
            onDismiss = { }
        )
    }

    if (showTextInputDialog) {
        TextInputDialog(
            deviceName = activeDevice?.name ?: "Smart TV",
            onSendText = onSendTextInput,
            onDismiss = { showTextInputDialog = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
            .testTag("remote_screen")
    ) {
        ConnectionStatusBar(
            connectionState = connectionState,
            activeDevice = activeDevice,
            onChangeDeviceClick = onChangeDeviceClick,
            onDisconnectClick = onDisconnectClick
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
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
                label = "Voice",
                onClick = { startVoiceInput() },
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

        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
            DPadControl(onKeyClick = onKeyClick, size = 250.dp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
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

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
        ) {
            RockerButton(
                title = "VOL",
                plusLabel = "Vol +",
                minusLabel = "Vol -",
                plusIcon = Icons.AutoMirrored.Filled.VolumeUp,
                minusIcon = Icons.AutoMirrored.Filled.VolumeDown,
                onPlusClick = { onKeyClick(RemoteKey.VOLUME_UP) },
                onMinusClick = { onKeyClick(RemoteKey.VOLUME_DOWN) }
            )
            RemoteButton(
                icon = Icons.Default.PlayArrow,
                label = "Play / Pause",
                onClick = { onKeyClick(RemoteKey.PLAY_PAUSE) },
                style = RemoteButtonStyle.ACCENT,
                size = 72.dp,
                testTag = "remote_btn_play_pause"
            )
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

        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp)) {
            Text("Channel Number Pad", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Sends number-key events; channel tuning depends on the TV and the app currently open.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            val rows = listOf(
                listOf(RemoteKey.DIGIT_1, RemoteKey.DIGIT_2, RemoteKey.DIGIT_3),
                listOf(RemoteKey.DIGIT_4, RemoteKey.DIGIT_5, RemoteKey.DIGIT_6),
                listOf(RemoteKey.DIGIT_7, RemoteKey.DIGIT_8, RemoteKey.DIGIT_9),
                listOf(RemoteKey.BACK, RemoteKey.DIGIT_0, RemoteKey.SELECT)
            )
            rows.forEach { rowKeys ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowKeys.forEach { key ->
                        Button(
                            onClick = { onKeyClick(key) },
                            modifier = Modifier.weight(1f).height(52.dp)
                        ) {
                            Text(
                                when (key) {
                                    RemoteKey.BACK -> "Back"
                                    RemoteKey.SELECT -> "OK"
                                    else -> key.label
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
