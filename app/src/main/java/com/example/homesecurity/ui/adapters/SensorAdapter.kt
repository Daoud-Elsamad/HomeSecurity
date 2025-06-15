package com.example.homesecurity.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.homesecurity.R
import com.example.homesecurity.databinding.ItemSensorBinding
import com.example.homesecurity.models.SensorData
import com.example.homesecurity.models.SensorType

class SensorAdapter(
    private val onLockToggle: (String, Boolean) -> Unit,
    private val onSensorToggle: (String, Boolean) -> Unit,
    private var showControls: Boolean = true
) : ListAdapter<SensorData, SensorAdapter.ViewHolder>(SensorDiffCallback()) {

    fun updateControlPermissions(showControls: Boolean) {
        this.showControls = showControls
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSensorBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemSensorBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(sensor: SensorData) {
            // Reset any existing animations
            binding.root.clearAnimation()
            binding.root.alpha = 1f
            binding.root.translationY = 0f
            
            binding.apply {
                // Set sensor name and type
                sensorName.text = getSensorName(sensor)
                sensorType.text = getSensorTypeDisplay(sensor.type)
                
                // Set sensor icon based on type
                sensorIcon.setImageResource(getSensorIcon(sensor.type))
                
                // Set sensor value (only show for non-door sensors)
                val formattedValue = formatSensorValue(sensor)
                if (formattedValue.isNotEmpty() && sensor.type != SensorType.DOOR) {
                    sensorValue.text = formattedValue
                    sensorValue.visibility = View.VISIBLE
                } else {
                    sensorValue.visibility = View.GONE
                }
                
                // Set sensor status
                sensorStatus.text = getSensorStatus(sensor)
                
                // Door lock toggle (only for door sensors)
                doorControls.isVisible = sensor.type == SensorType.DOOR && showControls
                
                // Only show enable switch if controls are visible
                enableSwitch.isVisible = showControls
                
                // Clear previous listeners to prevent cross-triggering
                enableSwitch.setOnCheckedChangeListener(null)
                
                // For door sensors, set up lock/unlock buttons
                if (sensor.type == SensorType.DOOR && showControls) {
                    // Clear previous click listeners
                    unlockButton.setOnClickListener(null)
                    lockButton.setOnClickListener(null)
                    
                    // Set up new click listeners
                    unlockButton.setOnClickListener {
                        onLockToggle.invoke(sensor.id, false) // false = unlocked
                    }
                    
                    lockButton.setOnClickListener {
                        onLockToggle.invoke(sensor.id, true) // true = locked
                    }
                }
                
                // Set enable switch state
                enableSwitch.isChecked = sensor.isEnabled
                
                // Set enable switch listener
                enableSwitch.setOnCheckedChangeListener { _, isChecked ->
                    onSensorToggle.invoke(sensor.id, isChecked)
                }
            }
        }
        
        private fun getSensorIcon(sensorType: SensorType): Int {
            return when (sensorType) {
                SensorType.DOOR -> R.drawable.ic_door_sensor
                SensorType.GAS -> R.drawable.ic_fire_alert
                SensorType.VIBRATION -> R.drawable.ic_vibration_sensor
                SensorType.ULTRASONIC -> R.drawable.ic_ultrasonic_sensor
                SensorType.NFC -> R.drawable.ic_nfc
            }
        }
        
        private fun getSensorTypeDisplay(sensorType: SensorType): String {
            return when (sensorType) {
                SensorType.DOOR -> "Door Sensor"
                SensorType.GAS -> "Gas Detection"
                SensorType.VIBRATION -> "Living Room"
                SensorType.ULTRASONIC -> "Entrance"
                SensorType.NFC -> "Main Door Access"
            }
        }
        
        private fun getSensorStatus(sensor: SensorData): String {
            return when (sensor.type) {
                SensorType.DOOR -> if (sensor.value > 0) "Open" else "Closed"
                SensorType.GAS -> if (sensor.value > 300) "Alert" else "Normal"
                SensorType.VIBRATION -> if (sensor.value > 0) "Active" else "Inactive"
                SensorType.ULTRASONIC -> "Active"
                SensorType.NFC -> "Ready"
            }
        }

        private fun formatSensorValue(sensor: SensorData): String {
            return when (sensor.type) {
                SensorType.GAS -> "${sensor.value}ppm"
                SensorType.DOOR -> "" // Status is shown separately
                SensorType.VIBRATION -> "${sensor.value}Hz"
                SensorType.ULTRASONIC -> "${sensor.value}cm"
                SensorType.NFC -> "" // No value needed
            }
        }

        private fun getSensorName(sensor: SensorData): String {
            return when (sensor.type) {
                SensorType.DOOR -> "Main Door"
                SensorType.GAS -> "Kitchen Gas Sensor"
                SensorType.VIBRATION -> "Vibration Sensor"
                SensorType.ULTRASONIC -> "Ultrasonic Sensor"
                SensorType.NFC -> "NFC Module"
            }
        }
    }

    class SensorDiffCallback : DiffUtil.ItemCallback<SensorData>() {
        override fun areItemsTheSame(oldItem: SensorData, newItem: SensorData): Boolean = 
            oldItem.id == newItem.id
            
        override fun areContentsTheSame(oldItem: SensorData, newItem: SensorData): Boolean = 
            oldItem == newItem
    }
} 