package com.example.homesecurity.repository

import android.util.Log
import com.example.homesecurity.models.ActivityLog
import com.example.homesecurity.models.LogType
import com.example.homesecurity.models.createDoorAccessLog
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAccessLogRepository @Inject constructor() : AccessLogRepository {
    private val database = FirebaseDatabase.getInstance()
    private val accessLogsRef = database.getReference("access_logs")
    
    private val _accessLogs = MutableStateFlow<List<ActivityLog>>(emptyList())
    
    companion object {
        private const val TAG = "FirebaseAccessLogRepo"
        private const val MAX_LOGS_PER_QUERY = 1000
    }
    
    init {
        // Listen for access log updates
        accessLogsRef.orderByChild("timestamp")
            .limitToLast(MAX_LOGS_PER_QUERY)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val logs = snapshot.children.mapNotNull { logSnapshot ->
                        try {
                            val logId = logSnapshot.key ?: return@mapNotNull null
                            
                            ActivityLog(
                                id = logId,
                                timestamp = logSnapshot.child("timestamp").getValue(Long::class.java) ?: 0L,
                                description = logSnapshot.child("description").getValue(String::class.java) ?: "",
                                type = LogType.valueOf(
                                    logSnapshot.child("type").getValue(String::class.java) ?: "SENSOR_EVENT"
                                ),
                                sensorId = logSnapshot.child("sensorId").getValue(String::class.java),
                                doorId = logSnapshot.child("doorId").getValue(String::class.java),
                                userId = logSnapshot.child("userId").getValue(String::class.java),
                                accessMethod = logSnapshot.child("accessMethod").getValue(String::class.java)
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing access log", e)
                            null
                        }
                    }.sortedByDescending { it.timestamp }
                    
                    _accessLogs.value = logs
                    Log.d(TAG, "Updated ${logs.size} access logs from Firebase")
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Failed to load access logs", error.toException())
                }
            })
    }
    
    override suspend fun logDoorAccess(
        sensorId: String,
        doorLocation: String,
        isOpening: Boolean,
        timestamp: Long
    ) {
        try {
            val accessLog = createDoorAccessLog(sensorId, doorLocation, isOpening, timestamp)
            
            val logData = mapOf(
                "timestamp" to accessLog.timestamp,
                "description" to accessLog.description,
                "type" to accessLog.type.name,
                "sensorId" to accessLog.sensorId,
                "doorId" to accessLog.doorId,
                "accessMethod" to accessLog.accessMethod
            )
            
            accessLogsRef.child(accessLog.id).setValue(logData)
                .addOnSuccessListener {
                    Log.d(TAG, "Door access logged: ${accessLog.description}")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to log door access", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating door access log", e)
        }
    }
    
    override suspend fun getAllAccessLogs(): Flow<List<ActivityLog>> = _accessLogs
    
    override suspend fun getAccessLogsBySensor(sensorId: String): Flow<List<ActivityLog>> = 
        callbackFlow {
            val listener = accessLogsRef.orderByChild("sensorId").equalTo(sensorId)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val logs = snapshot.children.mapNotNull { logSnapshot ->
                            try {
                                val logId = logSnapshot.key ?: return@mapNotNull null
                                
                                ActivityLog(
                                    id = logId,
                                    timestamp = logSnapshot.child("timestamp").getValue(Long::class.java) ?: 0L,
                                    description = logSnapshot.child("description").getValue(String::class.java) ?: "",
                                    type = LogType.valueOf(
                                        logSnapshot.child("type").getValue(String::class.java) ?: "SENSOR_EVENT"
                                    ),
                                    sensorId = logSnapshot.child("sensorId").getValue(String::class.java),
                                    doorId = logSnapshot.child("doorId").getValue(String::class.java),
                                    userId = logSnapshot.child("userId").getValue(String::class.java),
                                    accessMethod = logSnapshot.child("accessMethod").getValue(String::class.java)
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing sensor access log", e)
                                null
                            }
                        }.sortedByDescending { it.timestamp }
                        
                        trySend(logs)
                    }
                    
                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Failed to load sensor access logs", error.toException())
                        close(error.toException())
                    }
                })
            
            awaitClose { accessLogsRef.removeEventListener(listener) }
        }
    
    override suspend fun getAccessLogsByTimeRange(startTime: Long, endTime: Long): Flow<List<ActivityLog>> = 
        callbackFlow {
            val listener = accessLogsRef.orderByChild("timestamp")
                .startAt(startTime.toDouble()).endAt(endTime.toDouble())
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val logs = snapshot.children.mapNotNull { logSnapshot ->
                            try {
                                val logId = logSnapshot.key ?: return@mapNotNull null
                                
                                ActivityLog(
                                    id = logId,
                                    timestamp = logSnapshot.child("timestamp").getValue(Long::class.java) ?: 0L,
                                    description = logSnapshot.child("description").getValue(String::class.java) ?: "",
                                    type = LogType.valueOf(
                                        logSnapshot.child("type").getValue(String::class.java) ?: "SENSOR_EVENT"
                                    ),
                                    sensorId = logSnapshot.child("sensorId").getValue(String::class.java),
                                    doorId = logSnapshot.child("doorId").getValue(String::class.java),
                                    userId = logSnapshot.child("userId").getValue(String::class.java),
                                    accessMethod = logSnapshot.child("accessMethod").getValue(String::class.java)
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing time range access log", e)
                                null
                            }
                        }.sortedByDescending { it.timestamp }
                        
                        trySend(logs)
                    }
                    
                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Failed to load time range access logs", error.toException())
                        close(error.toException())
                    }
                })
            
            awaitClose { accessLogsRef.removeEventListener(listener) }
        }
    
    override suspend fun clearOldLogs(olderThanDays: Int) {
        try {
            val cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)
            
            accessLogsRef.orderByChild("timestamp").endAt(cutoffTime.toDouble())
                .get().addOnSuccessListener { snapshot ->
                    snapshot.children.forEach { logSnapshot ->
                        logSnapshot.ref.removeValue()
                    }
                    Log.d(TAG, "Cleared old access logs older than $olderThanDays days")
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Failed to clear old access logs", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing old logs", e)
        }
    }
} 