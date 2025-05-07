package com.example.homesecurity.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homesecurity.models.User
import com.example.homesecurity.repository.AuthRepository
import com.example.homesecurity.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _gasThreshold = MutableStateFlow(400.0)
    val gasThreshold: StateFlow<Double> = _gasThreshold.asStateFlow()
    
    private val _vibrationSensitivity = MutableStateFlow(8.0)
    val vibrationSensitivity: StateFlow<Double> = _vibrationSensitivity.asStateFlow()
    
    private val _notificationEnabled = MutableStateFlow(true)
    val notificationEnabled: StateFlow<Boolean> = _notificationEnabled.asStateFlow()
    
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    init {
        observeSettings()
        observeUsers()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.getGasThreshold().collect {
                _gasThreshold.value = it
            }
        }
        viewModelScope.launch {
            settingsRepository.getVibrationSensitivity().collect {
                _vibrationSensitivity.value = it
            }
        }
        viewModelScope.launch {
            settingsRepository.getNotificationPreference().collect {
                _notificationEnabled.value = it
            }
        }
    }
    
    private fun observeUsers() {
        viewModelScope.launch {
            authRepository.getAllUsers().collect {
                _users.value = it
            }
        }
    }

    fun updateGasThreshold(threshold: Double) {
        viewModelScope.launch {
            settingsRepository.updateGasThreshold(threshold)
            _gasThreshold.value = threshold
        }
    }

    fun updateVibrationSensitivity(sensitivity: Double) {
        viewModelScope.launch {
            settingsRepository.updateVibrationSensitivity(sensitivity)
            _vibrationSensitivity.value = sensitivity
        }
    }

    fun updateNotificationPreference(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateNotificationPreference(enabled)
            _notificationEnabled.value = enabled
        }
    }

    fun addUser(username: String, password: String, isAdmin: Boolean) {
        viewModelScope.launch {
            settingsRepository.addUser(username, password, isAdmin)
        }
    }

    fun updatePermissions(viewLogs: Boolean, manageSensors: Boolean, manageUsers: Boolean) {
        viewModelScope.launch {
            settingsRepository.updatePermissions(viewLogs, manageSensors, manageUsers)
        }
    }
} 