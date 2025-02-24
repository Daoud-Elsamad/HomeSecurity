package com.example.homesecurity.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NfcReaderMonitor @Inject constructor(
    private val scopeProvider: CoroutineScopeProvider
) {
    private val _readerStatus = MutableStateFlow<ReaderStatus>(ReaderStatus.Unknown)
    val readerStatus: StateFlow<ReaderStatus> = _readerStatus.asStateFlow()

    fun startMonitoring() {
        scopeProvider.scope.launch {
            monitorReader()
        }
    }

    private suspend fun monitorReader() {
        while (true) {
            try {
                // Check ESP32 NFC reader status
                // Will be implemented with ESP32 communication
                
                delay(5000) // Check every 5 seconds
            } catch (e: Exception) {
                _readerStatus.value = ReaderStatus.Error(e.message)
            }
        }
    }
}

sealed class ReaderStatus {
    object Unknown : ReaderStatus()
    object Active : ReaderStatus()
    object Inactive : ReaderStatus()
    data class Error(val message: String?) : ReaderStatus()
} 