package com.example.routemapper

import android.app.Activity
import android.content.Context
import android.content.Context.MODE_APPEND
import android.content.Context.MODE_PRIVATE
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import java.io.FileOutputStream
import java.io.OutputStreamWriter


class StepSensorDetector constructor(
    private val context: Activity,
) : SensorEventListener {

    private val REQUEST_ACTIVITY_RECOGNITION: Int = 100;
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private var stepListener: StepListener? = null
    private var outputStreamWriter: OutputStreamWriter? = null


    fun registerListener(stepListener: StepListener): Boolean {
        this.stepListener = stepListener

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasPermission = ActivityCompat.checkSelfPermission(
                context,
                "android.permission.ACTIVITY_RECOGNITION",
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                ActivityCompat.requestPermissions(context,
                    arrayOf("android.permission.ACTIVITY_RECOGNITION"),
                    REQUEST_ACTIVITY_RECOGNITION);
            }
        }

        val stepDetectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        if (stepDetectorSensor != null) {
            return sensorManager?.registerListener(
                /* listener = */ this@StepSensorDetector,
                /* sensor = */ stepDetectorSensor,
                /* samplingPeriodUs = */ SensorManager.SENSOR_DELAY_FASTEST,
            ) ?: false
        }



        return false
    }

    fun unregisterListener() {
        stepListener = null
        sensorManager?.unregisterListener(this@StepSensorDetector)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_DETECTOR) {
            stepListener?.onStep(count = 1)
            val file: FileOutputStream = context.openFileOutput("step-timestamps.txt", MODE_APPEND)
            outputStreamWriter = OutputStreamWriter(file)
            outputStreamWriter?.write(event.timestamp.toString() + "\n")
            outputStreamWriter?.flush()
            outputStreamWriter?.close()
        }
    }

    fun saveStepToFile(userId: Int, heading: Double) {
        val file: FileOutputStream = context.openFileOutput("step-directions.txt", MODE_APPEND)
        outputStreamWriter = OutputStreamWriter(file)
        outputStreamWriter?.write("$userId,$heading\n")
        outputStreamWriter?.flush()
        outputStreamWriter?.close()
    }

    private fun mod(a: Double, b: Double): Double {
        return a % b
    }


    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }
}