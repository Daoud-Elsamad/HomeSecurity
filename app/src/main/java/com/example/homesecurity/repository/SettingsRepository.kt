package com.example.homesecurity.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    suspend fun getGasThreshold(): Flow<Double>
    suspend fun getVibrationSensitivity(): Flow<Double>
    suspend fun getNotificationPreference(): Flow<Boolean>
    suspend fun updateGasThreshold(threshold: Double)
    suspend fun updateVibrationSensitivity(sensitivity: Double)
    suspend fun updateNotificationPreference(enabled: Boolean)
    suspend fun addUser(username: String, password: String, isAdmin: Boolean)
    suspend fun updatePermissions(viewLogs: Boolean, manageSensors: Boolean, manageUsers: Boolean)
} 