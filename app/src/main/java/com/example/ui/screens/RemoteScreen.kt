package com.example.ui.screens

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
import androidx.compose.material3.MaterialTheme
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
    var showTextInputDialog by remember { mutableStateOf(false) }

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
                onClick = { onKeyClick(RemoteKey.VOICE_SEARCH) },
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
