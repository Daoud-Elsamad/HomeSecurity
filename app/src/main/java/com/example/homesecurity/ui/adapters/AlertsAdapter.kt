package com.example.homesecurity.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.homesecurity.R
import com.example.homesecurity.databinding.ItemAlertBinding
import com.example.homesecurity.models.Alert
import com.example.homesecurity.models.AlertType
import java.text.SimpleDateFormat
import java.util.Locale

class AlertsAdapter : ListAdapter<Alert, AlertsAdapter.ViewHolder>(AlertDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAlertBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemAlertBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())

        fun bind(alert: Alert) {
            binding.apply {
                alertTitle.text = getAlertTitle(alert.type)
                alertMessage.text = alert.message
                timestamp.text = dateFormat.format(alert.timestamp)
                alertIcon.setImageResource(getAlertIcon(alert.type))
            }
        }

        private fun getAlertTitle(type: AlertType): String = when(type) {
            AlertType.GAS_LEAK -> "Gas Leak Alert"
            AlertType.DOOR_UNAUTHORIZED -> "Unauthorized Door Access"
            AlertType.DOOR_LEFT_OPEN -> "Door Alert"
            AlertType.VIBRATION_DETECTED -> "Vibration Alert"
            AlertType.NFC_UNAUTHORIZED -> "Unauthorized NFC Access"
        }

        private fun getAlertIcon(type: AlertType): Int = when(type) {
            AlertType.GAS_LEAK -> R.drawable.ic_gas_alert
            AlertType.DOOR_UNAUTHORIZED, AlertType.DOOR_LEFT_OPEN -> R.drawable.ic_door_alert
            AlertType.VIBRATION_DETECTED -> R.drawable.ic_vibration_alert
            AlertType.NFC_UNAUTHORIZED -> R.drawable.ic_nfc_alert
        }
    }

    class AlertDiffCallback : DiffUtil.ItemCallback<Alert>() {
        override fun areItemsTheSame(oldItem: Alert, newItem: Alert): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Alert, newItem: Alert): Boolean =
            oldItem == newItem
    }
} 