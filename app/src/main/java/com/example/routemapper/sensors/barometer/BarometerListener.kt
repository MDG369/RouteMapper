package com.example.routemapper.sensors.barometer

interface BarometerListener {
    fun onPressureChanged(pressure: Float, timestamp: Long)
}