package com.example.data.models

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Scanning : ConnectionState()
    data class Connecting(val deviceName: String) : ConnectionState()
    data class PairingRequired(val deviceName: String, val message: String = "Enter the 4 or 6-digit PIN displayed on your TV") : ConnectionState()
    data class Connected(val device: TvDevice) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
