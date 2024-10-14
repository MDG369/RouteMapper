package com.example.routemapper.sensors.accelerometer

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class AccelerometerSensorDetector constructor(
    private val context: Context
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private var accelerometerListener: AccelerometerListener? = null
    private var lastAcceleration: FloatArray? = null

    fun registerListener(accelerometerListener: AccelerometerListener): Boolean {
        this.accelerometerListener = accelerometerListener

        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        return sensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        } ?: false
    }

    fun unregisterListener() {
        accelerometerListener = null
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val values = event.values.clone()
            lastAcceleration = values
            accelerometerListener?.onAccelerationChanged(values, System.currentTimeMillis())
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

}