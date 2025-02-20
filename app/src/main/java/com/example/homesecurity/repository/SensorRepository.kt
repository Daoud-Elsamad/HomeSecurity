package com.example.homesecurity.repository

import com.example.homesecurity.models.Alert
import com.example.homesecurity.models.AlertType
import com.example.homesecurity.models.SensorData
import com.example.homesecurity.models.SensorType
import com.example.homesecurity.models.SensorStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import java.util.Random

interface SensorRepository {
    suspend fun getAllSensorsData(): Flow<List<SensorData>>
    suspend fun getSensorData(sensorId: String): Flow<SensorData>
    suspend fun getAlerts(): Flow<List<Alert>>
    suspend fun getDoorOpenDuration(doorId: String): Long?
    suspend fun updateSensorThreshold(sensorId: String, threshold: Double)
    suspend fun acknowledgeAlert(alertId: String)
    suspend fun toggleDoorLock(doorId: String, isLocked: Boolean)
    suspend fun toggleSensor(sensorId: String, isEnabled: Boolean)
}

@Singleton
class MockSensorRepository @Inject constructor() : SensorRepository {
    private val DOOR_OPEN_THRESHOLD = 5 * 60 * 1000L // 5 minutes in milliseconds
    
    private val mockData = MutableStateFlow<List<SensorData>>(
        listOf(
            SensorData(
                id = "gas1",
                type = SensorType.GAS,
                value = 200.0,
                location = "Kitchen",
                threshold = 400.0,
                isEnabled = true
            ),
            SensorData(
                id = "door1",
                type = SensorType.DOOR,
                value = 0.0,
                location = "Main Door",
                isEnabled = true,
                isLocked = true
            ),
            SensorData(
                id = "door2",
                type = SensorType.DOOR,
                value = 0.0,
                location = "Bedroom 2 Door",
                isEnabled = true,
                isLocked = false
            ),
            SensorData(
                id = "vib1",
                type = SensorType.VIBRATION,
                value = 2.5,
                location = "Living Room",
                threshold = 8.0,
                isEnabled = true
            )
        )
    )

    private val _alerts = MutableStateFlow<List<Alert>>(emptyList())

    init {
        startMockDataUpdates()
    }

    private fun startMockDataUpdates() {
        CoroutineScope(Dispatchers.Default).launch {
            while (true) {
                delay(2000) // Increase delay to 2 seconds
                mockData.update { sensors ->
                    sensors.map { sensor ->
                        when (sensor.type) {
                            SensorType.GAS -> sensor.copy(
                                value = (sensor.value + (Random().nextInt(5) - 2)).coerceIn(0.0, 500.0)
                            )
                            SensorType.VIBRATION -> sensor.copy(
                                value = (sensor.value + (Random().nextInt(5) - 2) / 10.0).coerceIn(0.0, 10.0)
                            )
                            else -> sensor
                        }
                    }
                }
            }
        }
    }

    override suspend fun getAllSensorsData(): Flow<List<SensorData>> = mockData
    
    override suspend fun getSensorData(sensorId: String): Flow<SensorData> {
        return mockData.map { sensors ->
            sensors.find { it.id == sensorId } ?: throw IllegalArgumentException("Sensor not found")
        }
    }

    override suspend fun getAlerts(): Flow<List<Alert>> = _alerts

    override suspend fun getDoorOpenDuration(doorId: String): Long? {
        val door = mockData.value.find { it.id == doorId && it.type == SensorType.DOOR }
        return if (door?.value == 1.0) {
            door.lastOpenTime?.let { System.currentTimeMillis() - it }
        } else null
    }

    override suspend fun updateSensorThreshold(sensorId: String, threshold: Double) {
        mockData.update { sensors ->
            sensors.map { sensor ->
                if (sensor.id == sensorId) sensor.copy(threshold = threshold) else sensor
            }
        }
    }

    override suspend fun acknowledgeAlert(alertId: String) {
        _alerts.update { alerts ->
            alerts.map { alert ->
                if (alert.id == alertId) alert.copy(isAcknowledged = true) else alert
            }
        }
    }

    override suspend fun toggleDoorLock(doorId: String, isLocked: Boolean) {
        mockData.update { sensors ->
            sensors.map { sensor ->
                if (sensor.id == doorId && sensor.type == SensorType.DOOR) {
                    sensor.copy(isLocked = isLocked)
                } else sensor
            }
        }
    }

    override suspend fun toggleSensor(sensorId: String, isEnabled: Boolean) {
        mockData.update { sensors ->
            sensors.map { sensor ->
                if (sensor.id == sensorId) {
                    sensor.copy(isEnabled = isEnabled)
                } else sensor
            }
        }
    }
} 