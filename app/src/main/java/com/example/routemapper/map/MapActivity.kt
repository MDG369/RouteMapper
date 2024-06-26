package com.example.routemapper.map
import ServerConfigDialogFragment
import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import com.example.routemapper.*
import com.example.routemapper.network.WebClient
import com.example.routemapper.stephandling.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlin.math.cos
import kotlin.math.sin


class MapActivity : AppCompatActivity(), OnMapReadyCallback,  ServerConfigDialogFragment.ServerConfigListener {

    private lateinit var mMap: GoogleMap
    private var userMarker: Marker? = null
    private var userLocation: LatLng? = null
    private val webClient = WebClient()
    private val mapperViewModel by viewModels<MapperViewModel>()
    private var userId: Int = 0
    private var polyline: Polyline? = null
    private var polylines: ArrayList<Polyline> = ArrayList()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val DEFAULT_ZOOM: Float = 20.0F
    private var localizationStarted: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        showServerConfigDialog()
        initStepCounter()
    }

    private fun showServerConfigDialog() {
        val dialogFragment = ServerConfigDialogFragment()
        dialogFragment.show(supportFragmentManager, "serverConfigDialog")
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
        } else {
            requestPermissions(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }

        getLastKnownLocation()

        // Set onClickListener for the button
        val btnCenterMap = findViewById<FloatingActionButton>(R.id.btnCenterMap)
        btnCenterMap.setOnClickListener {
            if (!localizationStarted) {
                userLocation?.let { location ->
                    // Send the latitude and longitude to the server
                    registerUser(location.latitude, location.longitude)!!
                    Log.e("TAG", userId.toString())

                    btnCenterMap.setImageResource(R.drawable.ic_stop_button)
                    mMap.setOnMapClickListener(null)
                    mMap.setOnMyLocationButtonClickListener(null)

                    localizationStarted = true
                }
            } else {
                stopLocalization(userId)
                btnCenterMap.setImageResource(R.drawable.ic_start_button)
                userMarker?.remove()
                userLocation = null
                val iterator = polylines.iterator()
                while (iterator.hasNext()) {
                    val polyline = iterator.next()
                    polyline.remove()
                    iterator.remove()
                }
                setMapListeners(btnCenterMap)
                localizationStarted = false
            }
        }
        btnCenterMap.isEnabled = false
        setMapListeners(btnCenterMap)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission granted, enable location
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            mMap.isMyLocationEnabled = true
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setMapListeners(btnCenterMap: FloatingActionButton) {
        mMap.setOnMapClickListener { latLng ->
            userMarker?.remove()
            userMarker = mMap.addMarker(MarkerOptions().position(latLng).title("Twoja lokalizacja"))
            userLocation = latLng
            btnCenterMap.isEnabled = true
        }
        mMap.setOnMyLocationButtonClickListener {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                userMarker?.remove()

                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val latLng = LatLng(location.latitude, location.longitude)
                        userMarker = mMap.addMarker(MarkerOptions().position(latLng).title("Twoja lokalizacja"))
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 20f))
                        userLocation = latLng

                        btnCenterMap.isEnabled = true
                    }
                }
            }
            true
        }
    }

    private fun initStepCounter() {
        val stepSensorDetector = StepSensorDetector(this@MapActivity)
        val rotationSensorDetector = RotationSensorDetector(this@MapActivity)

        val availableStepDetector = stepSensorDetector.registerListener(object : StepListener {
            override fun onStep(count: Int) {
                if (localizationStarted) {
                    val lastHeading = rotationSensorDetector.getLastHeading()
//                    stepSensorDetector.saveStepToFile(0, lastHeading)
                    val newLocation = getNewLocationFromHeading(lastHeading, 0.5)
                    postStep(userId, lastHeading)
                    drawPolyline(userLocation!!, newLocation)
                    mapperViewModel.incrementCounter(count)
                    userLocation = newLocation
                }
            }
        })
        rotationSensorDetector.registerListener(object : RotationListener {
            override fun onRotation(rotation: Float) {
                mapperViewModel.setRotation(rotation)
            }
        })

        var error = ""

        if (!availableStepDetector) {
            if (error.isNotEmpty()) {
                error += "\n\n"
                mapperViewModel.setMsg(error)

                Log.i("Main", error)
            } else {
                mapperViewModel.setMsg("not available")
            }
        } else {
            mapperViewModel.setMsg("initialized successful")
        }
    }

    private fun registerUser(lat: Double, long: Double): Int? {
        var res: Int? = 0
        webClient.registerUser(lat, long) { response ->
            Log.e("TAG", "onResponse:la ${response}")
            runOnUiThread {
                Toast.makeText(this, response.toString() ?: "Failed to register the user", Toast.LENGTH_SHORT).show()
            }
            res = response
            userId = response!!
        }
        return res
    }

    private fun postStep(userId: Int, heading: Double) {
        webClient.postStep(userId, heading) { }
    }

    private fun stopLocalization(userId: Int) {

    }

    private fun drawPolyline(previousLocation: LatLng, location: LatLng) {
        // Create a PolylineOptions object and add the current and previous locations
        val polylineOptions = PolylineOptions()
            .add(previousLocation)
            .add(location)

        polyline = mMap.addPolyline(polylineOptions)
        polylines.add(polyline!!)
    }

    private fun getNewLocationFromHeading(heading: Double, distance: Double): LatLng {
        // Earth's radius in meters
        val earthRadius = 6378137 // Approximate value for WGS84 ellipsoid

        // Convert heading to Cartesian coordinates
        val dx = cos(heading) * distance
        val dy = sin(heading) * distance

        // Convert offset in meters to offset in degrees (longitude and latitude)
        val deltaLongitude = dx / (earthRadius * cos(this.userLocation!!.latitude * Math.PI / 180)) * (180 / Math.PI);
        val deltaLatitude = dy / earthRadius * (180 / Math.PI)

        val newLatitude = this.userLocation!!.latitude + deltaLatitude
        val newLongitude = this.userLocation!!.longitude + deltaLongitude

        return LatLng(newLatitude, newLongitude)
    }

    private fun getLastKnownLocation() {
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Retrieve the last known location
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    // Move the camera to the last known location if available
                    if (location != null) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), DEFAULT_ZOOM))
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to get last known location: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            requestPermissions(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }
    override fun onServerConfigInput(ip: String, port: String) {
        val sharedPreferences = getSharedPreferences("ServerPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("IP", ip)
        editor.putString("Port", port)
        editor.apply()
        webClient.ip = ip;
        webClient.port = port
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}
