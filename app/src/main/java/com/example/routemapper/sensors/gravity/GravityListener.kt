package com.example.routemapper.sensors.gravity

interface GravityListener {
    fun onGravityChanged(gravity: FloatArray, timestamp: Long)
}