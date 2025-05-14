package com.example.homesecurity.models

import java.util.UUID

data class Alert(
    val id: String = UUID.randomUUID().toString(),
    val type: AlertType,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sensorId: String,
    val isAcknowledged: Boolean = false,
    val originalId: String? = null
)

enum class AlertType {
    GAS_LEAK,
    PROXIMITY,
    VIBRATION_DETECTED,
    NFC_UNAUTHORIZED,
    FIRE,
    DOOR_UNAUTHORIZED,
    DOOR_LEFT_OPEN
} 