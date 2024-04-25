package com.example.routemapper.map
import android.Manifest
import android.app.ProgressDialog
import android.content.pm.PackageManager
import android.graphics.Rect
import android.location.GpsStatus
import android.location.Location
import android.util.Log

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.routemapper.*
import com.example.routemapper.databinding.ActivityMapBinding
import com.example.routemapper.services.WebClient
import com.example.routemapper.ui.theme.RouteMapperTheme
import okhttp3.Response

import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay



class MapActivity : AppCompatActivity(), MapListener, GpsStatus.Listener {

    lateinit var mMap: MapView
    lateinit var controller: IMapController;
    lateinit var mMyLocationOverlay: MyLocationNewOverlay;
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private val mapperViewModel by viewModels<MapperViewModel>();
    private val webClient = WebClient();
    private var userId = 0;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE)
        )

        mMap = binding.osmmap
        mMap.setTileSource(TileSourceFactory.MAPNIK)
        mMap.mapCenter
        mMap.setMultiTouchControls(true)

        mMyLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), mMap)
        controller = mMap.controller

        mMyLocationOverlay.enableMyLocation()
        mMyLocationOverlay.enableFollowLocation()
        mMyLocationOverlay.isDrawAccuracyEnabled = true
        mMyLocationOverlay.runOnFirstFix {
            runOnUiThread {
                controller.setCenter(mMyLocationOverlay.myLocation)
                controller.animateTo(mMyLocationOverlay.myLocation)
            }
        }

        controller.setZoom(6.0)
        mMap.overlays.add(mMyLocationOverlay)
        mMap.addMapListener(this)

        // Find the FloatingActionButton and set OnClickListener
        binding.btnCenterMap.setOnClickListener {
            val userLocation = mMyLocationOverlay.myLocation
            this.userId = registerUser(userLocation.latitude, userLocation.longitude)?: 0;
        }

        checkLocationPermission() // Check location permission when activity starts
        initStepCounter();
//        setContent {
//            RouteMapperTheme {
//                // A surface container using the 'background' color from the theme
//                Surface(
//                    modifier = Modifier.fillMaxSize(),
//                ) {
//
//
//                    Greeting(mapperViewModel, mMyLocationOverlay, mMap)
//                }
//            }
//        }
    }

        private fun initStepCounter() {
            val stepSensorDetector = StepSensorDetector(this@MapActivity)
            val rotationSensorDetector = RotationSensorDetector(this@MapActivity)

            val availableStepDetector = stepSensorDetector.registerListener(object : StepListener {
                override fun onStep(count: Int) {
                    val lastHeading = rotationSensorDetector.getLastHeading();
                    stepSensorDetector.saveStepToFile(0, lastHeading)
                    postStep(userId, lastHeading)
                    mapperViewModel.incrementCounter(count)
                }
            })
            print("Available step d: $availableStepDetector");


            val availableRorationDetector =
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

        private fun centerMapToUserLocation() {
            val userLocation = mMyLocationOverlay.myLocation
            if (userLocation != null) {
                controller.animateTo(userLocation)
            } else {
                Snackbar.make(
                    mMap,
                    "Unable to get current location. Make sure location permissions are granted.",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }

        private fun fetchData() {
            webClient.fetchData { response ->
                Log.e("TAG", "onResponse:la ${response}");
                Snackbar.make(
                    mMap,
                    response.toString(),
                    Snackbar.LENGTH_SHORT
                ).show()
                runOnUiThread {
                    Toast.makeText(this, response ?: "Failed to fetch data", Toast.LENGTH_SHORT).show()
                }
            }
        }
        private fun registerUser(lat: Double, long: Double): Int? {
            var res: Int? = 0
            webClient.registerUser(lat, long) { response ->
                Log.e("TAG", "onResponse:la ${response}");
                Snackbar.make(
                    mMap,
                    response.toString(),
                    Snackbar.LENGTH_SHORT
                ).show()
                res = response
            }
            return res;
        }

    private fun postStep(userId: Int, heading: Double) {
        var res: Int? = 0
        webClient.postStep(userId, heading) { response ->
            Log.e("TAG", "onResponse:la ${response}");
            Snackbar.make(
                mMap,
                response.toString(),
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

        override fun onScroll(event: ScrollEvent?): Boolean {
            // event?.source?.getMapCenter()
//            Log.e("TAG", "onCreate:la ${event?.source?.getMapCenter()?.latitude}")
//            Log.e("TAG", "onCreate:lo ${event?.source?.getMapCenter()?.longitude}")
            //  Log.e("TAG", "onScroll   x: ${event?.x}  y: ${event?.y}", )
            return true
        }

        override fun onZoom(event: ZoomEvent?): Boolean {
            //  event?.zoomLevel?.let { controller.setZoom(it) }


            Log.e("TAG", "onZoom zoom level: ${event?.zoomLevel}   source:  ${event?.source}")
            return false;
        }

        override fun onGpsStatusChanged(event: Int) {


            TODO("Not yet implemented")
        }

        private fun checkLocationPermission() {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Permission is not granted
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    @Composable
    fun Greeting(
        mainViewModel: MapperViewModel = viewModel(),
        mMyLocationOverlay: MyLocationNewOverlay,
        mMap: MapView
    ) {
        AndroidView({ context ->
            MapView(context).apply {
                id = R.id.osmmap
                setTileSource(TileSourceFactory.MAPNIK)
                mapCenter
                setMultiTouchControls(true)
                controller.setZoom(6.0)
                mMap.overlays.add(mMyLocationOverlay)
            }
        }) { mapView ->
            // MapView is a child of the Box
            mapView
        }
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 20.dp)
        ) {

            val drawable = LineDrawable(mainViewModel.points.toList())

            drawIntoCanvas { canvas ->
                drawable.setBounds(0, 0, size.width.toInt(), size.height.toInt())
                drawable.draw(canvas.nativeCanvas)
            }
            // Draw map here

        }

        Text(
            text = mainViewModel.msg.value
        )
    }

