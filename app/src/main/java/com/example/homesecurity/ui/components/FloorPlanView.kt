package com.example.homesecurity.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.content.ContextCompat
import com.example.homesecurity.R
import com.example.homesecurity.models.SensorData
import com.example.homesecurity.models.SensorStatus
import com.example.homesecurity.models.SensorType
import kotlin.math.max
import kotlin.math.min

class FloorPlanView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val floorPlanPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.floor_plan_wall)
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }

    private val sensorPaint = Paint().apply {
        style = Paint.Style.FILL
    }

    private val textPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.floor_plan_wall)
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }

    private var sensors: List<SensorData> = emptyList()
    private var floorPlanPath = Path()
    private var scaleFactor = 1f
    private var translateX = 0f
    private var translateY = 0f

    // Add gesture detectors
    private val scaleDetector = ScaleGestureDetector(context, ScaleListener())
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false
    
    // Add bounds for pan and zoom
    private var minScale = 0.5f
    private var maxScale = 3.0f
    private var boundX = 0f
    private var boundY = 0f

    // Add after existing properties
    private var onSensorClick: ((SensorData) -> Unit)? = null

    init {
        setupFloorPlan()
    }

    private fun setupFloorPlan() {
        floorPlanPath.apply {
            // Main outline (scaled based on actual measurements)
            moveTo(100f, 100f)
            lineTo(900f, 100f)    // Top wall
            lineTo(900f, 600f)    // Right wall
            lineTo(100f, 600f)    // Bottom wall
            lineTo(100f, 100f)    // Left wall
            
            // Garage (33.2 m²)
            moveTo(300f, 100f)
            lineTo(300f, 600f)
            
            // Bedroom (10 m²) and Bedroom 2 (10 m²)
            moveTo(300f, 250f)
            lineTo(500f, 250f)
            
            // Bathroom (4.2 m²)
            moveTo(500f, 100f)
            lineTo(500f, 250f)
            
            // Kitchen (14 m²)
            moveTo(700f, 250f)
            lineTo(900f, 250f)
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.save()
        
        // Apply transformations
        canvas.translate(translateX, translateY)
        canvas.scale(scaleFactor, scaleFactor)
        
        // Draw floor plan
        canvas.drawPath(floorPlanPath, floorPlanPaint)
        
        // Draw room labels with adjusted text size based on scale
        textPaint.textSize = 24f / scaleFactor
        
        // Draw room labels
        canvas.drawText("Garage", 200f, 300f, textPaint)
        canvas.drawText("Bedroom", 400f, 200f, textPaint)
        canvas.drawText("Bathroom", 600f, 200f, textPaint)
        canvas.drawText("Bedroom 2", 800f, 200f, textPaint)
        canvas.drawText("Living", 400f, 400f, textPaint)
        canvas.drawText("Room", 400f, 430f, textPaint)
        canvas.drawText("Kitchen", 800f, 400f, textPaint)
        canvas.drawText("Hall", 600f, 400f, textPaint)
        
        // Draw sensors
        sensors.forEach { sensor ->
            sensorPaint.color = getSensorColor(sensor.status)
            val (x, y) = getSensorPosition(sensor)
            canvas.drawCircle(x, y, 20f, sensorPaint)
            drawSensorIcon(canvas, sensor, x, y)
        }
        
        canvas.restore()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val planWidth = 800f
        scaleFactor = min((w - 32f) / planWidth, (h - 32f) / 600f)
        boundX = w.toFloat()
        boundY = h.toFloat()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        // Pan and zoom functionality
        // Sensor click detection
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                isDragging = true

                // Check for sensor clicks
                val touchX = (event.x - translateX) / scaleFactor
                val touchY = (event.y - translateY) / scaleFactor
                
                sensors.forEach { sensor ->
                    val (sensorX, sensorY) = getSensorPosition(sensor)
                    val distance = Math.hypot(
                        (touchX - sensorX).toDouble(),
                        (touchY - sensorY).toDouble()
                    )
                    if (distance < 30f) {
                        onSensorClick?.invoke(sensor)
                        return true
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging && !scaleDetector.isInProgress) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    
                    translateX += dx
                    translateY += dy
                    
                    lastTouchX = event.x
                    lastTouchY = event.y
                    
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
            }
        }
        return true
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            // Constrain scale within bounds
            scaleFactor = min(max(scaleFactor * detector.scaleFactor, minScale), maxScale)
            invalidate()
            return true
        }
    }

    fun updateSensors(newSensors: List<SensorData>) {
        sensors = newSensors
        invalidate()
    }

    private fun getSensorColor(status: SensorStatus): Int {
        return ContextCompat.getColor(context, when(status) {
            SensorStatus.NORMAL -> R.color.status_normal
            SensorStatus.WARNING -> R.color.status_warning
            SensorStatus.ALERT -> R.color.status_alert
            SensorStatus.DISCONNECTED -> R.color.status_disconnected
        })
    }

    private fun getSensorPosition(sensor: SensorData): Pair<Float, Float> {
        // Map sensor locations based on the actual floor plan
        return when(sensor.location) {
            "Kitchen" -> 800f to 400f      // MQ6 Gas sensor location
            "Living Room" -> 400f to 350f   // Vibration sensor location
            "Main Door" -> 600f to 550f     // Main door with NFC module
            "Bedroom Door" -> 400f to 100f  // Bedroom door sensor
            "Bedroom 2 Door" -> 700f to 200f // Bedroom 2 door sensor
            else -> 0f to 0f
        }
    }

    private fun drawSensorIcon(canvas: Canvas, sensor: SensorData, x: Float, y: Float) {
        val icon = when(sensor.type) {
            SensorType.GAS -> R.drawable.ic_gas_sensor
            SensorType.DOOR -> R.drawable.ic_door_sensor
            SensorType.VIBRATION -> R.drawable.ic_vibration_sensor
            SensorType.NFC -> R.drawable.ic_nfc_module
            else -> null
        }
        
        icon?.let {
            ContextCompat.getDrawable(context, it)?.let { drawable ->
                drawable.setBounds(
                    (x - 15).toInt(),
                    (y - 15).toInt(),
                    (x + 15).toInt(),
                    (y + 15).toInt()
                )
                drawable.draw(canvas)
            }
        }
    }

    fun setOnSensorClickListener(listener: (SensorData) -> Unit) {
        onSensorClick = listener
    }
} 