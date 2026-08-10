package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.DeviceDiscoveryScreen
import com.example.ui.screens.RemoteScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AwnishTvRemoteTheme
import com.example.ui.viewmodel.TvRemoteViewModel

enum class NavTab {
    REMOTE,
    DEVICES,
    SETTINGS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: TvRemoteViewModel = viewModel()
            val userPrefs by viewModel.userPreferences.collectAsState()

            AwnishTvRemoteTheme(themeMode = userPrefs.themeMode) {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: TvRemoteViewModel) {
    var selectedTab by remember { mutableStateOf(NavTab.REMOTE) }

    val connectionState by viewModel.connectionState.collectAsState()
    val activeDevice by viewModel.activeDevice.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val savedDevices by viewModel.savedDevices.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val userPrefs by viewModel.userPreferences.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = selectedTab == NavTab.REMOTE,
                    onClick = { selectedTab = NavTab.REMOTE },
                    icon = { Icon(Icons.Default.Tv, contentDescription = "Remote") },
                    label = { Text("Remote") },
                    modifier = Modifier.testTag("nav_item_remote")
                )
                NavigationBarItem(
                    selected = selectedTab == NavTab.DEVICES,
                    onClick = { selectedTab = NavTab.DEVICES },
                    icon = { Icon(Icons.Default.Router, contentDescription = "Devices") },
                    label = { Text("Devices") },
                    modifier = Modifier.testTag("nav_item_devices")
                )
                NavigationBarItem(
                    selected = selectedTab == NavTab.SETTINGS,
                    onClick = { selectedTab = NavTab.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    modifier = Modifier.testTag("nav_item_settings")
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                NavTab.REMOTE -> RemoteScreen(
                    connectionState = connectionState,
                    activeDevice = activeDevice,
                    onKeyClick = { key -> viewModel.sendRemoteKey(key) },
                    onChangeDeviceClick = { selectedTab = NavTab.DEVICES },
                    onDisconnectClick = { viewModel.disconnect() },
                    onSubmitPin = { pin -> viewModel.submitPairingPin(pin) },
                    onSendTextInput = { text -> viewModel.sendTextInput(text) }
                )

                NavTab.DEVICES -> DeviceDiscoveryScreen(
                    discoveredDevices = discoveredDevices,
                    savedDevices = savedDevices,
                    activeDevice = activeDevice,
                    connectionState = connectionState,
                    isScanning = isScanning,
                    onStartScan = { viewModel.startDeviceScan() },
                    onStopScan = { viewModel.stopDeviceScan() },
                    onConnectDevice = { device ->
                        viewModel.connectToDevice(device)
                        selectedTab = NavTab.REMOTE
                    },
                    onDisconnect = { viewModel.disconnect() },
                    onAddManualDevice = { ip, name, type ->
                        viewModel.addManualIpDevice(ip, name, type)
                    },
                    onDeleteSavedDevice = { id -> viewModel.deleteSavedDevice(id) }
                )

                NavTab.SETTINGS -> SettingsScreen(
                    preferences = userPrefs,
                    onHapticChange = { viewModel.setHapticEnabled(it) },
                    onThemeChange = { viewModel.setThemeMode(it) },
                    onButtonLayoutChange = { viewModel.setButtonLayout(it) },
                    onAutoReconnectChange = { viewModel.setAutoReconnectEnabled(it) },
                    onTestDemoModeChange = { viewModel.setTestDemoModeEnabled(it) },
                    onClearSavedDevices = { viewModel.clearAllSavedDevices() }
                )
            }
        }
    }
}

