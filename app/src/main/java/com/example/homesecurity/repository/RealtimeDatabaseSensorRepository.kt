package com.example.homesecurity.repository

import android.util.Log
import com.example.homesecurity.models.Alert
import com.example.homesecurity.models.AlertType
import com.example.homesecurity.models.SensorData
import com.example.homesecurity.models.SensorType
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealtimeDatabaseSensorRepository @Inject constructor() : SensorRepository {
    private val database = FirebaseDatabase.getInstance()
    private val sensorsRef = database.getReference("sensors")
    private val alertsRef = database.getReference("alerts")
    
    private val _sensorData = MutableStateFlow<List<SensorData>>(emptyList())
    private val _alerts = MutableStateFlow<List<Alert>>(emptyList())
    
    init {
        // Listen for sensor updates
        sensorsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sensors = snapshot.children.mapNotNull { sensorSnapshot ->
                    try {
                        val sensorId = sensorSnapshot.key ?: return@mapNotNull null
                        
                        // Get the sensor type
                        val typeStr = sensorSnapshot.child("type").getValue(String::class.java)
                        val type = when (typeStr) {
                            "GAS" -> SensorType.GAS
                            "DOOR" -> SensorType.DOOR
                            "VIBRATION" -> SensorType.VIBRATION
                            "ULTRASONIC" -> SensorType.ULTRASONIC
                            "NFC" -> SensorType.NFC
                            else -> return@mapNotNull null
                        }
                        
                        // Get the sensor value
                        val value = sensorSnapshot.child("value").getValue(Double::class.java) ?: 0.0
                        
                        // Get other fields
                        val location = sensorSnapshot.child("location").getValue(String::class.java) ?: "Unknown"
                        val timestamp = sensorSnapshot.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()
                        val isEnabled = sensorSnapshot.child("isEnabled").getValue(Boolean::class.java) ?: true
                        
                        // Get door lock status if applicable
                        val isLocked = if (type == SensorType.DOOR) {
                            sensorSnapshot.child("isLocked").getValue(Boolean::class.java) ?: false
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
                }.toList()
                
                _sensorData.value = sensors
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Sensor updates failed", error.toException())
            }
        })
        
        // Listen for alerts
        alertsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val alerts = snapshot.children.mapNotNull { alertSnapshot ->
                    try {
                        Alert(
                            id = alertSnapshot.key ?: return@mapNotNull null,
                            type = AlertType.valueOf(
                                alertSnapshot.child("type").getValue(String::class.java) 
                                    ?: return@mapNotNull null
                            ),
                            message = alertSnapshot.child("message").getValue(String::class.java) ?: "",
                            timestamp = alertSnapshot.child("timestamp").getValue(Long::class.java) 
                                ?: System.currentTimeMillis(),
                            sensorId = alertSnapshot.child("sensorId").getValue(String::class.java) ?: "",
                            isAcknowledged = alertSnapshot.child("isAcknowledged").getValue(Boolean::class.java) ?: false
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing alert", e)
                        null
                    }
                }.toList()
                
                _alerts.value = alerts
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Alert updates failed", error.toException())
            }
        })
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
        sensorsRef.child(sensorId).child("threshold").setValue(threshold)
    }

    override suspend fun acknowledgeAlert(alertId: String) {
        alertsRef.child(alertId).child("isAcknowledged").setValue(true)
    }

    override suspend fun toggleDoorLock(doorId: String, isLocked: Boolean) {
        sensorsRef.child(doorId).child("isLocked").setValue(isLocked)
    }

    override suspend fun toggleSensor(sensorId: String, isEnabled: Boolean) {
        sensorsRef.child(sensorId).child("isEnabled").setValue(isEnabled)
    }

    companion object {
        private const val TAG = "RealtimeDBSensorRepo"
    }
} 