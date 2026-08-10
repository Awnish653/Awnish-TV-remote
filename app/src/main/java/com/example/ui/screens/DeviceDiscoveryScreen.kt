package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.DeviceEntity
import com.example.data.models.ConnectionState
import com.example.data.models.TvDevice
import com.example.data.models.TvType
import com.example.ui.theme.TvConnectedGreen
import com.example.ui.theme.TvErrorRed
import com.example.ui.theme.TvPrimaryCyan

@Composable
fun DeviceDiscoveryScreen(
    discoveredDevices: List<TvDevice>,
    savedDevices: List<DeviceEntity>,
    activeDevice: TvDevice?,
    connectionState: ConnectionState,
    isScanning: Boolean,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onConnectDevice: (TvDevice) -> Unit,
    onDisconnect: () -> Unit,
    onAddManualDevice: (String, String, TvType) -> Unit,
    onDeleteSavedDevice: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showManualIpDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        onStartScan()
    }

    if (showManualIpDialog) {
        ManualIpDialog(
            onAdd = { ip, name, type ->
                onAddManualDevice(ip, name, type)
                showManualIpDialog = false
            },
            onDismiss = { showManualIpDialog = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("device_discovery_screen")
    ) {
        // Wi-Fi Banner Header
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(TvPrimaryCyan.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = "Wi-Fi Network",
                            tint = TvPrimaryCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Wi-Fi TV Discovery",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isScanning) "Searching local Wi-Fi network..." else "Phone & TV must be on same Wi-Fi",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row {
                    IconButton(
                        onClick = {
                            if (isScanning) onStopScan() else onStartScan()
                        },
                        modifier = Modifier.testTag("rescan_button")
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Rescan",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Actions Row
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Discovered TVs (${discoveredDevices.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            TextButton(
                onClick = { showManualIpDialog = true },
                modifier = Modifier.testTag("manual_ip_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add by IP")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (discoveredDevices.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Router,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (isScanning) "Scanning local Wi-Fi network..." else "No TVs found on local Wi-Fi",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Ensure your TV is turned on and connected to the same Wi-Fi network as your Android phone. You can also tap 'Add by IP' above.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onStartScan,
                                modifier = Modifier.testTag("scan_again_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Scan Network Again")
                            }
                        }
                    }
                }
            } else {
                items(discoveredDevices, key = { it.id }) { device ->
                    TvDeviceCard(
                        device = device,
                        isActive = activeDevice?.id == device.id,
                        connectionState = if (activeDevice?.id == device.id) connectionState else ConnectionState.Disconnected,
                        onConnectClick = { onConnectDevice(device) },
                        onDisconnectClick = onDisconnect
                    )
                }
            }

            if (savedDevices.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Previously Saved TVs (${savedDevices.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(savedDevices, key = { "saved_${it.id}" }) { savedEntity ->
                    val savedTvDevice = TvDevice(
                        id = savedEntity.id,
                        name = savedEntity.name,
                        ipAddress = savedEntity.ipAddress,
                        port = savedEntity.port,
                        type = try { TvType.valueOf(savedEntity.tvType) } catch (e: Exception) { TvType.ANDROID_TV },
                        isAuthorized = savedEntity.isAuthorized,
                        lastConnectedTimestamp = savedEntity.lastConnectedTimestamp
                    )

                    TvDeviceCard(
                        device = savedTvDevice,
                        isActive = activeDevice?.id == savedTvDevice.id,
                        connectionState = if (activeDevice?.id == savedTvDevice.id) connectionState else ConnectionState.Disconnected,
                        onConnectClick = { onConnectDevice(savedTvDevice) },
                        onDisconnectClick = onDisconnect,
                        onDeleteClick = { onDeleteSavedDevice(savedEntity.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun TvDeviceCard(
    device: TvDevice,
    isActive: Boolean,
    connectionState: ConnectionState,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null
) {
    val isConnected = isActive && connectionState is ConnectionState.Connected

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tv_card_${device.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isConnected) TvConnectedGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = "TV Icon",
                        tint = if (isConnected) TvConnectedGreen else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = device.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (device.isAuthorized) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Authorized",
                                tint = TvConnectedGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Text(
                        text = "${device.type.displayName} • ${device.ipAddress}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isConnected) {
                    OutlinedButton(
                        onClick = onDisconnectClick,
                        modifier = Modifier.testTag("disconnect_button_${device.id}")
                    ) {
                        Text("Disconnect", color = TvErrorRed)
                    }
                } else {
                    Button(
                        onClick = onConnectClick,
                        modifier = Modifier.testTag("connect_button_${device.id}")
                    ) {
                        Text("Connect")
                    }
                }

                if (onDeleteClick != null) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.testTag("delete_saved_${device.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ManualIpDialog(
    onAdd: (String, String, TvType) -> Unit,
    onDismiss: () -> Unit
) {
    var ipText by remember { mutableStateOf("") }
    var nameText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TvType.ANDROID_TV) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add TV by IP Address", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = ipText,
                    onValueChange = { ipText = it },
                    label = { Text("TV IP Address") },
                    placeholder = { Text("e.g. 192.168.1.105") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("TV Name (Optional)") },
                    placeholder = { Text("e.g. Living Room TV") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (ipText.isNotBlank()) {
                        onAdd(ipText.trim(), nameText.ifBlank { "Manual TV ($ipText)" }, selectedType)
                    }
                },
                enabled = ipText.isNotBlank()
            ) {
                Text("Add & Scan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
