package com.example.homesecurity.repository

import com.example.homesecurity.models.ActivityLog
import kotlinx.coroutines.flow.Flow

interface AccessLogRepository {
    suspend fun logDoorAccess(
        sensorId: String,
        doorLocation: String,
        isOpening: Boolean,
        timestamp: Long = System.currentTimeMillis()
    )
    
    suspend fun getAllAccessLogs(): Flow<List<ActivityLog>>
    
    suspend fun getAccessLogsBySensor(sensorId: String): Flow<List<ActivityLog>>
    
    suspend fun getAccessLogsByTimeRange(startTime: Long, endTime: Long): Flow<List<ActivityLog>>
    
    suspend fun clearOldLogs(olderThanDays: Int = 30)
} 