package com.example.homesecurity.repository

import com.example.homesecurity.models.User
import com.example.homesecurity.models.UserPermissions
import com.example.homesecurity.models.UserRole
import com.example.homesecurity.viewmodels.DashboardViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val authRepository: AuthRepository
) : SettingsRepository {
    private val gasThreshold = MutableStateFlow(DashboardViewModel.GAS_THRESHOLD)
    private val vibrationSensitivity = MutableStateFlow(DashboardViewModel.VIBRATION_THRESHOLD)
    private val notificationEnabled = MutableStateFlow(true)

    override suspend fun getGasThreshold(): Flow<Double> = gasThreshold
    override suspend fun getVibrationSensitivity(): Flow<Double> = vibrationSensitivity
    override suspend fun getNotificationPreference(): Flow<Boolean> = notificationEnabled

    override suspend fun updateGasThreshold(threshold: Double) {
        gasThreshold.value = threshold
    }

    override suspend fun updateVibrationSensitivity(sensitivity: Double) {
        vibrationSensitivity.value = sensitivity
    }

    override suspend fun updateNotificationPreference(enabled: Boolean) {
        notificationEnabled.value = enabled
    }

    override suspend fun addUser(username: String, password: String, isAdmin: Boolean) {
        authRepository.createUser(username, password, isAdmin)
    }

    override suspend fun updatePermissions(viewLogs: Boolean, manageSensors: Boolean, manageUsers: Boolean) {
        // Create new permissions object
        val newPermissions = UserPermissions(
            canViewLogs = viewLogs,
            canManageSensors = manageSensors,
            canManageUsers = manageUsers
        )
        
        // Get current user
        val currentUser = authRepository.getCurrentUser() ?: return
        
        // Update permissions for the current user
        authRepository.updateUserPermissions(currentUser.id, newPermissions)
    }
    
    // Implementation of new methods
    override suspend fun addUserWithRole(username: String, password: String, role: UserRole): Result<User> {
        return authRepository.createUserWithRole(username, password, role)
    }
    
    override suspend fun updateUserPermissions(userId: String, permissions: UserPermissions): Result<Unit> {
        return authRepository.updateUserPermissions(userId, permissions)
    }
    
    override suspend fun deleteUser(userId: String): Result<Unit> {
        return authRepository.deleteUser(userId)
    }
    
    override suspend fun updateUserRole(userId: String, role: UserRole): Result<Unit> {
        return authRepository.updateUserRole(userId, role)
    }
    
    override suspend fun getUserById(userId: String): Result<User> {
        return authRepository.getUserById(userId)
    }
} 