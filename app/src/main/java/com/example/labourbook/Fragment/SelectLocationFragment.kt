package com.example.labourbook.Fragment

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.labourbook.R
import com.example.labourbook.network.ServiceAreaRepository
import com.google.android.gms.location.LocationServices
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.util.Locale

class SelectLocationFragment : Fragment() {

    private lateinit var etManualAddress: EditText
    private lateinit var btnUseCurrentLocation: View
    private lateinit var btnContinueLocation: MaterialButton
    private lateinit var tvCurrentLocation: TextView

    private var selectedAddress: String = ""

    // GPS extracted location details
    private var selectedCityArea: String = ""
    private var selectedState: String = ""


    // ==========================================
    // LOCATION PERMISSION
    // ==========================================

    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val fineLocationGranted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION]
                    ?: false

            val coarseLocationGranted =
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION]
                    ?: false

            if (
                fineLocationGranted ||
                coarseLocationGranted
            ) {

                fetchCurrentLocation()

            } else {

                Toast.makeText(
                    requireContext(),
                    "Location permission is required to detect your current location",
                    Toast.LENGTH_LONG
                ).show()
            }
        }


    // ==========================================
    // CREATE VIEW
    // ==========================================

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        return inflater.inflate(
            R.layout.fragment_select_location,
            container,
            false
        )
    }


    // ==========================================
    // VIEW CREATED
    // ==========================================

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(
            view,
            savedInstanceState
        )


        // ==========================================
        // INITIALIZE VIEWS
        // ==========================================

        etManualAddress =
            view.findViewById(
                R.id.etManualAddress
            )

        btnUseCurrentLocation =
            view.findViewById(
                R.id.btnUseCurrentLocation
            )

        btnContinueLocation =
            view.findViewById(
                R.id.btnContinueLocation
            )

        tvCurrentLocation =
            view.findViewById(
                R.id.tvCurrentLocation
            )


        // ==========================================
        // USE CURRENT LOCATION
        // ==========================================

        btnUseCurrentLocation.setOnClickListener {

            checkLocationPermission()
        }


        // ==========================================
        // CONTINUE BUTTON
        // ==========================================

        btnContinueLocation.setOnClickListener {

            val manualAddress =
                etManualAddress.text
                    .toString()
                    .trim()


            // ==========================================
            // MANUAL ADDRESS
            // ==========================================

            if (manualAddress.isNotEmpty()) {

                selectedAddress =
                    manualAddress

                validateServiceArea(
                    manualAddress
                )


                // ==========================================
                // GPS LOCATION
                // ==========================================

            } else if (
                selectedCityArea.isNotEmpty() &&
                selectedState.isNotEmpty()
            ) {

                validateServiceArea(
                    selectedCityArea,
                    selectedState
                )


                // ==========================================
                // NO LOCATION
                // ==========================================

            } else {

                Toast.makeText(
                    requireContext(),
                    "Please enter your address or use current location",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    // ==========================================
    // VALIDATE MANUAL ADDRESS
    // ==========================================

    private fun validateServiceArea(
        address: String
    ) {

        val sharedPreferences =
            requireContext().getSharedPreferences(
                "salesforce_auth",
                Context.MODE_PRIVATE
            )

        val accessToken =
            sharedPreferences.getString(
                "access_token",
                null
            )

        val instanceUrl =
            sharedPreferences.getString(
                "instance_url",
                null
            )


        if (
            accessToken.isNullOrEmpty() ||
            instanceUrl.isNullOrEmpty()
        ) {

            Toast.makeText(
                requireContext(),
                "Salesforce connection not found",
                Toast.LENGTH_LONG
            ).show()

            return
        }


        viewLifecycleOwner.lifecycleScope.launch {

            try {

                val response =
                    ServiceAreaRepository.getServiceAreas(
                        instanceUrl = instanceUrl,
                        accessToken = accessToken
                    )


                if (response == null) {

                    Toast.makeText(
                        requireContext(),
                        "Failed to check service availability",
                        Toast.LENGTH_LONG
                    ).show()

                    return@launch
                }


                val normalizedAddress =
                    address
                        .lowercase()
                        .trim()


                val serviceAvailable =
                    response.records.any { area ->

                        val cityArea =
                            area.City_Area__c
                                ?.lowercase()
                                ?.trim()
                                ?: ""

                        val state =
                            area.State__c
                                ?.lowercase()
                                ?.trim()
                                ?: ""


                        val cityMatch = cityArea.isNotEmpty() && (normalizedAddress.contains(cityArea) || cityArea.contains(normalizedAddress))
                        val stateMatch = state.isNotEmpty() && (normalizedAddress.contains(state) || state.contains(normalizedAddress))

                        cityMatch || stateMatch
                    }


                if (serviceAvailable) {

                    saveLocationAndGoHome(
                        address = address,
                        cityArea = "",
                        state = ""
                    )

                } else {

                    Toast.makeText(
                        requireContext(),
                        "Sorry, our service is not available in this area yet.",
                        Toast.LENGTH_LONG
                    ).show()
                }


            } catch (e: Exception) {

                Toast.makeText(
                    requireContext(),
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    // ==========================================
    // VALIDATE GPS CITY AREA + STATE
    // ==========================================

    private fun validateServiceArea(
        cityArea: String,
        state: String
    ) {

        val sharedPreferences =
            requireContext().getSharedPreferences(
                "salesforce_auth",
                Context.MODE_PRIVATE
            )

        val accessToken =
            sharedPreferences.getString(
                "access_token",
                null
            )

        val instanceUrl =
            sharedPreferences.getString(
                "instance_url",
                null
            )


        if (
            accessToken.isNullOrEmpty() ||
            instanceUrl.isNullOrEmpty()
        ) {

            Toast.makeText(
                requireContext(),
                "Salesforce connection not found",
                Toast.LENGTH_LONG
            ).show()

            return
        }


        viewLifecycleOwner.lifecycleScope.launch {

            try {

                val response =
                    ServiceAreaRepository.getServiceAreas(
                        instanceUrl = instanceUrl,
                        accessToken = accessToken
                    )


                if (response == null) {

                    Toast.makeText(
                        requireContext(),
                        "Failed to check service availability",
                        Toast.LENGTH_LONG
                    ).show()

                    return@launch
                }


                val normalizedCityArea =
                    cityArea
                        .lowercase()
                        .trim()

                val normalizedState =
                    state
                        .lowercase()
                        .trim()


                val serviceAvailable =
                    response.records.any { area ->

                        val salesforceCityArea =
                            area.City_Area__c
                                ?.lowercase()
                                ?.trim()
                                ?: ""

                        val salesforceState =
                            area.State__c
                                ?.lowercase()
                                ?.trim()
                                ?: ""


                        val cityMatches = salesforceCityArea.isNotEmpty() && (normalizedCityArea.contains(salesforceCityArea) || salesforceCityArea.contains(normalizedCityArea))
                        val stateMatches = salesforceState.isNotEmpty() && (normalizedState.contains(salesforceState) || salesforceState.contains(normalizedState))

                        cityMatches || stateMatches
                    }


                if (serviceAvailable) {

                    saveLocationAndGoHome(
                        address = selectedAddress,
                        cityArea = cityArea,
                        state = state
                    )

                } else {

                    Toast.makeText(
                        requireContext(),
                        "Sorry, our service is not available in this area yet.",
                        Toast.LENGTH_LONG
                    ).show()
                }


            } catch (e: Exception) {

                Toast.makeText(
                    requireContext(),
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    // ==========================================
    // SAVE LOCATION AND GO HOME
    // ==========================================

    private fun saveLocationAndGoHome(
        address: String,
        cityArea: String,
        state: String
    ) {

        val prefs =
            requireContext()
                .getSharedPreferences(
                    "app_prefs",
                    Context.MODE_PRIVATE
                )


        prefs.edit()
            .putString(
                "selected_location",
                address
            )
            .putString(
                "selected_city_area",
                cityArea
            )
            .putString(
                "selected_state",
                state
            )
            .apply()

        val salesforcePrefs =
            requireContext()
                .getSharedPreferences(
                    "salesforce_auth",
                    Context.MODE_PRIVATE
                )

        salesforcePrefs.edit()
            .putString(
                "customer_address",
                address
            )
            .apply()


        Toast.makeText(
            requireContext(),
            "Great! Service is available in your area.",
            Toast.LENGTH_LONG
        ).show()


        findNavController().navigate(
            R.id.action_selectLocation_to_homeFragment
        )
    }


    // ==========================================
    // CHECK LOCATION PERMISSION
    // ==========================================

    private fun checkLocationPermission() {

        val fineLocationGranted =
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED


        val coarseLocationGranted =
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED


        if (
            fineLocationGranted ||
            coarseLocationGranted
        ) {

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


    // ==========================================
    // FETCH CURRENT LOCATION
    // ==========================================

    private fun fetchCurrentLocation() {

        val fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(
                requireActivity()
            )

        try {

            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->

                    if (location != null) {

                        val latitude =
                            location.latitude

                        val longitude =
                            location.longitude


                        getReadableAddress(
                            latitude,
                            longitude
                        )

                    } else {

                        if (isAdded) {
                            Toast.makeText(
                                requireContext(),
                                "Unable to detect location. Please turn on GPS.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }

                .addOnFailureListener {

                    if (isAdded) {
                        Toast.makeText(
                            requireContext(),
                            "Failed to get current location",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }


        } catch (e: SecurityException) {

            if (isAdded) {
                Toast.makeText(
                    requireContext(),
                    "Location permission not granted",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    // ==========================================
    // CONVERT GPS TO READABLE ADDRESS
    // ==========================================

    private fun getReadableAddress(
        latitude: Double,
        longitude: Double
    ) {

        val safeContext = context ?: return

        val geocoder =
            Geocoder(
                safeContext,
                Locale.getDefault()
            )


        try {

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU
            ) {

                geocoder.getFromLocation(
                    latitude,
                    longitude,
                    1
                ) { addresses ->

                    activity?.runOnUiThread {
                        if (isAdded) {
                            updateReadableLocation(
                                addresses
                            )
                        }
                    }
                }


            } else {

                @Suppress("DEPRECATION")

                val addresses =
                    geocoder.getFromLocation(
                        latitude,
                        longitude,
                        1
                    )


                updateReadableLocation(
                    addresses ?: emptyList()
                )
            }


        } catch (e: Exception) {

            if (isAdded) {
                Toast.makeText(
                    requireContext(),
                    "Unable to get address from location",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    // ==========================================
    // UPDATE READABLE LOCATION
    // ==========================================

    private fun updateReadableLocation(
        addresses: List<Address>
    ) {

        if (addresses.isNotEmpty()) {

            val address =
                addresses[0]


            // Full readable address

            val readableAddress =
                address.getAddressLine(0)
                    ?: ""


            // Extract City Area
            // First preference: subLocality
            // Second preference: locality

            selectedCityArea =
                address.subLocality
                    ?: address.locality
                            ?: ""


            // Extract State

            selectedState =
                address.adminArea
                    ?: ""


            // Save full address

            selectedAddress =
                readableAddress


            // Display location

            if (isAdded) {

                tvCurrentLocation.text =
                    "$readableAddress\n\n" +
                            "Area: $selectedCityArea\n" +
                            "State: $selectedState"


                Toast.makeText(
                    requireContext(),
                    "Location detected successfully",
                    Toast.LENGTH_SHORT
                ).show()
            }


        } else {

            if (isAdded) {

                Toast.makeText(
                    requireContext(),
                    "Unable to find your address",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}