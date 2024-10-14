package com.example.routemapper.sensors.gyroscope

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class GyroscopeSensorDetector constructor(
    private val context: Context
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private var gyroscopeListener: GyroscopeListener? = null
    private var lastGyroscope: FloatArray? = null

    fun registerListener(gyroscopeListener: GyroscopeListener): Boolean {
        this.gyroscopeListener = gyroscopeListener

        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        return sensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        } ?: false
    }

    fun unregisterListener() {
        gyroscopeListener = null
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_GYROSCOPE) {
            val values = event.values.clone()
            lastGyroscope = values
            gyroscopeListener?.onGyroscopeChanged(values, System.currentTimeMillis())
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}