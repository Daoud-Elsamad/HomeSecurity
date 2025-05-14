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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HybridSensorRepository @Inject constructor() : SensorRepository {
    // Firebase instances
    private val realtimeDb = FirebaseDatabase.getInstance()
    private val firestoreDb = FirebaseFirestore.getInstance()
    
    // Database references
    private val sensorsRef = realtimeDb.getReference("sensors")
    private val realtimeAlertsRef = realtimeDb.getReference("alerts")
    private val firestoreSensorsCollection = firestoreDb.collection("sensors")
    private val firestoreAlertsCollection = firestoreDb.collection("Alerts")
    
    // Data flows
    private val _sensorData = MutableStateFlow<List<SensorData>>(emptyList())
    private val _alerts = MutableStateFlow<List<Alert>>(emptyList())
    
    init {
        // Listen for sensor updates from Realtime Database
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
        
        // Listen specifically for changes to the "0" alert entry in Realtime Database
        realtimeAlertsRef.child("0").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    if (!snapshot.exists()) {
                        Log.d(TAG, "Alert '0' does not exist in Realtime Database")
                        return
                    }
                    
                    Log.d(TAG, "Alert '0' updated in Realtime Database, updating in Firestore")
                    
                    val typeStr = snapshot.child("type").getValue(String::class.java)
                    if (typeStr == null) {
                        Log.e(TAG, "Alert type is null")
                        return
                    }
                    
                    val type = try {
                        AlertType.valueOf(typeStr)
                    } catch (e: Exception) {
                        Log.e(TAG, "Invalid alert type: $typeStr", e)
                        return
                    }
                    
                    val message = snapshot.child("message").getValue(String::class.java) ?: ""
                    val sensorId = snapshot.child("sensorId").getValue(String::class.java) ?: ""
                    
                    // Fix potential timestamp issues from Realtime Database
                    val originalTimestamp = snapshot.child("timestamp").getValue(Long::class.java) 
                        ?: System.currentTimeMillis()
                    
                    // If timestamp is too old (before 2020), it's likely in the wrong format
                    // ESP32 might be sending seconds instead of milliseconds
                    val correctedTimestamp = if (originalTimestamp < 1577836800000L) { // Jan 1, 2020
                        // Log the problematic timestamp
                        Log.d(TAG, "Fixing invalid timestamp: $originalTimestamp")
                        
                        if (originalTimestamp < 2000000000L) {
                            // If it's in seconds, convert to milliseconds
                            originalTimestamp * 1000
                        } else {
                            // If we can't determine the format, use current time
                            System.currentTimeMillis()
                        }
                    } else {
                        originalTimestamp
                    }
                    
                    val isAcknowledged = snapshot.child("isAcknowledged").getValue(Boolean::class.java) ?: false
                    
                    // Check if we already have an alert with originalId "0" in Firestore
                    firestoreAlertsCollection
                        .whereEqualTo("originalId", "0")
                        .limit(1)
                        .get()
                        .addOnSuccessListener { documents ->
                            val alertData = hashMapOf(
                                "type" to type.name,
                                "message" to message,
                                "timestamp" to correctedTimestamp,
                                "sensorId" to sensorId,
                                "isAcknowledged" to isAcknowledged,
                                "originalId" to "0" // Mark that this came from alert "0"
                            )
                            
                            // Log the timestamp values for debugging
                            val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm:ss", java.util.Locale.getDefault())
                            Log.d(TAG, "Original timestamp: $originalTimestamp (${dateFormat.format(java.util.Date(originalTimestamp))})")
                            Log.d(TAG, "Corrected timestamp: $correctedTimestamp (${dateFormat.format(java.util.Date(correctedTimestamp))})")
                            
                            if (!documents.isEmpty) {
                                // Update existing document instead of creating new one
                                val existingDoc = documents.documents[0]
                                firestoreAlertsCollection.document(existingDoc.id)
                                    .set(alertData)
                                    .addOnSuccessListener {
                                        Log.d(TAG, "Updated existing Firestore alert for '0': ${existingDoc.id}")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e(TAG, "Error updating existing Firestore alert", e)
                                    }
                            } else {
                                // Create new document
                                val firestoreAlertId = UUID.randomUUID().toString()
                                firestoreAlertsCollection.document(firestoreAlertId)
                                    .set(alertData)
                                    .addOnSuccessListener {
                                        Log.d(TAG, "Created new Firestore alert for '0': $firestoreAlertId")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e(TAG, "Error creating new Firestore alert", e)
                                    }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error checking for existing Firestore alerts", e)
                        }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing alert '0' from Realtime DB", e)
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Alert '0' updates failed", error.toException())
            }
        })
        
        // Listen for alerts from Firestore for display
        firestoreAlertsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)  // Limit to most recent 50 alerts
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Listen failed for Firestore alerts", error)
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
                        val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        val isAcknowledged = doc.getBoolean("isAcknowledged") ?: false
                        val originalId = doc.getString("originalId")
                        
                        Alert(
                            id = alertId,
                            type = type,
                            message = message,
                            timestamp = timestamp,
                            sensorId = sensorId,
                            isAcknowledged = isAcknowledged,
                            originalId = originalId
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing alert from Firestore", e)
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
            sensorsRef.child(sensorId).child("threshold").setValue(threshold)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating sensor threshold in Realtime DB", e)
        }
    }

    override suspend fun acknowledgeAlert(alertId: String) {
        try {
            // Update in Firestore
            firestoreAlertsCollection.document(alertId)
                .update("isAcknowledged", true)
                .await()
            
            Log.d(TAG, "Alert acknowledged in Firestore: $alertId")
            
            // Check if this alert originated from the "0" alert in Realtime DB
            val firestoreAlert = firestoreAlertsCollection.document(alertId).get().await()
            val originalId = firestoreAlert.getString("originalId")
            
            if (originalId == "0") {
                // Update the "0" alert in Realtime DB
                realtimeAlertsRef.child("0").child("isAcknowledged").setValue(true)
                    .addOnSuccessListener {
                        Log.d(TAG, "Alert '0' acknowledged in Realtime DB")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error acknowledging alert '0' in Realtime DB", e)
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error acknowledging alert", e)
        }
    }

    override suspend fun toggleDoorLock(doorId: String, isLocked: Boolean) {
        try {
            sensorsRef.child(doorId).child("isLocked").setValue(isLocked)
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling door lock", e)
        }
    }

    override suspend fun toggleSensor(sensorId: String, isEnabled: Boolean) {
        try {
            sensorsRef.child(sensorId).child("isEnabled").setValue(isEnabled)
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling sensor", e)
        }
    }
    
    // Method to add an alert to Firestore only (not Realtime DB)
    suspend fun addAlert(alert: Alert) {
        try {
            // Add to Firestore only
            val alertData = hashMapOf(
                "type" to alert.type.name,
                "message" to alert.message,
                "timestamp" to alert.timestamp,
                "sensorId" to alert.sensorId,
                "isAcknowledged" to alert.isAcknowledged
            )
            
            firestoreAlertsCollection.document(alert.id)
                .set(alertData)
                .await()
            
            Log.d(TAG, "Successfully added alert to Firestore: ${alert.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding alert to Firestore", e)
        }
    }
    
    // Method to manually add a test alert to Firestore only
    suspend fun addTestAlert(alert: Alert) {
        try {
            // Add to Firestore only
            val alertData = hashMapOf(
                "type" to alert.type.name,
                "message" to alert.message,
                "timestamp" to alert.timestamp,
                "sensorId" to alert.sensorId,
                "isAcknowledged" to alert.isAcknowledged
            )
            
            firestoreAlertsCollection.document(alert.id)
                .set(alertData)
                .await()
            
            Log.d(TAG, "Successfully added test alert to Firestore: ${alert.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding test alert to Firestore", e)
        }
    }
    
    // Function to manually trigger a sync of all alerts from Realtime DB to Firestore
    suspend fun syncRealtimeAlertsToFirestore(): Int {
        var syncCount = 0
        try {
            val snapshot = realtimeAlertsRef.get().await()
            
            for (childSnapshot in snapshot.children) {
                val alertId = childSnapshot.key ?: continue
                
                val typeStr = childSnapshot.child("type").getValue(String::class.java) ?: continue
                val type = try {
                    AlertType.valueOf(typeStr)
                } catch (e: Exception) {
                    Log.e(TAG, "Invalid alert type: $typeStr", e)
                    continue
                }
                
                val message = childSnapshot.child("message").getValue(String::class.java) ?: ""
                val sensorId = childSnapshot.child("sensorId").getValue(String::class.java) ?: ""
                val timestamp = childSnapshot.child("timestamp").getValue(Long::class.java) 
                    ?: System.currentTimeMillis()
                val isAcknowledged = childSnapshot.child("isAcknowledged").getValue(Boolean::class.java) ?: false
                
                // Use the original ID from Realtime DB
                val alertData = hashMapOf(
                    "type" to type.name,
                    "message" to message,
                    "timestamp" to timestamp,
                    "sensorId" to sensorId,
                    "isAcknowledged" to isAcknowledged,
                    "originalId" to alertId
                )
                
                // Add to Firestore with the same ID as in Realtime DB
                firestoreAlertsCollection.document(alertId)
                    .set(alertData)
                    .await()
                
                syncCount++
                Log.d(TAG, "Synced alert from Realtime DB to Firestore: $alertId")
            }
            
            Log.d(TAG, "Successfully synced $syncCount alerts from Realtime DB to Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing alerts from Realtime DB to Firestore", e)
        }
        
        return syncCount
    }

    companion object {
        private const val TAG = "HybridSensorRepo"
    }
} 