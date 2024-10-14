package com.example.routemapper.sensors.manager

import android.net.wifi.ScanResult
import android.net.wifi.rtt.RangingResult
import com.google.android.gms.maps.model.LatLng

interface CombinedSensorListener {
    fun onSensorDataChanged(
        pressure: Float?,
        acceleration: FloatArray?,
        gravity: FloatArray?,
        gyroscope: FloatArray?,
        rotation: FloatArray?,
        stepsDetected: Int,
        lastWifiScanResults: List<ScanResult>?,
        lastWifi80211ScanResults: List<ScanResult>?,
        lastRttResults: List<RangingResult>?,
        lastRttLocation: LatLng?,
        timestamp: Long
    )
}