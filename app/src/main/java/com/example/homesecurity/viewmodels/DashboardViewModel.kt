package com.example.homesecurity.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homesecurity.models.Alert
import com.example.homesecurity.models.AlertType
import com.example.homesecurity.models.SensorData
import com.example.homesecurity.models.SensorType
import com.example.homesecurity.models.User
import com.example.homesecurity.repository.AuthRepository
import com.example.homesecurity.repository.HybridSensorRepository
import com.example.homesecurity.repository.SensorRepository
import com.example.homesecurity.repository.SettingsRepository
import com.example.homesecurity.services.NotificationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

private const val TAG = "DashboardViewModel"

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val sensorRepository: SensorRepository,
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
    private val notificationService: NotificationService
) : ViewModel() {
    
    // Cast the repository to HybridSensorRepository if possible
    private val hybridRepository = sensorRepository as? HybridSensorRepository
    
    private val _sensorData = MutableStateFlow<List<SensorData>>(emptyList())
    val sensorData: StateFlow<List<SensorData>> = _sensorData.asStateFlow()
    
    private val _alerts = MutableStateFlow<List<Alert>>(emptyList())
    val alerts: StateFlow<List<Alert>> = _alerts.asStateFlow()
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private var currentGasThreshold = GAS_THRESHOLD
    private var currentVibrationThreshold = VIBRATION_THRESHOLD
    
    // Date formatter for better display
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())

    init {
        startMonitoring()
        observeSettings()
        observeCurrentUser()
    }
    
    private fun observeCurrentUser() {
        viewModelScope.launch {
            authRepository.observeAuthState().collect { user ->
                _currentUser.value = user
            }
        }
    }

    private fun startMonitoring() {
        viewModelScope.launch {
            sensorRepository.getAllSensorsData()
                .collect { sensors ->
                    _sensorData.value = sensors
                    checkAllSensors(sensors)
                }
        }
        
        viewModelScope.launch {
            sensorRepository.getAlerts()
                .collect { alerts ->
                    _alerts.value = alerts
                }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.getGasThreshold().collect { threshold ->
                currentGasThreshold = threshold
            }
        }
        viewModelScope.launch {
            settingsRepository.getVibrationSensitivity().collect { sensitivity ->
                currentVibrationThreshold = sensitivity
            }
        }
    }

    private fun checkAllSensors(sensors: List<SensorData>) {
        // Only log sensor values but don't create alerts directly
        // The alerts will come from the Realtime Database "0" alert
        sensors.forEach { sensor ->
            when (sensor.type) {
                SensorType.GAS -> {
                    logSensorValue("Gas", sensor)
                }
                SensorType.DOOR -> {
                    logSensorValue("Door", sensor)
                }
                SensorType.VIBRATION -> {
                    logSensorValue("Vibration", sensor)
                }
                SensorType.ULTRASONIC -> {
                    logSensorValue("Ultrasonic", sensor)
                }
                SensorType.NFC -> {} // NFC module status is handled by NfcViewModel
            }
        }
    }

    private fun logSensorValue(sensorType: String, sensor: SensorData) {
        Log.d(TAG, "$sensorType sensor in ${sensor.location}: value=${sensor.value}")
    }

    fun acknowledgeAlert(alertId: String) {
        viewModelScope.launch {
            sensorRepository.acknowledgeAlert(alertId)
            notificationService.acknowledgeAlert(alertId)
        }
    }

    fun toggleDoorLock(doorId: String, isLocked: Boolean) {
        viewModelScope.launch {
            sensorRepository.toggleDoorLock(doorId, isLocked)
        }
    }

    fun toggleSensor(sensorId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            sensorRepository.toggleSensor(sensorId, isEnabled)
        }
    }

    // Force refreshes the alerts from the repository
    fun refreshAlerts() {
        Log.d(TAG, "Refreshing alerts")
        // Since we're observing the Flow, we don't need to do anything here
        // The changes will automatically propagate through the Flow
        // But we can force a refresh by accessing the repository again
        viewModelScope.launch {
            sensorRepository.getAlerts()
        }
    }
    
    // Function for testing only - creates a test alert directly in Firestore
    fun createTestAlert() {
        val alertTypes = AlertType.values()
        val randomType = alertTypes.random()
        val sensorId = "test_sensor_${(1..5).random()}"
        val location = listOf("Living Room", "Kitchen", "Bedroom", "Hallway", "Garage").random()
        
        val testAlert = when (randomType) {
            AlertType.GAS_LEAK -> Alert(
                id = UUID.randomUUID().toString(),
                type = randomType,
                message = "High gas levels detected in $location",
                sensorId = sensorId,
                timestamp = System.currentTimeMillis(),
                originalId = "test_${UUID.randomUUID().toString().substring(0, 8)}"
            )
            AlertType.PROXIMITY -> Alert(
                id = UUID.randomUUID().toString(),
                type = randomType,
                message = "Movement detected in $location (${(10..50).random()} cm)",
                sensorId = sensorId,
                timestamp = System.currentTimeMillis(),
                originalId = "test_${UUID.randomUUID().toString().substring(0, 8)}"
            )
            AlertType.VIBRATION_DETECTED -> Alert(
                id = UUID.randomUUID().toString(),
                type = randomType,
                message = "Unusual vibration detected in $location",
                sensorId = sensorId,
                timestamp = System.currentTimeMillis(),
                originalId = "test_${UUID.randomUUID().toString().substring(0, 8)}"
            )
            AlertType.FIRE -> Alert(
                id = UUID.randomUUID().toString(),
                type = randomType,
                message = "Fire detected in $location",
                sensorId = sensorId,
                timestamp = System.currentTimeMillis(),
                originalId = "test_${UUID.randomUUID().toString().substring(0, 8)}"
            )
            else -> Alert(
                id = UUID.randomUUID().toString(),
                type = randomType,
                message = "Alert in $location",
                sensorId = sensorId,
                timestamp = System.currentTimeMillis(),
                originalId = "test_${UUID.randomUUID().toString().substring(0, 8)}"
            )
        }
        
        Log.d(TAG, "Creating test alert: ${testAlert.type}, ${testAlert.message}")
        
        viewModelScope.launch {
            // Use the hybrid repository to add test alert
            hybridRepository?.addTestAlert(testAlert) ?: run {
                // Fallback to local update if not using hybrid repository
                _alerts.update { currentAlerts ->
                    currentAlerts + testAlert
                }
            }
        }
    }
    
    // Debug function to log alerts state
    fun logAlertsState() {
        Log.d(TAG, "Current alerts in ViewModel: ${_alerts.value.size}")
        _alerts.value.forEach { alert ->
            Log.d(TAG, "Alert from ViewModel: ${alert.id}, type: ${alert.type}, message: ${alert.message}")
        }
        
        // Create a test alert for debugging purposes if we don't have any
        viewModelScope.launch {
            // Check if we have Firebase alerts - if not, create a test one
            if (_alerts.value.isEmpty()) {
                Log.d(TAG, "No alerts found, creating a test alert")
                createTestAlert()
            }
        }
    }

    companion object {
        const val GAS_THRESHOLD = 400.0 // ppm
        const val VIBRATION_THRESHOLD = 8.0
        const val DOOR_OPEN_THRESHOLD = 5 * 60 * 1000L // 5 minutes in milliseconds
    }
} 