package com.example.homesecurity.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homesecurity.models.User
import com.example.homesecurity.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _systemInitialized = MutableStateFlow<Boolean?>(null)
    val systemInitialized: StateFlow<Boolean?> = _systemInitialized.asStateFlow()

    init {
        viewModelScope.launch {
            // Check if system is initialized
            _systemInitialized.value = authRepository.isSystemInitialized()
            
            // Get current user if any
            _currentUser.value = authRepository.getCurrentUser()
            
            // Observe auth state changes
            authRepository.observeAuthState().collect { user ->
                _currentUser.value = user
            }
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            
            try {
                val result = authRepository.login(username, password)
                
                result.fold(
                    onSuccess = { user ->
                        _currentUser.value = user
                        
                        // Check if first login requiring password change
                        if (authRepository.isFirstLogin()) {
                            _loginState.value = LoginState.RequiresPasswordChange
                        } else {
                            _loginState.value = LoginState.Success(user)
                        }
                    },
                    onFailure = { error ->
                        _loginState.value = LoginState.Error(error.message ?: "Login failed")
                    }
                )
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _loginState.value = LoginState.Idle
        }
    }

    fun updateDefaultPassword(newPassword: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            
            try {
                val result = authRepository.updateDefaultAdminPassword(newPassword)
                
                result.fold(
                    onSuccess = {
                        _loginState.value = LoginState.Success(_currentUser.value!!)
                    },
                    onFailure = { error ->
                        _loginState.value = LoginState.Error(error.message ?: "Password update failed")
                    }
                )
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Password update failed")
            }
        }
    }

    fun createUser(username: String, password: String, isAdmin: Boolean) {
        viewModelScope.launch {
            try {
                authRepository.createUser(username, password, isAdmin)
            } catch (e: Exception) {
                // Handle user creation error
            }
        }
    }

    fun isDefaultAdminCredentials(username: String, password: String): Boolean {
        return username == "admin" && password == "homesecurity123"
    }

    fun refreshSystemInitializationStatus() {
        viewModelScope.launch {
            _systemInitialized.value = authRepository.isSystemInitialized()
        }
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val user: User) : LoginState()
    data class Error(val message: String) : LoginState()
    object RequiresPasswordChange : LoginState()
} 