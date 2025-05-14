package com.example.homesecurity.ui.adapters

import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.homesecurity.R
import com.example.homesecurity.databinding.ItemAlertBinding
import com.example.homesecurity.models.Alert
import com.example.homesecurity.models.AlertType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "AlertsAdapter"

class AlertsAdapter(
    private val onAlertClick: (String) -> Unit
) : ListAdapter<Alert, AlertsAdapter.ViewHolder>(AlertDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAlertBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onAlertClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Log.d(TAG, "Binding alert at position $position")
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemAlertBinding,
        private val onAlertClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
        private val shortDateFormat = SimpleDateFormat("MMM dd HH:mm", Locale.getDefault())

        fun bind(alert: Alert) {
            Log.d(TAG, "Binding alert: ${alert.id}, type: ${alert.type}, acknowledged: ${alert.isAcknowledged}")
            
            // Format timestamp with error handling
            val formattedTimestamp = try {
                // Ensure timestamp is valid
                val timestamp = if (alert.timestamp < 1577836800000L) { // Jan 1, 2020
                    // If timestamp is too old, it might be in seconds instead of milliseconds
                    if (alert.timestamp < 2000000000L) {
                        alert.timestamp * 1000 // Convert seconds to milliseconds
                    } else {
                        System.currentTimeMillis() // Use current time as fallback
                    }
                } else {
                    alert.timestamp
                }
                
                // Use a shorter date format for better display
                shortDateFormat.format(Date(timestamp))
            } catch (e: Exception) {
                Log.e(TAG, "Error formatting timestamp: ${alert.timestamp}", e)
                "Unknown date"
            }
            
            Log.d(TAG, "Alert timestamp: ${alert.timestamp} -> $formattedTimestamp")
            
            binding.apply {
                alertTitle.text = getAlertTitle(alert.type)
                alertMessage.text = alert.message
                timestamp.text = formattedTimestamp
                alertIcon.setImageResource(getAlertIcon(alert.type))
                
                // Set up click listener
                root.setOnClickListener {
                    onAlertClick(alert.id)
                }
                
                // Visual indication for acknowledged alerts
                if (alert.isAcknowledged) {
                    // Show acknowledged icon
                    acknowledgedIcon.visibility = View.VISIBLE
                    
                    // Dim the whole item
                    root.alpha = 0.8f
                    
                    // Set background to indicate acknowledged
                    root.setCardBackgroundColor(ContextCompat.getColor(root.context, R.color.acknowledged_alert_bg))
                } else {
                    // Hide acknowledged icon
                    acknowledgedIcon.visibility = View.GONE
                    
                    // Reset to normal state
                    root.alpha = 1.0f
                    root.setCardBackgroundColor(ContextCompat.getColor(root.context, R.color.white))
                }
            }
        }

        private fun getAlertTitle(type: AlertType): String = when(type) {
            AlertType.GAS_LEAK -> "Gas Leak Alert"
            AlertType.DOOR_UNAUTHORIZED -> "Unauthorized Door Access"
            AlertType.DOOR_LEFT_OPEN -> "Door Alert"
            AlertType.VIBRATION_DETECTED -> "Vibration Alert"
            AlertType.NFC_UNAUTHORIZED -> "Unauthorized NFC Access"
            AlertType.PROXIMITY -> "Proximity Alert"
            AlertType.FIRE -> "Fire Alert"
        }

        private fun getAlertIcon(type: AlertType): Int = when(type) {
            AlertType.GAS_LEAK -> R.drawable.ic_gas_alert
            AlertType.DOOR_UNAUTHORIZED, AlertType.DOOR_LEFT_OPEN -> R.drawable.ic_door_alert
            AlertType.VIBRATION_DETECTED -> R.drawable.ic_vibration_alert
            AlertType.NFC_UNAUTHORIZED -> R.drawable.ic_nfc_alert
            AlertType.PROXIMITY -> R.drawable.ic_proximity_alert
            AlertType.FIRE -> R.drawable.ic_fire_alert
        }
    }

    class AlertDiffCallback : DiffUtil.ItemCallback<Alert>() {
        override fun areItemsTheSame(oldItem: Alert, newItem: Alert): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Alert, newItem: Alert): Boolean =
            oldItem == newItem
    }
} 