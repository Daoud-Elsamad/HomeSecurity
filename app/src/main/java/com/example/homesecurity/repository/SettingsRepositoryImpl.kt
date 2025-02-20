package com.example.homesecurity.repository

import com.example.homesecurity.viewmodels.DashboardViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor() : SettingsRepository {
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
        // TODO: Implement user addition when backend is ready
    }

    override suspend fun updatePermissions(viewLogs: Boolean, manageSensors: Boolean, manageUsers: Boolean) {
        // TODO: Implement permissions update when backend is ready
    }
} 