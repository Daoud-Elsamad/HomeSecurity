package com.example.homesecurity.models

import com.example.homesecurity.models.AccessLog
import com.example.homesecurity.models.Alert

data class ActivityLog(
    val id: String,
    val timestamp: Long,
    val description: String,
    val type: LogType
)

enum class LogType {
    NFC_ACCESS,
    SENSOR_EVENT
}

// Extension functions for conversion
fun AccessLog.toActivityLog() = ActivityLog(
    id = id,
    timestamp = timestamp,
    description = if (isGranted) 
        "Access granted to $userId at $doorId" 
    else 
        "Access denied for $userId at $doorId",
    type = LogType.NFC_ACCESS
)

fun Alert.toActivityLog() = ActivityLog(
    id = id,
    timestamp = timestamp,
    description = message,
    type = LogType.SENSOR_EVENT
) 