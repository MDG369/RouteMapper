package com.example.routemapper.stephandling.lstm

import android.app.Activity
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import com.example.routemapper.stephandling.StepListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicBoolean

class LSTMStepDetector(
    private val context: Activity,
    private val model: Module,
    private val windowSize: Int = 160,
    private val samplingRate: Int = 50
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Activity.SENSOR_SERVICE) as SensorManager
    private var stepListener: StepListener? = null

    // Bufory na dane z sensorów
    private val accelBuffer = ArrayDeque<FloatArray>()
    private val gyroBuffer = ArrayDeque<FloatArray>()

    // Wątek roboczy
    private val handlerThread = HandlerThread("SensorProcessingThread")
    private lateinit var handler: Handler
    private val mainHandler = Handler(Looper.getMainLooper())

    // Flaga do zapobiegania równoległemu przetwarzaniu
    private val isProcessing = AtomicBoolean(false)

    // Znaczniki czasu
    private var lastStepTimestamp: Long = 0
    private val stepCooldownMs = 400

    // Flaga rejestracji sensorów
    private var sensorsRegistered = false

    // Scope dla coroutines
    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        handlerThread.start()
        handler = Handler(handlerThread.looper)
    }

    fun registerListener(stepListener: StepListener): Boolean {
        this.stepListener = stepListener
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        if (accelerometer != null && gyroscope != null) {
            val sensorDelay = SensorManager.SENSOR_DELAY_GAME
            val accelSuccess = sensorManager.registerListener(this, accelerometer, sensorDelay, handler)
            val gyroSuccess = sensorManager.registerListener(this, gyroscope, sensorDelay, handler)
            sensorsRegistered = accelSuccess && gyroSuccess
            return sensorsRegistered
        }
        return false
    }

    fun unregisterListener() {
        if (sensorsRegistered) {
            sensorManager.unregisterListener(this)
            sensorsRegistered = false
        }
        stepListener = null
        synchronized(accelBuffer) { accelBuffer.clear() }
        synchronized(gyroBuffer) { gyroBuffer.clear() }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val data = floatArrayOf(event.values[0], event.values[1], event.values[2])
                synchronized(accelBuffer) {
                    accelBuffer.addLast(data)
                    if (accelBuffer.size > windowSize) accelBuffer.removeFirst()
                }
            }
            Sensor.TYPE_GYROSCOPE -> {
                val data = floatArrayOf(event.values[0], event.values[1], event.values[2])
                synchronized(gyroBuffer) {
                    gyroBuffer.addLast(data)
                    if (gyroBuffer.size > windowSize) gyroBuffer.removeFirst()
                }
            }
        }

        // Przetwarzaj tylko jeśli oba bufory są pełne i nie trwa już przetwarzanie
        if (accelBuffer.size == windowSize && gyroBuffer.size == windowSize && isProcessing.compareAndSet(false, true)) {
            val accelCopy = synchronized(accelBuffer) { accelBuffer.toList() }
            val gyroCopy = synchronized(gyroBuffer) { gyroBuffer.toList() }
            val currentTime = System.currentTimeMillis()
            scope.launch {
                processWindow(accelCopy, gyroCopy, currentTime)
                isProcessing.set(false)
            }
        }
    }

    private suspend fun processWindow(accel: List<FloatArray>, gyro: List<FloatArray>, currentTime: Long) {
        withContext(Dispatchers.Default) {
            val input = FloatArray(windowSize * 6)
            for (i in 0 until windowSize) {
                val a = accel[i]
                val g = gyro[i]
                input[i * 6] = a[0]
                input[i * 6 + 1] = a[1]
                input[i * 6 + 2] = a[2]
                input[i * 6 + 3] = g[0]
                input[i * 6 + 4] = g[1]
                input[i * 6 + 5] = g[2]
            }
            val inputTensor = Tensor.fromBlob(input, longArrayOf(1, windowSize.toLong(), 6))
            val output = model.forward(IValue.from(inputTensor)).toTensor().dataAsFloatArray
            val prediction = if (output[0] > 0.5f) 1 else 0

            // Detekcja kroku z cooldownem
            if (prediction == 1 && currentTime - lastStepTimestamp > stepCooldownMs) {
                lastStepTimestamp = currentTime
                withContext(Dispatchers.Main) {
                    stepListener?.onStep(1)
                }
                Log.d("LSTMStepDetector", "Step detected!")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun onDestroy() {
        handlerThread.quitSafely()
    }
}