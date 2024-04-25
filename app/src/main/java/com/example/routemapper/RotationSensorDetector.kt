package com.example.routemapper

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

class RotationSensorDetector constructor(
    private val context: Context,
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private var rotationListener: RotationListener? = null
    private var lastHeading: Double = 0.0

    fun registerListener(rotationListener: RotationListener): Boolean {
        this.rotationListener = rotationListener

        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (sensor != null) {
            return sensorManager?.registerListener(
                /* listener = */ this@RotationSensorDetector,
                /* sensor = */ sensor,
                /* samplingPeriodUs = */ SensorManager.SENSOR_DELAY_FASTEST,
            ) ?: false
        }

        return false
    }

    fun unregisterListener() {
        rotationListener = null
        sensorManager?.unregisterListener(this@RotationSensorDetector)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {

            val heading = getHeading(event);
            rotationListener?.onRotation(heading.toFloat())
        }
    }

    fun getHeading(event: SensorEvent?): Double {
        val orientation = FloatArray(3)
        val rotationMatrix = FloatArray(9)

        SensorManager.getRotationMatrixFromVector(rotationMatrix, event?.values)
        SensorManager.getOrientation(rotationMatrix, orientation)
        val heading = mod(orientation[0] + Math.PI*2, Math.PI*2) //important
        this.lastHeading = heading;
//        Log.e("HEADING", "heading: $heading")
        return heading
    }

    fun getLastHeading(): Double {
        return this.lastHeading;
    }

    private fun mod(a: Double, b: Double): Double {
        return a % b
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }
}