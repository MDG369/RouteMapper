package com.example.routemapper.sensors.accelerometer

interface AccelerometerListener {
    fun onAccelerationChanged(acceleration: FloatArray, timestamp: Long)
}