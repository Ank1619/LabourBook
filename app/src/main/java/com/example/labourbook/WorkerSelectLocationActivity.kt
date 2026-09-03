package com.example.labourbook

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.labourbook.network.ServiceAreaRepository
import com.google.android.gms.location.LocationServices
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.util.Locale

class WorkerSelectLocationActivity : AppCompatActivity() {

    private lateinit var etManualAddress: EditText
    private lateinit var btnUseCurrentLocation: View
    private lateinit var btnContinueLocation: MaterialButton
    private lateinit var tvCurrentLocation: TextView

    private var selectedAddress: String = ""
    private var selectedCityArea: String = ""
    private var selectedState: String = ""

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

            if (fineLocationGranted || coarseLocationGranted) {
                fetchCurrentLocation()
            } else {
                Toast.makeText(
                    this,
                    "Location permission is required to detect your working location",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_worker_select_location)

        etManualAddress = findViewById(R.id.etWorkerManualAddress)
        btnUseCurrentLocation = findViewById(R.id.btnWorkerUseCurrentLocation)
        btnContinueLocation = findViewById(R.id.btnWorkerContinueLocation)
        tvCurrentLocation = findViewById(R.id.tvWorkerCurrentLocation)

        btnUseCurrentLocation.setOnClickListener {
            checkLocationPermission()
        }

        btnContinueLocation.setOnClickListener {
            val manualAddress = etManualAddress.text.toString().trim()

            if (manualAddress.isNotEmpty()) {
                selectedAddress = manualAddress
                validateServiceArea(manualAddress)
            } else if (selectedCityArea.isNotEmpty() && selectedState.isNotEmpty()) {
                validateServiceArea(selectedCityArea, selectedState)
            } else {
                Toast.makeText(
                    this,
                    "Please enter your address or use current location",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun checkLocationPermission() {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineLocationGranted || coarseLocationGranted) {
            fetchCurrentLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun fetchCurrentLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        getReadableAddress(location.latitude, location.longitude)
                    } else {
                        Toast.makeText(
                            this,
                            "Unable to detect location. Please turn on GPS.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(
                        this,
                        "Failed to get current location",
                        Toast.LENGTH_LONG
                    ).show()
                }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_LONG).show()
        }
    }

    private fun getReadableAddress(latitude: Double, longitude: Double) {
        val geocoder = Geocoder(this, Locale.getDefault())

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                    runOnUiThread {
                        updateReadableLocation(addresses)
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                updateReadableLocation(addresses ?: emptyList())
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to get address from location", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateReadableLocation(addresses: List<Address>) {
        if (addresses.isNotEmpty()) {
            val address = addresses[0]
            val readableAddress = address.getAddressLine(0) ?: ""
            selectedCityArea = address.subLocality ?: address.locality ?: ""
            selectedState = address.adminArea ?: ""
            selectedAddress = readableAddress

            tvCurrentLocation.text = "$readableAddress\n\nArea: $selectedCityArea\nState: $selectedState"
            Toast.makeText(this, "Location detected successfully", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Unable to find your address", Toast.LENGTH_LONG).show()
        }
    }

    private fun validateServiceArea(address: String) {
        val sharedPreferences = getSharedPreferences("salesforce_auth", Context.MODE_PRIVATE)
        val accessToken = sharedPreferences.getString("access_token", null)
        val instanceUrl = sharedPreferences.getString("instance_url", null)

        if (accessToken.isNullOrEmpty() || instanceUrl.isNullOrEmpty()) {
            saveLocationAndProceed(address)
            return
        }

        lifecycleScope.launch {
            try {
                val response = ServiceAreaRepository.getServiceAreas(
                    instanceUrl = instanceUrl,
                    accessToken = accessToken
                )

                if (response == null || response.records.isEmpty()) {
                    saveLocationAndProceed(address)
                    return@launch
                }

                val normalizedAddress = address.lowercase().trim()
                val serviceAvailable = response.records.any { area ->
                    val cityArea = area.City_Area__c?.lowercase()?.trim() ?: ""
                    val state = area.State__c?.lowercase()?.trim() ?: ""

                    val matchesCity = cityArea.isNotEmpty() && (normalizedAddress.contains(cityArea) || cityArea.contains(normalizedAddress))
                    val matchesState = state.isNotEmpty() && (normalizedAddress.contains(state) || state.contains(normalizedAddress))

                    matchesCity || matchesState
                }

                if (serviceAvailable) {
                    saveLocationAndProceed(address)
                } else {
                    Toast.makeText(
                        this@WorkerSelectLocationActivity,
                        "Sorry, our service is not available in this area yet.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@WorkerSelectLocationActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun validateServiceArea(cityArea: String, state: String) {
        val sharedPreferences = getSharedPreferences("salesforce_auth", Context.MODE_PRIVATE)
        val accessToken = sharedPreferences.getString("access_token", null)
        val instanceUrl = sharedPreferences.getString("instance_url", null)

        if (accessToken.isNullOrEmpty() || instanceUrl.isNullOrEmpty()) {
            saveLocationAndProceed(selectedAddress)
            return
        }

        lifecycleScope.launch {
            try {
                val response = ServiceAreaRepository.getServiceAreas(
                    instanceUrl = instanceUrl,
                    accessToken = accessToken
                )

                if (response == null || response.records.isEmpty()) {
                    saveLocationAndProceed(selectedAddress)
                    return@launch
                }

                val normalizedCityArea = cityArea.lowercase().trim()
                val normalizedState = state.lowercase().trim()

                val serviceAvailable = response.records.any { area ->
                    val salesforceCityArea = area.City_Area__c?.lowercase()?.trim() ?: ""
                    val salesforceState = area.State__c?.lowercase()?.trim() ?: ""

                    val cityMatches = salesforceCityArea.isNotEmpty() && (normalizedCityArea.contains(salesforceCityArea) || salesforceCityArea.contains(normalizedCityArea))
                    val stateMatches = salesforceState.isNotEmpty() && (normalizedState.contains(salesforceState) || salesforceState.contains(normalizedState))

                    cityMatches || stateMatches
                }

                if (serviceAvailable) {
                    saveLocationAndProceed(selectedAddress)
                } else {
                    Toast.makeText(
                        this@WorkerSelectLocationActivity,
                        "Sorry, our service is not available in this area yet.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@WorkerSelectLocationActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun saveLocationAndProceed(address: String) {
        val appPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        appPrefs.edit()
            .putString("worker_address", address)
            .putString("selected_location", address)
            .putString("selected_city_area", selectedCityArea)
            .putString("selected_state", selectedState)
            .apply()

        Toast.makeText(
            this,
            "Great! Service is available in your working area.",
            Toast.LENGTH_LONG
        ).show()

        val intent = Intent(this, WorkerMainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
