package com.example.homesecurity.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homesecurity.models.AccessLevel
import com.example.homesecurity.models.AccessLog
import com.example.homesecurity.models.NfcCard
import com.example.homesecurity.repository.NfcRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class NfcViewModel @Inject constructor(
    private val nfcRepository: NfcRepository
) : ViewModel() {

    private val _accessLogs = MutableStateFlow<List<AccessLog>>(emptyList())
    val accessLogs: StateFlow<List<AccessLog>> = _accessLogs.asStateFlow()

    private val _registeredCards = MutableStateFlow<List<NfcCard>>(emptyList())
    val registeredCards: StateFlow<List<NfcCard>> = _registeredCards.asStateFlow()

    private var currentUser: NfcCard? = null

    init {
        viewModelScope.launch {
            nfcRepository.getAccessLogs().collect {
                _accessLogs.value = it
            }
        }
        viewModelScope.launch {
            nfcRepository.getRegisteredCards().collect {
                _registeredCards.value = it
            }
        }
        // Initialize current user (for admin check)
        viewModelScope.launch {
            currentUser = nfcRepository.getCurrentUser()
        }
    }

    fun registerCard(userId: String, accessLevel: AccessLevel) {
        viewModelScope.launch {
            val card = NfcCard(
                id = UUID.randomUUID().toString(),
                userId = userId,
                accessLevel = accessLevel
            )
            nfcRepository.registerCard(card)
        }
    }

    fun validateAccess(cardId: String, doorId: String) {
        viewModelScope.launch {
            val isGranted = nfcRepository.validateAccess(cardId, doorId)
            // Repository already handles logging
        }
    }

    fun deactivateCard(cardId: String) {
        viewModelScope.launch {
            nfcRepository.deactivateCard(cardId)
        }
    }

    // Temporarily allow access for testing
    fun isAdminUser(): Boolean {
        return true  // This will be replaced with proper admin check when connected to backend
    }
} 