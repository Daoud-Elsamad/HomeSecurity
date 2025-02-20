package com.example.homesecurity.ui.components

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.google.android.material.card.MaterialCardView
import com.example.homesecurity.R
import com.example.homesecurity.databinding.ViewSensorCardBinding
import com.example.homesecurity.models.SensorData
import com.example.homesecurity.models.SensorType
import com.example.homesecurity.models.SensorStatus

class SensorCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {

    private var binding: ViewSensorCardBinding
    private var currentStatus: SensorStatus = SensorStatus.NORMAL
    private var currentAnimator: ValueAnimator? = null

    init {
        binding = ViewSensorCardBinding.inflate(LayoutInflater.from(context), this, true)
        
        radius = 0f
        elevation = 0f
        strokeWidth = 0
        setCardBackgroundColor(android.graphics.Color.TRANSPARENT)
        
        // Set up ripple effect
        isClickable = true
        isFocusable = true
        
        // Initial setup
        strokeWidth = resources.getDimensionPixelSize(R.dimen.card_stroke_width)
        strokeColor = ContextCompat.getColor(context, R.color.status_normal)
    }

    private fun formatSensorValue(data: SensorData, animatedValue: Float? = null): String {
        val value = animatedValue?.toDouble() ?: data.value
        return when (data.type) {
            SensorType.GAS -> String.format("%.0f ppm", value)
            SensorType.DOOR -> if (value > 0) "Open" else "Closed"
            SensorType.VIBRATION -> String.format("%.1f Hz", value)
            SensorType.ULTRASONIC -> String.format("%.0f cm", value)
            SensorType.NFC -> "NFC Module"
        }
    }

    fun setSensorData(data: SensorData) {
        binding.apply {
            // Update name
            sensorName.text = data.location

            // Update icon
            sensorIcon.setImageResource(getSensorIcon(data.type))

            // Update value
            sensorValue.text = formatSensorValue(data)

            // Show/hide lock controls for door sensors
            lockText.visibility = if (data.type == SensorType.DOOR) VISIBLE else GONE
            lockSwitch.visibility = if (data.type == SensorType.DOOR) VISIBLE else GONE
            if (data.type == SensorType.DOOR) {
                lockSwitch.isChecked = data.isLocked ?: false
            }

            // Update enable switch
            enableSwitch.isChecked = data.isEnabled

            // Update status only if changed
            if (currentStatus != data.status) {
                currentStatus = data.status
                strokeColor = ContextCompat.getColor(context, getStatusColor(data.status))
            }
        }
    }

    private fun animateStatusChange(newStatus: SensorStatus) {
        // Animate elevation change
        ValueAnimator.ofFloat(elevation, elevation * 1.5f).apply {
            duration = 150
            addUpdateListener { animator ->
                elevation = animator.animatedValue as Float
            }
            start()
        }

        // Animate stroke color change
        val colorFrom = strokeColor
        val colorTo = ContextCompat.getColor(context, getStatusColor(newStatus))
        ValueAnimator.ofArgb(colorFrom, colorTo).apply {
            duration = 300
            addUpdateListener { animator ->
                strokeColor = animator.animatedValue as Int
            }
            start()
        }

        // Reset elevation after delay
        postDelayed({
            animate()
                .setDuration(150)
                .setInterpolator(FastOutSlowInInterpolator())
                .translationZ(0f)
                .start()
        }, 300)
    }

    private fun getSensorIcon(type: SensorType): Int {
        return when (type) {
            SensorType.GAS -> R.drawable.ic_gas_sensor
            SensorType.DOOR -> R.drawable.ic_door_sensor
            SensorType.VIBRATION -> R.drawable.ic_vibration_sensor
            SensorType.ULTRASONIC -> R.drawable.ic_ultrasonic_sensor
            SensorType.NFC -> R.drawable.ic_nfc_module
        }
    }

    private fun getStatusColor(status: SensorStatus): Int {
        return when (status) {
            SensorStatus.NORMAL -> R.color.status_normal
            SensorStatus.WARNING -> R.color.status_warning
            SensorStatus.ALERT -> R.color.status_alert
            SensorStatus.DISCONNECTED -> R.color.status_disconnected
        }
    }
} 