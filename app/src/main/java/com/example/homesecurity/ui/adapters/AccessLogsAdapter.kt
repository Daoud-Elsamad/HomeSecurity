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
                }
                description.text = log.description
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