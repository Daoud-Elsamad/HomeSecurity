package com.example.homesecurity.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homesecurity.models.Alert
import com.example.homesecurity.models.AlertType
import com.example.homesecurity.models.SensorData
import com.example.homesecurity.models.SensorType
import com.example.homesecurity.repository.SensorRepository
import com.example.homesecurity.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val sensorRepository: SensorRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    private val _isSystemArmed = MutableStateFlow(false)
    val isSystemArmed: StateFlow<Boolean> = _isSystemArmed.asStateFlow()
    
    private val _sensorData = MutableStateFlow<List<SensorData>>(emptyList())
    val sensorData: StateFlow<List<SensorData>> = _sensorData.asStateFlow()
    
    private val _alerts = MutableStateFlow<List<Alert>>(emptyList())
    val alerts: StateFlow<List<Alert>> = _alerts.asStateFlow()

    private var currentGasThreshold = GAS_THRESHOLD
    private var currentVibrationThreshold = VIBRATION_THRESHOLD

    init {
        startMonitoring()
        observeSettings()
    }

    fun toggleSystemArmed() {
        _isSystemArmed.value = !_isSystemArmed.value
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
        sensors.forEach { sensor ->
            when (sensor.type) {
                SensorType.GAS -> checkGasLevel(sensor)
                SensorType.DOOR -> checkDoorStatus(sensor)
                SensorType.VIBRATION -> checkVibration(sensor)
                SensorType.ULTRASONIC -> {} // Handle ultrasonic sensor if needed
                SensorType.NFC -> {} // NFC module status is handled by NfcViewModel
            }
        }
    }

    private fun checkGasLevel(sensor: SensorData) {
        if (sensor.value >= currentGasThreshold) {
            createAlert(
                AlertType.GAS_LEAK,
                "High gas levels (${sensor.value} ppm) detected in ${sensor.location}",
                sensor.id
            )
        }
    }

    private fun checkDoorStatus(sensor: SensorData) {
        viewModelScope.launch {
            val openDuration = sensorRepository.getDoorOpenDuration(sensor.id)
            if (openDuration != null && openDuration > DOOR_OPEN_THRESHOLD) {
                createAlert(
                    AlertType.DOOR_LEFT_OPEN,
                    "Door in ${sensor.location} has been open for ${openDuration / 1000 / 60} minutes",
                    sensor.id
                )
            }
        }
    }

    private fun checkVibration(sensor: SensorData) {
        if (sensor.value >= currentVibrationThreshold) {
            createAlert(
                AlertType.VIBRATION_DETECTED,
                "Unusual vibration detected in ${sensor.location}",
                sensor.id
            )
        }
    }

    private fun createAlert(type: AlertType, message: String, sensorId: String) {
        val alert = Alert(
            type = type,
            message = message,
            sensorId = sensorId
        )
        viewModelScope.launch {
            _alerts.update { currentAlerts ->
                currentAlerts + alert
            }
        }
    }

    fun acknowledgeAlert(alertId: String) {
        viewModelScope.launch {
            sensorRepository.acknowledgeAlert(alertId)
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

    companion object {
        const val GAS_THRESHOLD = 400.0 // ppm
        const val VIBRATION_THRESHOLD = 8.0
        const val DOOR_OPEN_THRESHOLD = 5 * 60 * 1000L // 5 minutes in milliseconds
    }
} 