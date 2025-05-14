package com.example.homesecurity.repository

import android.util.Log
import com.example.homesecurity.models.Alert
import com.example.homesecurity.models.AlertType
import com.example.homesecurity.models.SensorData
import com.example.homesecurity.models.SensorType
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import java.util.Date

@Singleton
class FirestoreSensorRepository @Inject constructor() : SensorRepository {
    private val db = FirebaseFirestore.getInstance()
    private val sensorsCollection = db.collection("sensors")
    private val alertsCollection = db.collection("alerts")
    
    private val _sensorData = MutableStateFlow<List<SensorData>>(emptyList())
    private val _alerts = MutableStateFlow<List<Alert>>(emptyList())
    
    init {
        // Listen for sensor updates
        sensorsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Listen failed for sensors", error)
                return@addSnapshotListener
            }
            
            val sensors = snapshot?.documents?.mapNotNull { doc ->
                try {
                    val sensorId = doc.id
                    val typeStr = doc.getString("type") ?: return@mapNotNull null
                    val type = when (typeStr.uppercase()) {
                        "GAS" -> SensorType.GAS
                        "DOOR" -> SensorType.DOOR
                        "VIBRATION" -> SensorType.VIBRATION
                        "ULTRASONIC" -> SensorType.ULTRASONIC
                        "NFC" -> SensorType.NFC
                        else -> return@mapNotNull null
                    }
                    
                    val value = doc.getDouble("value") ?: 0.0
                    val location = doc.getString("location") ?: "Unknown"
                    val timestamp = doc.getTimestamp("timestamp")?.toDate()?.time ?: System.currentTimeMillis()
                    val isEnabled = doc.getBoolean("isEnabled") ?: true
                    val isLocked = if (type == SensorType.DOOR) { 
                        doc.getBoolean("isLocked") ?: false
                    } else false
                    
                    SensorData(
                        id = sensorId,
                        type = type,
                        value = value,
                        location = location,
                        timestamp = timestamp,
                        isEnabled = isEnabled,
                        isLocked = isLocked
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing sensor data", e)
                    null
                }
            } ?: emptyList()
            
            _sensorData.value = sensors
            Log.d(TAG, "Updated ${sensors.size} sensors from Firestore")
        }
        
        // Listen for alerts (ordered by timestamp descending - newest first)
        alertsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)  // Limit to most recent 50 alerts
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Listen failed for alerts", error)
                    return@addSnapshotListener
                }
                
                val alerts = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val alertId = doc.id
                        val typeStr = doc.getString("type") ?: return@mapNotNull null
                        val type = try {
                            AlertType.valueOf(typeStr)
                        } catch (e: Exception) {
                            Log.e(TAG, "Invalid alert type: $typeStr", e)
                            return@mapNotNull null
                        }
                        
                        val message = doc.getString("message") ?: ""
                        val sensorId = doc.getString("sensorId") ?: ""
                        val timestamp = doc.getTimestamp("timestamp")?.toDate()?.time ?: System.currentTimeMillis()
                        val isAcknowledged = doc.getBoolean("isAcknowledged") ?: false
                        
                        Alert(
                            id = alertId,
                            type = type,
                            message = message,
                            timestamp = timestamp,
                            sensorId = sensorId,
                            isAcknowledged = isAcknowledged
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing alert", e)
                        null
                    }
                } ?: emptyList()
                
                _alerts.value = alerts
                Log.d(TAG, "Updated ${alerts.size} alerts from Firestore")
            }
    }

    override suspend fun getAllSensorsData(): Flow<List<SensorData>> = _sensorData
    
    override suspend fun getSensorData(sensorId: String): Flow<SensorData> {
        return _sensorData.map { sensors ->
            sensors.find { sensor -> sensor.id == sensorId } 
                ?: throw IllegalArgumentException("Sensor not found")
        }
    }
    
    override suspend fun getAlerts(): Flow<List<Alert>> = _alerts

    override suspend fun getDoorOpenDuration(doorId: String): Long? {
        val door = _sensorData.value.find { 
            it.id == doorId && it.type == SensorType.DOOR 
        }
        return if (door?.value == 1.0) {
            door.lastOpenTime?.let { System.currentTimeMillis() - it }
        } else null
    }

    override suspend fun updateSensorThreshold(sensorId: String, threshold: Double) {
        try {
            sensorsCollection.document(sensorId)
                .update("threshold", threshold)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating sensor threshold", e)
        }
    }

    override suspend fun acknowledgeAlert(alertId: String) {
        try {
            alertsCollection.document(alertId)
                .update("isAcknowledged", true)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error acknowledging alert", e)
        }
    }

    override suspend fun toggleDoorLock(doorId: String, isLocked: Boolean) {
        try {
            sensorsCollection.document(doorId)
                .update("isLocked", isLocked)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling door lock", e)
        }
    }

    override suspend fun toggleSensor(sensorId: String, isEnabled: Boolean) {
        try {
            sensorsCollection.document(sensorId)
                .update("isEnabled", isEnabled)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling sensor", e)
        }
    }
    
    // Method to add a new alert to Firestore
    suspend fun addAlert(alert: Alert) {
        try {
            val alertData = hashMapOf(
                "type" to alert.type.name,
                "message" to alert.message,
                "timestamp" to FieldValue.serverTimestamp(),
                "sensorId" to alert.sensorId,
                "isAcknowledged" to alert.isAcknowledged
            )
            
            alertsCollection.document(alert.id)
                .set(alertData)
                .await()
            
            Log.d(TAG, "Successfully added alert: ${alert.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding alert", e)
        }
    }

    companion object {
        private const val TAG = "FirestoreSensorRepo"
    }
} 