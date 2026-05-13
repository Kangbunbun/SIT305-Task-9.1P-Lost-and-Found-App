package com.example.lostfoundapp.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import com.example.lostfoundapp.R
import com.example.lostfoundapp.data.DatabaseHelper
import com.example.lostfoundapp.data.LostFoundItem
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.CancellationTokenSource

class MapsActivity : FragmentActivity(), OnMapReadyCallback {

    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var txtMapInfo: TextView
    private lateinit var edtRadiusKm: EditText
    private lateinit var mapContainer: View

    private var googleMap: GoogleMap? = null
    private val defaultMelbourne = LatLng(-37.8136, 144.9631)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        Toast.makeText(
            this,
            "Permission updated. Please press SEARCH NEARBY again.",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        databaseHelper = DatabaseHelper(this)

        bindViews()
        setupButtons()
        setupMap()
    }

    private fun bindViews() {
        txtMapInfo = findViewById(R.id.txtMapInfo)
        edtRadiusKm = findViewById(R.id.edtRadiusKm)
        mapContainer = findViewById(R.id.mapFragment)
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.btnBackHome).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.btnShowAll).setOnClickListener {
            showAllItems()
        }

        findViewById<Button>(R.id.btnSearchNearby).setOnClickListener {
            searchNearby()
        }
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment

        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isCompassEnabled = true

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultMelbourne, 10f))

        showAllItems()
    }

    private fun showAllItems() {
        val map = googleMap ?: return
        val items = databaseHelper.getAllItems().filter { hasValidLocation(it) }

        map.clear()

        if (items.isEmpty()) {
            txtMapInfo.text = "No saved lost/found items to show"
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultMelbourne, 10f))
            return
        }

        val positions = mutableListOf<LatLng>()

        for (item in items) {
            val position = LatLng(item.latitude, item.longitude)
            positions.add(position)
            addItemMarker(item)
        }

        txtMapInfo.text = "Showing ${items.size} lost/found item(s) on map"
        zoomToPositions(positions)
    }

    private fun searchNearby() {
        val radiusKm = edtRadiusKm.text.toString().trim().toDoubleOrNull()

        if (radiusKm == null || radiusKm <= 0.0) {
            edtRadiusKm.error = "Enter a valid radius"
            return
        }

        if (!hasLocationPermission()) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }

        getCurrentLocation(radiusKm)
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation(radiusKm: Double) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        Toast.makeText(this, "Getting current location...", Toast.LENGTH_SHORT).show()

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            CancellationTokenSource().token
        ).addOnSuccessListener { location ->
            if (location == null) {
                Toast.makeText(
                    this,
                    "Could not get current location. Please set emulator location and try again.",
                    Toast.LENGTH_LONG
                ).show()
                return@addOnSuccessListener
            }

            showNearbyItems(location, radiusKm)
        }.addOnFailureListener {
            Toast.makeText(
                this,
                "Failed to get current location.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showNearbyItems(userLocation: Location, radiusKm: Double) {
        val map = googleMap ?: return

        val userPosition = LatLng(userLocation.latitude, userLocation.longitude)

        val nearbyItems = databaseHelper.getAllItems()
            .filter { hasValidLocation(it) }
            .filter { item ->
                val distance = distanceKm(
                    userLocation.latitude,
                    userLocation.longitude,
                    item.latitude,
                    item.longitude
                )

                distance <= radiusKm
            }

        map.clear()

        addCurrentLocationMarker(userPosition, radiusKm)

        val positions = mutableListOf(userPosition)

        for (item in nearbyItems) {
            val position = LatLng(item.latitude, item.longitude)
            positions.add(position)
            addItemMarker(item)
        }

        txtMapInfo.text =
            "Current: ${String.format("%.5f", userPosition.latitude)}, " +
                    "${String.format("%.5f", userPosition.longitude)} | " +
                    "Showing ${nearbyItems.size} item(s) within ${String.format("%.1f", radiusKm)} km"

        zoomToPositions(positions)
    }

    private fun addItemMarker(item: LostFoundItem) {
        val map = googleMap ?: return
        val position = LatLng(item.latitude, item.longitude)

        map.addMarker(
            MarkerOptions()
                .position(position)
                .title("${item.postType}: ${item.description}")
                .snippet("${item.category} | ${item.location}")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        )
    }

    private fun addCurrentLocationMarker(position: LatLng, radiusKm: Double) {
        val map = googleMap ?: return

        map.addMarker(
            MarkerOptions()
                .position(position)
                .title("Current Location")
                .snippet("Search radius: ${String.format("%.1f", radiusKm)} km")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        )
    }

    private fun zoomToPositions(positions: List<LatLng>) {
        val map = googleMap ?: return

        if (positions.isEmpty()) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultMelbourne, 10f))
            return
        }

        if (positions.size == 1) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(positions.first(), 14f))
            return
        }

        val boundsBuilder = LatLngBounds.Builder()

        for (position in positions) {
            boundsBuilder.include(position)
        }

        mapContainer.post {
            map.animateCamera(
                CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120)
            )
        }
    }

    private fun distanceKm(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double
    ): Double {
        val result = FloatArray(1)

        Location.distanceBetween(
            lat1,
            lng1,
            lat2,
            lng2,
            result
        )

        return result[0] / 1000.0
    }

    private fun hasValidLocation(item: LostFoundItem): Boolean {
        val validLatitude = item.latitude in -90.0..90.0
        val validLongitude = item.longitude in -180.0..180.0
        val notZero = !(item.latitude == 0.0 && item.longitude == 0.0)

        return validLatitude && validLongitude && notZero
    }

    private fun hasLocationPermission(): Boolean {
        val fineGranted = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineGranted || coarseGranted
    }
}