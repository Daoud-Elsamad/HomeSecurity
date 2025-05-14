package com.example.homesecurity.repository

import com.example.homesecurity.models.User
import com.example.homesecurity.models.UserPermissions
import com.example.homesecurity.models.UserRole
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
    
    // New methods
    suspend fun addUserWithRole(username: String, password: String, role: UserRole): Result<User>
    suspend fun updateUserPermissions(userId: String, permissions: UserPermissions): Result<Unit>
    suspend fun deleteUser(userId: String): Result<Unit>
    suspend fun updateUserRole(userId: String, role: UserRole): Result<Unit>
    suspend fun getUserById(userId: String): Result<User>
} 