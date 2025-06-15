package com.example.homesecurity.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.homesecurity.databinding.ItemAccessLogBinding
import com.example.homesecurity.models.ActivityLog
import com.example.homesecurity.models.LogType
import java.text.SimpleDateFormat
import java.util.Locale

class AccessLogsAdapter : ListAdapter<ActivityLog, AccessLogsAdapter.ViewHolder>(AccessLogDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAccessLogBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemAccessLogBinding) : RecyclerView.ViewHolder(binding.root) {
        private val dateFormat = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())

        fun bind(log: ActivityLog) {
            binding.apply {
                timestamp.text = dateFormat.format(log.timestamp)
                logType.text = when(log.type) {
                    LogType.NFC_ACCESS -> "NFC Access"
                    LogType.SENSOR_EVENT -> "Sensor Event"
                    LogType.DOOR_ENTRY -> "Door Entry"
                    LogType.DOOR_EXIT -> "Door Exit"
                    LogType.UNAUTHORIZED_ACCESS -> "Unauthorized Access"
                }
                description.text = log.description
                
                // Set appropriate text color based on log type
                val textColor = when(log.type) {
                    LogType.DOOR_ENTRY -> itemView.context.getColor(android.R.color.holo_green_dark)
                    LogType.DOOR_EXIT -> itemView.context.getColor(android.R.color.holo_blue_dark)
                    LogType.UNAUTHORIZED_ACCESS -> itemView.context.getColor(android.R.color.holo_red_dark)
                    LogType.NFC_ACCESS -> itemView.context.getColor(android.R.color.holo_purple)
                    else -> itemView.context.getColor(android.R.color.black)
                }
                description.setTextColor(textColor)
                logType.setTextColor(textColor)
            }
        }
    }

    class AccessLogDiffCallback : DiffUtil.ItemCallback<ActivityLog>() {
        override fun areItemsTheSame(oldItem: ActivityLog, newItem: ActivityLog): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ActivityLog, newItem: ActivityLog): Boolean =
            oldItem == newItem
    }
} 