package com.example.homesecurity.utils

fun bytesToHexString(bytes: ByteArray): String {
    val hexArray = "0123456789ABCDEF".toCharArray()
    val hexChars = CharArray(bytes.size * 2)
    for (j in bytes.indices) {
        val v = bytes[j].toInt() and 0xFF
        hexChars[j * 2] = hexArray[v ushr 4]
        hexChars[j * 2 + 1] = hexArray[v and 0x0F]
    }
    return String(hexChars)
} 