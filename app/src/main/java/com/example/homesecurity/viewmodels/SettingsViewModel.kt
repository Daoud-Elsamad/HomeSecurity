package com.example.homesecurity.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homesecurity.models.User
import com.example.homesecurity.models.UserPermissions
import com.example.homesecurity.models.UserRole
import com.example.homesecurity.repository.AuthRepository
import com.example.homesecurity.repository.FirebaseAuthRepository
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
    
    private val _selectedUser = MutableStateFlow<User?>(null)
    val selectedUser: StateFlow<User?> = _selectedUser.asStateFlow()
    
    private val _userActionState = MutableStateFlow<UserActionState>(UserActionState.Idle)
    val userActionState: StateFlow<UserActionState> = _userActionState.asStateFlow()
    
    // Add current user state to track permissions
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        observeSettings()
        observeUsers()
        observeCurrentUser()
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
    
    private fun observeCurrentUser() {
        viewModelScope.launch {
            authRepository.observeAuthState().collect { user ->
                _currentUser.value = user
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
            _userActionState.value = UserActionState.Loading
            try {
                settingsRepository.addUser(username, password, isAdmin)
                _userActionState.value = UserActionState.Success("User $username added successfully")
            } catch (e: Exception) {
                _userActionState.value = UserActionState.Error("Failed to add user: ${e.message}")
            }
        }
    }
    
    fun addUserWithRole(username: String, password: String, role: UserRole) {
        viewModelScope.launch {
            _userActionState.value = UserActionState.Loading
            val result = settingsRepository.addUserWithRole(username, password, role)
            result.fold(
                onSuccess = { user ->
                    _userActionState.value = UserActionState.Success("User ${user.username} added successfully")
                },
                onFailure = { error ->
                    _userActionState.value = UserActionState.Error("Failed to add user: ${error.message}")
                }
            )
        }
    }

    fun updatePermissions(viewLogs: Boolean, manageSensors: Boolean, manageUsers: Boolean) {
        viewModelScope.launch {
            settingsRepository.updatePermissions(viewLogs, manageSensors, manageUsers)
        }
    }
    
    fun updateSelectedUserPermissions(viewLogs: Boolean, manageSensors: Boolean, manageUsers: Boolean) {
        viewModelScope.launch {
            _userActionState.value = UserActionState.Loading
            
            val userId = _selectedUser.value?.id ?: run {
                _userActionState.value = UserActionState.Error("No user selected")
                return@launch
            }
            
            val permissions = UserPermissions(
                canViewLogs = viewLogs,
                canManageSensors = manageSensors,
                canManageUsers = manageUsers
            )
            
            val result = settingsRepository.updateUserPermissions(userId, permissions)
            result.fold(
                onSuccess = {
                    _userActionState.value = UserActionState.Success("Permissions updated successfully")
                    // Update the selected user with new permissions
                    _selectedUser.value = _selectedUser.value?.copy(permissions = permissions)
                },
                onFailure = { error ->
                    _userActionState.value = UserActionState.Error("Failed to update permissions: ${error.message}")
                }
            )
        }
    }
    
    fun selectUser(userId: String) {
        viewModelScope.launch {
            _userActionState.value = UserActionState.Loading
            val result = settingsRepository.getUserById(userId)
            result.fold(
                onSuccess = { user ->
                    _selectedUser.value = user
                    _userActionState.value = UserActionState.Idle
                },
                onFailure = { error ->
                    _userActionState.value = UserActionState.Error("Failed to get user: ${error.message}")
                    _selectedUser.value = null
                }
            )
        }
    }
    
    fun clearSelectedUser() {
        _selectedUser.value = null
    }
    
    fun updateSelectedUserRole(role: UserRole) {
        viewModelScope.launch {
            _userActionState.value = UserActionState.Loading
            
            val userId = _selectedUser.value?.id ?: run {
                _userActionState.value = UserActionState.Error("No user selected")
                return@launch
            }
            
            val result = settingsRepository.updateUserRole(userId, role)
            result.fold(
                onSuccess = {
                    // Get the updated permissions based on role
                    val permissions = UserPermissions.fromRole(role)
                    _selectedUser.value = _selectedUser.value?.copy(role = role, permissions = permissions)
                    _userActionState.value = UserActionState.Success("User role updated successfully")
                    
                    // Request a fresh users list to ensure UI updates properly
                    refreshUsersList()
                },
                onFailure = { error ->
                    _userActionState.value = UserActionState.Error("Failed to update role: ${error.message}")
                }
            )
        }
    }
    
    private fun refreshUsersList() {
        // This will force a re-fetch of the users list
        viewModelScope.launch {
            val authRepo = authRepository as? FirebaseAuthRepository
            authRepo?.refreshUsersList()
        }
    }
    
    fun deleteSelectedUser() {
        viewModelScope.launch {
            _userActionState.value = UserActionState.Loading
            
            val userId = _selectedUser.value?.id ?: run {
                _userActionState.value = UserActionState.Error("No user selected")
                return@launch
            }
            
            val result = settingsRepository.deleteUser(userId)
            result.fold(
                onSuccess = {
                    _userActionState.value = UserActionState.Success("User deleted successfully")
                    _selectedUser.value = null
                },
                onFailure = { error ->
                    _userActionState.value = UserActionState.Error("Failed to delete user: ${error.message}")
                }
            )
        }
    }
    
    fun resetUserActionState() {
        _userActionState.value = UserActionState.Idle
    }
}

sealed class UserActionState {
    object Idle : UserActionState()
    object Loading : UserActionState()
    data class Success(val message: String) : UserActionState()
    data class Error(val message: String) : UserActionState()
} 