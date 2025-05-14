package com.example.homesecurity.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.homesecurity.MainActivity
import com.example.homesecurity.R
import com.example.homesecurity.models.Alert
import com.example.homesecurity.models.AlertType
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val CHANNEL_ID = "home_security_alerts"
        private const val ALERT_GROUP = "home_security_alert_group"
        private const val SUMMARY_ID = 0
        
        // Minimum time between gas notifications (in milliseconds) - 5 minutes
        private const val GAS_NOTIFICATION_THROTTLE_TIME = 5 * 60 * 1000L
    }
    
    // Keep track of the last notification time for each sensor
    private val lastGasNotificationTime = ConcurrentHashMap<String, Long>()
    // Keep track of already shown notification IDs to avoid duplicates
    private val shownNotificationIds = ConcurrentHashMap.newKeySet<String>()
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Security Alerts"
            val descriptionText = "Notifications for home security alerts"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    fun showNotification(alert: Alert) {
        // For GAS_LEAK alerts, throttle notifications from the same sensor
        if (alert.type == AlertType.GAS_LEAK) {
            val lastTime = lastGasNotificationTime[alert.sensorId] ?: 0
            val currentTime = System.currentTimeMillis()
            
            if (currentTime - lastTime < GAS_NOTIFICATION_THROTTLE_TIME) {
                // Skip this notification if we recently showed one for this sensor
                return
            }
            
            // Update the last notification time for this sensor
            lastGasNotificationTime[alert.sensorId] = currentTime
        }
        
        // Check if we've already shown this specific alert
        if (!shownNotificationIds.add(alert.id)) {
            // We've already shown this alert, skip
            return
        }
        
        // Create an intent that opens the MainActivity when clicked
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Get appropriate icon and title for this alert type
        val (icon, title) = getAlertIconAndTitle(alert.type)
        
        // Build the notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(alert.message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setGroup(ALERT_GROUP)
            .setAutoCancel(true)
        
        // Show the notification
        with(NotificationManagerCompat.from(context)) {
            notify(alert.id.hashCode(), builder.build())
            
            // Show summary notification for Android 7.0+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val summaryNotification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setContentTitle("Security Alerts")
                    .setContentText("Multiple security alerts detected")
                    .setSmallIcon(R.drawable.ic_notifications)
                    .setGroup(ALERT_GROUP)
                    .setGroupSummary(true)
                    .setAutoCancel(true)
                    .build()
                
                notify(SUMMARY_ID, summaryNotification)
            }
        }
    }
    
    private fun getAlertIconAndTitle(type: AlertType): Pair<Int, String> {
        return when (type) {
            AlertType.GAS_LEAK -> Pair(R.drawable.ic_gas_alert, "Gas Leak Alert")
            AlertType.DOOR_UNAUTHORIZED -> Pair(R.drawable.ic_door_alert, "Unauthorized Door Access")
            AlertType.DOOR_LEFT_OPEN -> Pair(R.drawable.ic_door_alert, "Door Alert")
            AlertType.VIBRATION_DETECTED -> Pair(R.drawable.ic_vibration_alert, "Vibration Alert")
            AlertType.NFC_UNAUTHORIZED -> Pair(R.drawable.ic_nfc_alert, "Unauthorized NFC Access")
            AlertType.PROXIMITY -> Pair(R.drawable.ic_proximity_alert, "Proximity Alert")
            AlertType.FIRE -> Pair(R.drawable.ic_fire_alert, "Fire Alert")
        }
    }
    
    fun clearNotification(alertId: String) {
        with(NotificationManagerCompat.from(context)) {
            cancel(alertId.hashCode())
            shownNotificationIds.remove(alertId)
        }
    }
    
    fun clearAllNotifications() {
        with(NotificationManagerCompat.from(context)) {
            cancelAll()
            shownNotificationIds.clear()
        }
    }
} 