package com.example.homesecurity.utils

import com.example.homesecurity.models.AccessLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class NfcRegistrationManager @Inject constructor() {
    private val _registrationState = MutableStateFlow<RegistrationState>(RegistrationState.Idle)
    val registrationState: StateFlow<RegistrationState> = _registrationState.asStateFlow()

    fun startRegistration(cardId: String, userData: UserData) {
        _registrationState.value = RegistrationState.InProgress
        try {
            // Use userData in the implementation
            if (!NfcCardOperations.validateCardFormat(cardId)) {
                _registrationState.value = RegistrationState.Error("Invalid card format")
                return
            }
            
            // TODO: Implement registration with userData
            registerUserData(userData)
            
            _registrationState.value = RegistrationState.Success
        } catch (e: Exception) {
            _registrationState.value = RegistrationState.Error(e.message)
        }
    }

    private fun registerUserData(userData: UserData) {
        // TODO: Implement user data registration
    }
}

sealed class RegistrationState {
    object Idle : RegistrationState()
    object InProgress : RegistrationState()
    object Success : RegistrationState()
    data class Error(val message: String?) : RegistrationState()
}

data class UserData(
    val userId: String,
    val accessLevel: AccessLevel,
    val validUntil: Long? = null
) 