package com.example.routemapper.sensors.manager

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.LocationManager
import android.net.MacAddress
import android.net.wifi.rtt.RangingRequest
import android.net.wifi.rtt.RangingResult
import android.net.wifi.rtt.RangingResultCallback
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.model.LatLng
import kotlin.math.pow
import android.net.wifi.rtt.WifiRttManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Environment
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class CombinedSensorManager constructor(
    private val context: Context
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private var sensorListener: CombinedSensorListener? = null

    private var lastPressure: Float? = null
    private var lastAcceleration: FloatArray? = null
    private var lastGravity: FloatArray? = null
    private var lastGyroscope: FloatArray? = null
    private var lastRotation: FloatArray? = null
    private var lastWifiScanResults: List<ScanResult>? = null
    private var lastWifi80211ScanResults: List<ScanResult>? = null
    private var lastRttResults: List<RangingResult>? = null    // Store Wi-Fi RTT results
    private var lastRttLocation: LatLng? = null
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private var locationAccuracy: Float? = null  // Store location accuracy in meters

    private var stepsDetected: Int = 0  // Keep track of steps
    private var stepsCounted: Int = 0  // Keep track of steps
    private var initialStepCount: Float? = null
    private val handler = Handler(Looper.getMainLooper())
    private val loggingInterval: Long = 1
    private lateinit var wifiRttManager: WifiRttManager
    private lateinit var wifiManager: WifiManager
    private lateinit var rangingRequest: RangingRequest

    // Start sensors and register listeners
    fun registerListener(listener: CombinedSensorListener): Boolean {
        this.sensorListener = listener

        val sensorsToRegister = listOf(
            Sensor.TYPE_PRESSURE,
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_GRAVITY,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_ROTATION_VECTOR,
            Sensor.TYPE_STEP_DETECTOR,
        )
        getGpsLocationAccuracy()

        var success = true
        sensorsToRegister.forEach { sensorType ->
            val sensor = sensorManager?.getDefaultSensor(sensorType)
            if (sensor != null) {
                success = sensorManager?.registerListener(
                    this, sensor, SensorManager.SENSOR_DELAY_FASTEST
                ) ?: false
            } else {
                Log.i("SENSOR", "No sensor found for $sensorType")
            }
        }
        wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        startWifiRtt();
        // Start a handler to save data every 500ms
        handler.postDelayed(sensorDataLogger, loggingInterval)

        return success
    }


    private fun getGpsLocationAccuracy() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        locationManager?.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            1000L,  // Minimum time interval between updates in milliseconds
            1f      // Minimum distance between updates in meters
        ) { location ->
            locationAccuracy = location.accuracy
            lastRttLocation = LatLng(location.latitude, location.longitude) // Update with GPS location
        }
    }

    // Stop listening and clear resources
    fun unregisterListener() {
        sensorManager?.unregisterListener(this)
        handler.removeCallbacks(sensorDataLogger)
        sensorListener = null
    }

    // Handle sensor changes for all registered sensors
    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_PRESSURE -> {
                lastPressure = event.values[0]
            }

            Sensor.TYPE_ACCELEROMETER -> {
                lastAcceleration = event.values.clone()
            }

            Sensor.TYPE_GRAVITY -> {
                lastGravity = event.values.clone()
            }

            Sensor.TYPE_GYROSCOPE -> {
                lastGyroscope = event.values.clone()
            }

            Sensor.TYPE_ROTATION_VECTOR -> {
                lastRotation = event.values.clone()
            }

            Sensor.TYPE_STEP_DETECTOR -> {
                stepsDetected++
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // Task to log sensor data every 500ms
    private val sensorDataLogger = object : Runnable {
        override fun run() {
            val timestamp = System.currentTimeMillis()

            // Log or save the sensor data with a timestamp
            sensorListener?.onSensorDataChanged(
                lastPressure,
                lastAcceleration,
                lastGravity,
                lastGyroscope,
                lastRotation,
                stepsDetected,
                stepsCounted,
                lastWifiScanResults,
                lastWifi80211ScanResults,
                lastRttResults,
                lastRttLocation,
                timestamp,
                locationAccuracy
            )

            // Schedule the next logging run after 500ms
            handler.postDelayed(this, loggingInterval)
        }
    }

    private fun startWifiRtt() {
        // Get a list of nearby Wi-Fi APs capable of RTT
        wifiManager.startScan()
        this.lastWifiScanResults = wifiManager.scanResults  // Get scan results
        wifiManager.scanResults.forEachIndexed { index, scanResult ->
            Log.i("WIFI", "WIFISCAN: $scanResult")
        }
        val scanResults =
            wifiManager.scanResults.filter { it.is80211mcResponder }  // Only APs supporting 802.11mc (RTT)
        this.lastWifi80211ScanResults = scanResults
        // Create the RTT Request with these APs
        rangingRequest = RangingRequest.Builder().apply {
            addAccessPoints(scanResults)
        }.build()

        // Start the Ranging
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.NEARBY_WIFI_DEVICES
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
//        wifiRttManager.startRanging(
//            rangingRequest,
//            context.mainExecutor,
//            object : RangingResultCallback() {
//                override fun onRangingFailure(code: Int) {
//                    Log.e("RTT", "Ranging failed with code: $code")
//                }
//
//                override fun onRangingResults(results: List<RangingResult>) {
//                    if (results.isNotEmpty()) {
//                        handleRangingResults(results)
//                    }
//                }
//            }
//        )
    }


    private fun handleRangingResults(results: List<RangingResult>) {
        this.lastRttResults = results
        if (results.isNotEmpty()) {
            val accessPoints = results.mapNotNull { result ->
                val responderLocation = result.unverifiedResponderLocation

                // Check if the responder location is available and valid
                if (responderLocation != null && responderLocation.isZaxisSubelementValid) {
                    val apLat = responderLocation.latitudeUncertainty
                    val apLng = responderLocation.longitudeUncertainty
                    val apAltitude = responderLocation.altitude // Optional

                    APInfo(apLat, apLng, result.distanceMm / 1000.0, result.macAddress!!) // Use the distance in meters
                } else {
                    Log.d("RTT", "No valid location data for AP: ${result.macAddress}")
                    null
                }
            }

            if (accessPoints.size >= 3) {
                // Perform trilateration to calculate user's location
                val estimatedLocation = calculatePositionFromRTT(accessPoints[0], accessPoints[1], accessPoints[2])
                estimatedLocation?.let { location ->
                    lastRttLocation = location
                }
            } else {
                Log.d("RTT", "Not enough APs with valid location data")
            }
        }
    }


    private fun calculatePositionFromRTT(ap1: APInfo, ap2: APInfo, ap3: APInfo): LatLng? {
        // Convert latitude and longitude to radians
        val lat1 = Math.toRadians(ap1.lat)
        val lon1 = Math.toRadians(ap1.lng)
        val lat2 = Math.toRadians(ap2.lat)
        val lon2 = Math.toRadians(ap2.lng)
        val lat3 = Math.toRadians(ap3.lat)
        val lon3 = Math.toRadians(ap3.lng)

        // Assume Earth radius in meters
        val R = 6378137.0

        // Trilateration algorithm
        val A = 2 * (lon2 - lon1)
        val B = 2 * (lat2 - lat1)
        val C = ap1.distance.pow(2) - ap2.distance.pow(2) - lat1.pow(2) + lat2.pow(2) - lon1.pow(2) + lon2.pow(2)
        val D = 2 * (lon3 - lon1)
        val E = 2 * (lat3 - lat1)
        val F = ap1.distance.pow(2) - ap3.distance.pow(2) - lat1.pow(2) + lat3.pow(2) - lon1.pow(2) + lon3.pow(2)

        val lat = (C * E - F * B) / (E * A - B * D)
        val lon = (C - A * lat) / B

        return LatLng(Math.toDegrees(lat), Math.toDegrees(lon))
    }

    // Function to save sensor and Wi-Fi data to a file
    fun saveDataToFile(
        pressure: Float?,
        acceleration: FloatArray?,
        gravity: FloatArray?,
        gyroscope: FloatArray?,
        rotation: FloatArray?,
        stepsDetected: Int,
        stepsCounted: Int,
        lastWifiScanResults: List<ScanResult>?,
        userLatLng: LatLng,
        timestamp: Long,
        locationAccuracy: Float?,
        file_timestamp: String,
        folder: String
    ) {
        try {
            context.openFileOutput("${folder}_${file_timestamp}_sensor_data.csv", Context.MODE_APPEND)
                .use { outputStream ->
                    // Write CSV header if file is empty (optional, but you might want to check if the header is needed)
                    if (outputStream.channel.size() == 0L) {
                        outputStream.write("Timestamp,Pressure,Acceleration,Gravity,Gyroscope,Rotation,Steps Counted,Steps Detected, LocationAccuracy, Wi-Fi Scan Results,LatLng\n".toByteArray())
                    }

                    // Convert sensor data to CSV row
                    val accelData = acceleration?.joinToString(separator = "|") ?: "N/A"
                    val gravityData = gravity?.joinToString(separator = "|") ?: "N/A"
                    val gyroData = gyroscope?.joinToString(separator = "|") ?: "N/A"
                    val rotationData = rotation?.joinToString(separator = "|") ?: "N/A"
                    val wifiScanResults =
                        lastWifiScanResults?.joinToString(separator = "|") { "${it.SSID},${it.BSSID},${it.level}" }
                            ?: "N/A"
//                val rttResults = lastRttResults?.joinToString(separator = "|") { "${it.macAddress},${it.distanceMm} mm" } ?: "N/A"

                    // Write data row to the CSV file
                    val dataRow =
                        "$timestamp,$pressure,$accelData,$gravityData,$gyroData,$rotationData,$stepsCounted,$stepsDetected,$locationAccuracy,$wifiScanResults,$userLatLng\n"
                    outputStream.write(dataRow.toByteArray())
                    outputStream.flush()
                }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


    data class APInfo(val lat: Double, val lng: Double, val distance: Double, val macAddress: MacAddress)
}