package com.example.routemapper.sensors.barometer

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class BarometerSensorDetector constructor(
    private val context: Context
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private var pressureListener: BarometerListener? = null
    private var lastPressure: Float = 0.0f

    fun registerListener(pressureListener: BarometerListener): Boolean {
        this.pressureListener = pressureListener

        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PRESSURE)
        return sensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        } ?: false
    }

    fun unregisterListener() {
        pressureListener = null
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_PRESSURE) {
            val pressure = event.values[0]
            lastPressure = pressure
            pressureListener?.onPressureChanged(pressure, System.currentTimeMillis())
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}