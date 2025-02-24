package com.example.homesecurity.utils

import android.nfc.Tag
import android.nfc.tech.NfcA
import com.example.homesecurity.utils.bytesToHexString

object NfcCardOperations {
    fun readCardData(tag: Tag): String {
        val nfcA = NfcA.get(tag)
        return try {
            nfcA.connect()
            val response = nfcA.transceive(byteArrayOf(0x30.toByte(), 0x00)) // Read command
            nfcA.close()
            bytesToHexString(response)
        } catch (e: Exception) {
            throw NfcReadException("Failed to read card data", e)
        }
    }

    fun validateCardFormat(cardId: String): Boolean {
        return cardId.isNotEmpty() && cardId.length >= 8
    }
}

class NfcReadException(message: String, cause: Throwable? = null) : Exception(message, cause) 