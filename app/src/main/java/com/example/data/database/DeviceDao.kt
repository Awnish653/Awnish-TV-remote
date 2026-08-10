package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM saved_devices ORDER BY lastConnectedTimestamp DESC")
    fun getAllSavedDevices(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM saved_devices WHERE isAuthorized = 1 ORDER BY lastConnectedTimestamp DESC LIMIT 1")
    suspend fun getLastConnectedDevice(): DeviceEntity?

    @Query("SELECT * FROM saved_devices WHERE id = :id LIMIT 1")
    suspend fun getDeviceById(id: String): DeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDevice(device: DeviceEntity)

    @Query("DELETE FROM saved_devices WHERE id = :id")
    suspend fun deleteDeviceById(id: String)

    @Query("DELETE FROM saved_devices")
    suspend fun clearAllSavedDevices()
}
