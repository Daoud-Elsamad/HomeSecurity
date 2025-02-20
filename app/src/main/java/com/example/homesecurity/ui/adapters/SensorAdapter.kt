package com.example.homesecurity.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.homesecurity.databinding.ItemSensorBinding
import com.example.homesecurity.models.SensorData
import com.example.homesecurity.models.SensorType

class SensorAdapter(
    private val onLockToggle: (String, Boolean) -> Unit,
    private val onSensorToggle: (String, Boolean) -> Unit
) : ListAdapter<SensorData, SensorAdapter.ViewHolder>(SensorDiffCallback()) {

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
                sensorName.text = "${sensor.type.name} - ${sensor.location}"
                sensorValue.text = formatSensorValue(sensor)
                
                // Door lock toggle (only for door sensors)
                doorControls.isVisible = sensor.type == SensorType.DOOR
                
                // Clear previous listeners to prevent cross-triggering
                lockSwitch.setOnCheckedChangeListener(null)
                enableSwitch.setOnCheckedChangeListener(null)
                
                // Set switch states before adding listeners
                if (sensor.type == SensorType.DOOR) {
                    lockSwitch.isChecked = sensor.isLocked
                }
                enableSwitch.isChecked = sensor.isEnabled
                
                // Add listeners after setting states
                if (sensor.type == SensorType.DOOR) {
                    lockSwitch.setOnCheckedChangeListener { _, isChecked ->
                        onLockToggle.invoke(sensor.id, isChecked)
                    }
                }
                
                enableSwitch.setOnCheckedChangeListener { _, isChecked ->
                    onSensorToggle.invoke(sensor.id, isChecked)
                }
            }
        }

        private fun formatSensorValue(sensor: SensorData): String {
            return when (sensor.type) {
                SensorType.GAS -> "${sensor.value} ppm"
                SensorType.DOOR -> if (sensor.value > 0) "Open" else "Closed"
                SensorType.VIBRATION -> "${sensor.value} Hz"
                SensorType.ULTRASONIC -> "${sensor.value} cm"
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