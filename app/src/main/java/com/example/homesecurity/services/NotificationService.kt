package com.example.homesecurity.services

import android.util.Log
import com.example.homesecurity.models.Alert
import com.example.homesecurity.repository.SettingsRepository
import com.example.homesecurity.repository.SensorRepository
import com.example.homesecurity.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "NotificationService"

@Singleton
class NotificationService @Inject constructor(
    private val sensorRepository: SensorRepository,
    private val settingsRepository: SettingsRepository,
    private val notificationHelper: NotificationHelper
) {
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isNotificationsEnabled = true
    private var initialized = false
    
    fun initialize() {
        if (initialized) return
        initialized = true
        
        serviceScope.launch {
            // Get initial notification preference
            try {
                isNotificationsEnabled = settingsRepository.getNotificationPreference().first()
            } catch (e: Exception) {
                Log.e(TAG, "Error getting notification preference", e)
            }
            
            // Listen for changes to notification preferences
            try {
                settingsRepository.getNotificationPreference().collect { enabled ->
                    isNotificationsEnabled = enabled
                    Log.d(TAG, "Notification preference updated: $enabled")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error collecting notification preferences", e)
            }
        }
        
        // Listen for alerts
        serviceScope.launch {
            try {
                sensorRepository.getAlerts().collect { alerts ->
                    if (isNotificationsEnabled) {
                        processPendingAlerts(alerts)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error collecting alerts", e)
            }
        }
    }
    
    private val processedAlertIds = mutableSetOf<String>()
    
    private fun processPendingAlerts(alerts: List<Alert>) {
        alerts.forEach { alert ->
            // Skip if we've already processed this alert or if it's acknowledged
            if (alert.id in processedAlertIds || alert.isAcknowledged) {
                return@forEach
            }
            
            // Mark as processed and show notification
            processedAlertIds.add(alert.id)
            notificationHelper.showNotification(alert)
        }
    }
    
    fun acknowledgeAlert(alertId: String) {
        notificationHelper.clearNotification(alertId)
    }
} 