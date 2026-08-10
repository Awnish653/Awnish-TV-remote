package com.example.data.models

enum class TvType(val displayName: String) {
    ANDROID_TV("Android TV / Google TV"),
    SAMSUNG_TIZEN("Samsung Smart TV"),
    LG_WEBOS("LG webOS TV"),
    GENERIC_SMART_TV("Generic Smart TV")
}

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    PAIRING_REQUIRED,
    CONNECTED,
    ERROR
}

data class TvDevice(
    val id: String, // IP Address or MAC/UUID
    val name: String,
    val ipAddress: String,
    val port: Int = 6466,
    val type: TvType = TvType.ANDROID_TV,
    val isAuthorized: Boolean = false,
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val lastConnectedTimestamp: Long = 0L,
    val modelName: String? = null
)
