package com.example.lostfoundapp.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.example.lostfoundapp.R
import com.example.lostfoundapp.data.DatabaseHelper
import com.example.lostfoundapp.data.LostFoundItem
import com.example.lostfoundapp.utils.DateTimeUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode

class CreateAdvertActivity : ComponentActivity() {

    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var radioLost: RadioButton
    private lateinit var radioFound: RadioButton
    private lateinit var edtContactName: EditText
    private lateinit var edtPhone: EditText
    private lateinit var edtDescription: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var btnChooseImage: Button
    private lateinit var imgPreview: ImageView
    private lateinit var txtDateTime: TextView
    private lateinit var edtLocation: EditText
    private lateinit var btnSearchLocation: Button
    private lateinit var btnGetCurrentLocation: Button
    private lateinit var btnCancel: Button
    private lateinit var btnSave: Button

    private var selectedImageUri: String = ""
    private var selectedLatitude: Double? = null
    private var selectedLongitude: Double? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }

            selectedImageUri = uri.toString()

            imgPreview.visibility = View.VISIBLE
            imgPreview.setImageURI(uri)

            btnChooseImage.text = "Image Selected"
            Toast.makeText(this, "Image selected successfully", Toast.LENGTH_SHORT).show()
        }
    }

    private val autocompleteLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data

        if (result.resultCode == Activity.RESULT_OK && data != null) {
            val place = Autocomplete.getPlaceFromIntent(data)
            handleSelectedPlace(place)
        } else if (result.resultCode == AutocompleteActivity.RESULT_ERROR && data != null) {
            val status = Autocomplete.getStatusFromIntent(data)
            Toast.makeText(
                this,
                "Place search error: ${status.statusMessage}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineLocationGranted || coarseLocationGranted) {
            getCurrentLocation()
        } else {
            Toast.makeText(
                this,
                "Location permission is required to get current location",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_advert)

        databaseHelper = DatabaseHelper(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        bindViews()
        setupCategorySpinner()
        initializePlacesIfPossible()

        txtDateTime.text = "Posting time: ${DateTimeUtils.getCurrentDateTime()}"

        btnChooseImage.setOnClickListener {
            imagePickerLauncher.launch(arrayOf("image/*"))
        }

        edtLocation.setOnClickListener {
            openPlaceAutocomplete()
        }

        btnSearchLocation.setOnClickListener {
            openPlaceAutocomplete()
        }

        btnGetCurrentLocation.setOnClickListener {
            checkLocationPermissionAndFetch()
        }

        btnCancel.setOnClickListener {
            finish()
        }

        btnSave.setOnClickListener {
            saveAdvert()
        }
    }

    private fun bindViews() {
        radioLost = findViewById(R.id.radioLost)
        radioFound = findViewById(R.id.radioFound)
        edtContactName = findViewById(R.id.edtContactName)
        edtPhone = findViewById(R.id.edtPhone)
        edtDescription = findViewById(R.id.edtDescription)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        btnChooseImage = findViewById(R.id.btnChooseImage)
        imgPreview = findViewById(R.id.imgPreview)
        txtDateTime = findViewById(R.id.txtDateTime)
        edtLocation = findViewById(R.id.edtLocation)
        btnSearchLocation = findViewById(R.id.btnSearchLocation)
        btnGetCurrentLocation = findViewById(R.id.btnGetCurrentLocation)
        btnCancel = findViewById(R.id.btnCancel)
        btnSave = findViewById(R.id.btnSave)
    }

    private fun setupCategorySpinner() {
        val categories = listOf(
            "Electronics",
            "Pets",
            "Wallets",
            "Keys",
            "Documents",
            "Other"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            categories
        )

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter
    }

    private fun initializePlacesIfPossible() {
        if (Places.isInitialized()) {
            return
        }

        val apiKey = getMapsApiKeyFromManifest()

        if (apiKey.isBlank()) {
            Toast.makeText(
                this,
                "Google API key is missing. Place search may not work.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        Places.initialize(applicationContext, apiKey)
    }

    private fun getMapsApiKeyFromManifest(): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(
                packageName,
                PackageManager.GET_META_DATA
            )

            appInfo.metaData.getString("com.google.android.geo.API_KEY", "") ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    private fun openPlaceAutocomplete() {
        if (!Places.isInitialized()) {
            initializePlacesIfPossible()
        }

        if (!Places.isInitialized()) {
            Toast.makeText(
                this,
                "Places API is not ready. Please check your API key.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val fields = listOf(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG
        )

        val intent = Autocomplete.IntentBuilder(
            AutocompleteActivityMode.OVERLAY,
            fields
        ).build(this)

        autocompleteLauncher.launch(intent)
    }

    private fun handleSelectedPlace(place: Place) {
        val latLng = place.latLng

        if (latLng == null) {
            Toast.makeText(
                this,
                "Selected place does not have coordinates",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        selectedLatitude = latLng.latitude
        selectedLongitude = latLng.longitude

        val locationText = place.address
            ?: place.name
            ?: "Selected Location (${String.format("%.5f", latLng.latitude)}, ${
                String.format("%.5f", latLng.longitude)
            })"

        edtLocation.setText(locationText)

        Toast.makeText(this, "Location selected from search", Toast.LENGTH_SHORT).show()
    }

    private fun checkLocationPermissionAndFetch() {
        val fineLocationGranted = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineLocationGranted || coarseLocationGranted) {
            getCurrentLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        Toast.makeText(this, "Getting current location...", Toast.LENGTH_SHORT).show()

        val cancellationTokenSource = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        ).addOnSuccessListener { location ->
            if (location != null) {
                updateSelectedLocation(location)
            } else {
                getLastKnownLocation()
            }
        }.addOnFailureListener {
            getLastKnownLocation()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    updateSelectedLocation(location)
                } else {
                    Toast.makeText(
                        this,
                        "Unable to get location. Please enable location and try again.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Failed to get location. Please try again.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun updateSelectedLocation(location: Location) {
        selectedLatitude = location.latitude
        selectedLongitude = location.longitude

        val locationText = "Current Location (${String.format("%.5f", location.latitude)}, ${
            String.format("%.5f", location.longitude)
        })"

        edtLocation.setText(locationText)

        Toast.makeText(this, "Current location selected", Toast.LENGTH_SHORT).show()
    }

    private fun saveAdvert() {
        val postType = if (radioLost.isChecked) "Lost" else "Found"
        val contactName = edtContactName.text.toString().trim()
        val phone = edtPhone.text.toString().trim()
        val description = edtDescription.text.toString().trim()
        val category = spinnerCategory.selectedItem.toString()

        val dateTime = DateTimeUtils.getCurrentDateTime()
        txtDateTime.text = "Posting time: $dateTime"

        val location = edtLocation.text.toString().trim()

        if (contactName.isEmpty()) {
            edtContactName.error = "Contact name is required"
            return
        }

        if (phone.isEmpty()) {
            edtPhone.error = "Phone is required"
            return
        }

        if (description.isEmpty()) {
            edtDescription.error = "Description is required"
            return
        }

        if (location.isEmpty()) {
            edtLocation.error = "Location is required"
            return
        }

        val latitude = selectedLatitude
        val longitude = selectedLongitude

        if (latitude == null || longitude == null) {
            Toast.makeText(
                this,
                "Please select a location using search or current location",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (selectedImageUri.isEmpty()) {
            Toast.makeText(this, "Please choose an image", Toast.LENGTH_SHORT).show()
            return
        }

        val item = LostFoundItem(
            postType = postType,
            contactName = contactName,
            phone = phone,
            description = description,
            category = category,
            dateTime = dateTime,
            location = location,
            imageUri = selectedImageUri,
            latitude = latitude,
            longitude = longitude
        )

        val result = databaseHelper.insertItem(item)

        if (result > 0) {
            Toast.makeText(this, "Advert saved successfully", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, ItemListActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "Failed to save advert", Toast.LENGTH_SHORT).show()
        }
    }
}