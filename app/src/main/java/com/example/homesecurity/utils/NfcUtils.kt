package com.example.homesecurity.utils

import android.content.Context
import android.nfc.NfcAdapter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NfcUtils @Inject constructor(
    private val context: Context
) {
    private val nfcAdapter: NfcAdapter? by lazy {
        NfcAdapter.getDefaultAdapter(context)
    }

    fun isNfcAvailable(): Boolean = nfcAdapter != null
    
    fun isNfcEnabled(): Boolean = nfcAdapter?.isEnabled == true

    fun checkNfcStatus(): NfcStatus {
        return when {
            !isNfcAvailable() -> NfcStatus.NOT_SUPPORTED
            !isNfcEnabled() -> NfcStatus.DISABLED
            else -> NfcStatus.READY
        }
    }
}

enum class NfcStatus {
    NOT_SUPPORTED,
    DISABLED,
    READY,
    SCANNING,
    SUCCESS,
    ERROR
} 