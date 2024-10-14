package com.example.routemapper.sensors.gravity

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class GravitySensorDetector constructor(
    private val context: Context
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private var gravityListener: GravityListener? = null
    private var lastGravity: FloatArray? = null

    fun registerListener(gravityListener: GravityListener): Boolean {
        this.gravityListener = gravityListener

        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
        return sensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        } ?: false
    }

    fun unregisterListener() {
        gravityListener = null
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_GRAVITY) {
            val values = event.values.clone()
            lastGravity = values
            gravityListener?.onGravityChanged(values, System.currentTimeMillis())
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}