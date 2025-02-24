package com.example.homesecurity.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homesecurity.models.AccessLevel
import com.example.homesecurity.models.AccessLog
import com.example.homesecurity.models.NfcCard
import com.example.homesecurity.repository.NfcRepository
import com.example.homesecurity.utils.NfcUtils
import com.example.homesecurity.utils.NfcStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import android.nfc.Tag
import com.example.homesecurity.utils.NfcRegistrationManager
import com.example.homesecurity.utils.NfcAccessManager
import com.example.homesecurity.utils.NfcReaderMonitor
import com.example.homesecurity.utils.UserData
import com.example.homesecurity.utils.bytesToHexString

@HiltViewModel
class NfcViewModel @Inject constructor(
    private val nfcRepository: NfcRepository,
    private val nfcUtils: NfcUtils,
    private val registrationManager: NfcRegistrationManager,
    private val accessManager: NfcAccessManager,
    private val readerMonitor: NfcReaderMonitor
) : ViewModel() {

    // Access Logs
    private val _accessLogs = MutableStateFlow<List<AccessLog>>(emptyList())
    val accessLogs: StateFlow<List<AccessLog>> = _accessLogs.asStateFlow()

    // Registered Cards
    private val _registeredCards = MutableStateFlow<List<NfcCard>>(emptyList())
    val registeredCards: StateFlow<List<NfcCard>> = _registeredCards.asStateFlow()

    // NFC Status
    private val _nfcStatus = MutableStateFlow<NfcStatus>(NfcStatus.READY)
    val nfcStatus: StateFlow<NfcStatus> = _nfcStatus.asStateFlow()

    // Scanned Card
    private val _scannedCard = MutableStateFlow<NfcCard?>(null)
    val scannedCard: StateFlow<NfcCard?> = _scannedCard.asStateFlow()

    // Registration States
    val registrationState = registrationManager.registrationState
    val accessState = accessManager.accessState
    val readerStatus = readerMonitor.readerStatus

    private var currentUser: NfcCard? = null

    private val _registrationComplete = MutableStateFlow<Boolean>(false)
    val registrationComplete: StateFlow<Boolean> = _registrationComplete.asStateFlow()

    private val _cardDeactivated = MutableStateFlow<Boolean>(false)
    val cardDeactivated: StateFlow<Boolean> = _cardDeactivated.asStateFlow()

    init {
        viewModelScope.launch {
            // Initialize access logs
            nfcRepository.getAccessLogs().collect {
                _accessLogs.value = it
            }
        }

        viewModelScope.launch {
            // Initialize registered cards
            nfcRepository.getRegisteredCards().collect {
                _registeredCards.value = it
            }
        }

        viewModelScope.launch {
            // Initialize current user
            currentUser = nfcRepository.getCurrentUser()
        }

        // Start monitoring NFC reader
        readerMonitor.startMonitoring()
    }

    fun startScanning() {
        viewModelScope.launch {
            when (nfcUtils.checkNfcStatus()) {
                NfcStatus.NOT_SUPPORTED -> _nfcStatus.value = NfcStatus.NOT_SUPPORTED
                NfcStatus.DISABLED -> _nfcStatus.value = NfcStatus.DISABLED
                NfcStatus.READY -> _nfcStatus.value = NfcStatus.SCANNING
                else -> {} // Handle other states
            }
        }
    }

    fun handleNfcTag(tag: Tag) {
        viewModelScope.launch {
            try {
                val cardId = bytesToHexString(tag.id)
                _scannedCard.value = NfcCard(
                    id = cardId,
                    userId = "", // Will be set during registration
                    accessLevel = AccessLevel.GUEST // Default level
                )
                _nfcStatus.value = NfcStatus.SUCCESS
            } catch (e: Exception) {
                _nfcStatus.value = NfcStatus.ERROR
            }
        }
    }

    fun validateAccess(cardId: String, doorId: String) {
        accessManager.checkAccess(cardId, doorId)
    }

    fun registerCard(userId: String, accessLevel: AccessLevel) {
        viewModelScope.launch {
            _scannedCard.value?.let { card ->
                val updatedCard = card.copy(
                    userId = userId,
                    accessLevel = accessLevel
                )
                if (nfcRepository.registerCard(updatedCard)) {
                    _registrationComplete.value = true
                }
            }
        }
    }

    fun deactivateCard(cardId: String) {
        viewModelScope.launch {
            try {
                nfcRepository.deactivateCard(cardId)
                _cardDeactivated.value = true
            } catch (e: Exception) {
                _nfcStatus.value = NfcStatus.ERROR
            }
        }
    }

    fun isAdminUser(): Boolean {
        return currentUser?.accessLevel == AccessLevel.ADMIN
    }

    fun completeRegistration(userId: String, accessLevel: AccessLevel) {
        registerCard(userId, accessLevel)
    }
} 