package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_devices")
data class DeviceEntity(
    @PrimaryKey val id: String, // IP or MAC
    val name: String,
    val ipAddress: String,
    val port: Int,
    val tvType: String,
    val isAuthorized: Boolean,
    val lastConnectedTimestamp: Long,
    val authToken: String? = null
)
