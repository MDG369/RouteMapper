package com.example.routemapper.sensors.gyroscope

interface GyroscopeListener {
    fun onGyroscopeChanged(gyroscope: FloatArray, timestamp: Long)
}