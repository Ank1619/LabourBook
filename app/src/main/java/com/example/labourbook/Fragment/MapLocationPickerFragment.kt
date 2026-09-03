package com.example.labourbook.Fragment

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.labourbook.R
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MapLocationPickerFragment : Fragment(), OnMapReadyCallback {

    private var googleMap: GoogleMap? = null

    private lateinit var etSearchLocationMap: EditText
    private lateinit var btnSearchMap: ImageView
    private lateinit var tvPickedReadableAddress: TextView
    private lateinit var btnConfirmMapLocation: MaterialButton
    private lateinit var btnMapCurrentGps: MaterialButton

    private var currentReadableAddress: String = ""
    private var currentLatLng: LatLng? = null

    companion object {
        private const val MAP_LOCATION_PERMISSION_CODE = 2002
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_map_location_picker,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Views
        etSearchLocationMap = view.findViewById(R.id.etSearchLocationMap)
        btnSearchMap = view.findViewById(R.id.btnSearchMap)
        tvPickedReadableAddress = view.findViewById(R.id.tvPickedReadableAddress)
        btnConfirmMapLocation = view.findViewById(R.id.btnConfirmMapLocation)
        btnMapCurrentGps = view.findViewById(R.id.btnMapCurrentGps)

        // Back Button
        view.findViewById<ImageView>(R.id.btnBackMapPicker)?.setOnClickListener {
            findNavController().navigateUp()
        }

        // Initialize Map
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        // Search Handlers
        btnSearchMap.setOnClickListener {
            performLocationSearch()
        }

        etSearchLocationMap.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performLocationSearch()
                true
            } else false
        }

        // GPS Location Click
        btnMapCurrentGps.setOnClickListener {
            fetchGpsLocation()
        }

        // Confirm Location Click
        btnConfirmMapLocation.setOnClickListener {
            if (currentReadableAddress.isNotBlank()) {
                saveSelectedLocation(currentReadableAddress)
            } else {
                Toast.makeText(requireContext(), "Please select a valid location on map", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Default location (e.g. Mumbai/India)
        val defaultLatLng = LatLng(19.0760, 72.8777)
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLatLng, 12f))

        // On Map Camera Idle (user stopped dragging/panning map)
        googleMap?.setOnCameraIdleListener {
            val centerLatLng = googleMap?.cameraPosition?.target
            if (centerLatLng != null) {
                currentLatLng = centerLatLng
                reverseGeocodeLatLng(centerLatLng.latitude, centerLatLng.longitude)
            }
        }

        // Automatically fetch GPS location on start
        fetchGpsLocation()
    }

    private fun fetchGpsLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                MAP_LOCATION_PERMISSION_CODE
            )
            return
        }

        val fusedClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        fusedClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val latLng = LatLng(location.latitude, location.longitude)
                currentLatLng = latLng
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
                reverseGeocodeLatLng(location.latitude, location.longitude)
            } else {
                Toast.makeText(requireContext(), "GPS location unavailable, search location above", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performLocationSearch() {
        val query = etSearchLocationMap.text.toString().trim()
        if (query.isBlank()) {
            etSearchLocationMap.error = "Enter address or landmark"
            return
        }

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(query, 1)

                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val latLng = LatLng(addr.latitude, addr.longitude)
                    val fullFormattedAddress = formatAddressString(addr)

                    withContext(Dispatchers.Main) {
                        currentLatLng = latLng
                        currentReadableAddress = fullFormattedAddress
                        tvPickedReadableAddress.text = fullFormattedAddress
                        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
                        Toast.makeText(requireContext(), "Location found!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Location not found. Try another landmark/city.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Search error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun reverseGeocodeLatLng(lat: Double, lng: Double) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)

                if (!addresses.isNullOrEmpty()) {
                    val fullFormattedAddress = formatAddressString(addresses[0])

                    withContext(Dispatchers.Main) {
                        currentReadableAddress = fullFormattedAddress
                        tvPickedReadableAddress.text = fullFormattedAddress
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        currentReadableAddress = "Lat: ${String.format(Locale.US, "%.4f", lat)}, Lng: ${String.format(Locale.US, "%.4f", lng)}"
                        tvPickedReadableAddress.text = currentReadableAddress
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    currentReadableAddress = "Lat: ${String.format(Locale.US, "%.4f", lat)}, Lng: ${String.format(Locale.US, "%.4f", lng)}"
                    tvPickedReadableAddress.text = currentReadableAddress
                }
            }
        }
    }

    private fun formatAddressString(address: android.location.Address): String {
        val parts = mutableListOf<String>()

        if (!address.featureName.isNullOrBlank() && address.featureName != address.subLocality) {
            parts.add(address.featureName)
        }
        if (!address.thoroughfare.isNullOrBlank()) {
            parts.add(address.thoroughfare)
        }
        if (!address.subLocality.isNullOrBlank()) {
            parts.add(address.subLocality)
        }
        if (!address.locality.isNullOrBlank()) {
            parts.add(address.locality)
        }
        if (!address.adminArea.isNullOrBlank()) {
            parts.add(address.adminArea)
        }
        if (!address.postalCode.isNullOrBlank()) {
            parts.add(address.postalCode)
        }

        return if (parts.isNotEmpty()) {
            parts.distinct().joinToString(", ")
        } else {
            address.getAddressLine(0) ?: "Selected Map Location"
        }
    }

    private fun saveSelectedLocation(address: String) {
        val appPrefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val salesforcePrefs = requireContext().getSharedPreferences("salesforce_auth", Context.MODE_PRIVATE)

        appPrefs.edit().putString("selected_location", address).apply()
        salesforcePrefs.edit().putString("customer_address", address).apply()

        Toast.makeText(requireContext(), "Location set for booking!", Toast.LENGTH_SHORT).show()
        findNavController().navigateUp()
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MAP_LOCATION_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchGpsLocation()
            }
        }
    }
}
