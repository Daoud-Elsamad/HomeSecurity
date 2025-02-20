package com.example.homesecurity.models

data class SensorData(
    val id: String,
    val type: SensorType,
    val value: Double,
    val location: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: SensorStatus = SensorStatus.NORMAL,
    val threshold: Double? = null,
    val lastOpenTime: Long? = null, // For door sensors
    val isEnabled: Boolean = true,
    val isLocked: Boolean = false
)

enum class SensorType {
    GAS,
    DOOR,
    VIBRATION,
    ULTRASONIC,
    NFC
}

enum class SensorStatus {
    NORMAL,
    WARNING,
    ALERT,
    DISCONNECTED
} 