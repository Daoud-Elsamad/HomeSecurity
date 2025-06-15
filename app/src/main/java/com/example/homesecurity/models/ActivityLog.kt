package com.example.homesecurity.models

import com.example.homesecurity.models.AccessLog
import com.example.homesecurity.models.Alert

data class ActivityLog(
    val id: String,
    val timestamp: Long,
    val description: String,
    val type: LogType,
    val sensorId: String? = null,
    val doorId: String? = null,
    val userId: String? = null,
    val accessMethod: String? = null
)

enum class LogType {
    NFC_ACCESS,
    SENSOR_EVENT,
    DOOR_ENTRY,
    DOOR_EXIT,
    UNAUTHORIZED_ACCESS
}

// Extension functions for conversion
fun AccessLog.toActivityLog() = ActivityLog(
    id = id,
    timestamp = timestamp,
    description = if (isGranted) 
        "NFC access granted to $userId at $doorId" 
    else 
        "NFC access denied for $userId at $doorId",
    type = LogType.NFC_ACCESS,
    doorId = doorId,
    userId = userId,
    accessMethod = "NFC Card"
)

fun Alert.toActivityLog() = ActivityLog(
    id = id,
    timestamp = timestamp,
    description = message,
    type = when (type) {
        AlertType.DOOR_UNAUTHORIZED -> LogType.UNAUTHORIZED_ACCESS
        AlertType.DOOR_LEFT_OPEN -> LogType.SENSOR_EVENT
        else -> LogType.SENSOR_EVENT
    },
    sensorId = sensorId
)

// New function to create door access log from sensor data
fun createDoorAccessLog(
    sensorId: String,
    doorLocation: String,
    isOpening: Boolean,
    timestamp: Long = System.currentTimeMillis()
) = ActivityLog(
    id = java.util.UUID.randomUUID().toString(),
    timestamp = timestamp,
    description = if (isOpening) 
        "Door opened at $doorLocation" 
    else 
        "Door closed at $doorLocation",
    type = if (isOpening) LogType.DOOR_ENTRY else LogType.DOOR_EXIT,
    sensorId = sensorId,
    doorId = sensorId,
    accessMethod = "Physical Access"
) 