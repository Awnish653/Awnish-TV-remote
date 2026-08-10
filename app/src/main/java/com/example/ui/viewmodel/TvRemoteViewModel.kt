package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.DeviceEntity
import com.example.data.models.ConnectionState
import com.example.data.models.RemoteKey
import com.example.data.models.TvDevice
import com.example.data.models.TvType
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.discovery.NetworkDeviceScanner
import com.example.protocol.TvRemoteManager
import com.example.util.HapticHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TvRemoteViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val deviceDao = database.deviceDao()
    val preferencesRepository = UserPreferencesRepository(application)
    val hapticHelper = HapticHelper(application)

    val scanner = NetworkDeviceScanner(application)
    val remoteManager = TvRemoteManager(deviceDao)

    val userPreferences: StateFlow<UserPreferences> = preferencesRepository.preferences

    val connectionState: StateFlow<ConnectionState> = remoteManager.connectionState
    val activeDevice: StateFlow<TvDevice?> = remoteManager.activeDevice

    val discoveredDevices: StateFlow<List<TvDevice>> = scanner.discoveredDevices
    val isScanning: StateFlow<Boolean> = scanner.isScanning

    val savedDevices: StateFlow<List<DeviceEntity>> = deviceDao.getAllSavedDevices().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            if (userPreferences.value.autoReconnectEnabled) {
                remoteManager.autoConnectLastDevice()
            }
        }
    }

    fun startDeviceScan() {
        val demoMode = userPreferences.value.testDemoModeEnabled
        scanner.startScanning(includeDemoDevices = demoMode)
    }

    fun stopDeviceScan() {
        scanner.stopScanning()
    }

    fun addManualIpDevice(ipAddress: String, name: String = "Manual TV", type: TvType = TvType.ANDROID_TV) {
        scanner.addManualDevice(ipAddress, name, type)
    }

    fun connectToDevice(device: TvDevice) {
        viewModelScope.launch {
            hapticHelper.triggerHeavy(userPreferences.value.hapticFeedbackEnabled)
            remoteManager.connectToDevice(device)
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            hapticHelper.triggerHeavy(userPreferences.value.hapticFeedbackEnabled)
            remoteManager.disconnect()
        }
    }

    fun sendRemoteKey(key: RemoteKey) {
        viewModelScope.launch {
            hapticHelper.triggerClick(userPreferences.value.hapticFeedbackEnabled)
            remoteManager.sendKey(key)
        }
    }

    fun sendTextInput(text: String) {
        viewModelScope.launch {
            hapticHelper.triggerClick(userPreferences.value.hapticFeedbackEnabled)
            remoteManager.sendTextInput(text)
        }
    }

    fun submitPairingPin(pin: String) {
        viewModelScope.launch {
            hapticHelper.triggerHeavy(userPreferences.value.hapticFeedbackEnabled)
            remoteManager.submitPairingPin(pin)
        }
    }

    fun deleteSavedDevice(id: String) {
        viewModelScope.launch {
            deviceDao.deleteDeviceById(id)
        }
    }

    fun clearAllSavedDevices() {
        viewModelScope.launch {
            deviceDao.clearAllSavedDevices()
        }
    }

    fun setHapticEnabled(enabled: Boolean) {
        preferencesRepository.setHapticFeedbackEnabled(enabled)
    }

    fun setThemeMode(mode: String) {
        preferencesRepository.setThemeMode(mode)
    }

    fun setButtonLayout(layout: String) {
        preferencesRepository.setButtonLayout(layout)
    }

    fun setAutoReconnectEnabled(enabled: Boolean) {
        preferencesRepository.setAutoReconnectEnabled(enabled)
    }

    fun setTestDemoModeEnabled(enabled: Boolean) {
        preferencesRepository.setTestDemoModeEnabled(enabled)
        // Restart scan if scanning
        if (isScanning.value) {
            scanner.stopScanning()
            scanner.startScanning(includeDemoDevices = enabled)
        }
    }

    override fun onCleared() {
        super.onCleared()
        scanner.stopScanning()
    }
}
