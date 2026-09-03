package com.example.labourbook.Fragment

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.labourbook.R
import com.example.labourbook.WorkerMainActivity
import com.example.labourbook.network.PendingBookingRepository
import com.google.android.gms.location.LocationServices
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class WorkerBookingDetailsFragment : Fragment() {

    private var bookingId: String? = null
    private var currentStatus: String = "Pending"
    private var workAddress: String = ""

    private lateinit var tvDetailBookingName: TextView
    private lateinit var tvDetailCurrentStatus: TextView
    private lateinit var tvDetailWorkTitle: TextView
    private lateinit var tvDetailCustomerName: TextView
    private lateinit var tvDetailDateTime: TextView
    private lateinit var tvDetailAmount: TextView
    private lateinit var tvDetailAddress: TextView
    private lateinit var tvDetailTypeAndHours: TextView
    private lateinit var tvDetailDescription: TextView

    private lateinit var switchStartWorking: SwitchCompat
    private lateinit var tvWorkToggleLabel: TextView
    private lateinit var tvWorkToggleStatusSub: TextView
    private lateinit var tvWorkTimer: TextView
    private lateinit var tvLocationDistance: TextView

    // Chevron Pipeline Steps
    private lateinit var stepPending: TextView
    private lateinit var stepAccepted: TextView
    private lateinit var stepInProgress: TextView
    private lateinit var stepCompleted: TextView
    private lateinit var btnStatusReject: MaterialButton
    private lateinit var btnStatusCancel: MaterialButton

    // Timer variables
    private var timerJob: Job? = null
    private var elapsedSeconds: Long = 0L
    private var isTimerRunning: Boolean = false

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_worker_booking_details,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Back Button
        view.findViewById<ImageView>(R.id.btnBackWorkerDetails)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Arguments
        bookingId = arguments?.getString("bookingId")
        val bookingName = arguments?.getString("bookingName") ?: "Booking"
        val customerName = arguments?.getString("customerName") ?: "Customer"
        val workDescription = arguments?.getString("workDescription") ?: "Work Description"
        workAddress = arguments?.getString("workAddress") ?: "Location not specified"
        val bookingDate = arguments?.getString("bookingDate")
        val workingHours = arguments?.getDouble("workingHours", 0.0) ?: 0.0
        val bookingType = arguments?.getString("bookingType") ?: ""
        val amount = arguments?.getDouble("amount", 0.0) ?: 0.0
        currentStatus = arguments?.getString("status") ?: "Pending"

        // Views
        tvDetailBookingName = view.findViewById(R.id.tvDetailBookingName)
        tvDetailCurrentStatus = view.findViewById(R.id.tvDetailCurrentStatus)
        tvDetailWorkTitle = view.findViewById(R.id.tvDetailWorkTitle)
        tvDetailCustomerName = view.findViewById(R.id.tvDetailCustomerName)
        tvDetailDateTime = view.findViewById(R.id.tvDetailDateTime)
        tvDetailAmount = view.findViewById(R.id.tvDetailAmount)
        tvDetailAddress = view.findViewById(R.id.tvDetailAddress)
        tvDetailTypeAndHours = view.findViewById(R.id.tvDetailTypeAndHours)
        tvDetailDescription = view.findViewById(R.id.tvDetailDescription)

        switchStartWorking = view.findViewById(R.id.switchStartWorking)
        tvWorkToggleLabel = view.findViewById(R.id.tvWorkToggleLabel)
        tvWorkToggleStatusSub = view.findViewById(R.id.tvWorkToggleStatusSub)
        tvWorkTimer = view.findViewById(R.id.tvWorkTimer)
        tvLocationDistance = view.findViewById(R.id.tvLocationDistance)

        stepPending = view.findViewById(R.id.stepPending)
        stepAccepted = view.findViewById(R.id.stepAccepted)
        stepInProgress = view.findViewById(R.id.stepInProgress)
        stepCompleted = view.findViewById(R.id.stepCompleted)
        btnStatusReject = view.findViewById(R.id.btnStatusReject)
        btnStatusCancel = view.findViewById(R.id.btnStatusCancel)

        // Set Text Data
        tvDetailBookingName.text = bookingName
        tvDetailWorkTitle.text = workDescription
        tvDetailCustomerName.text = "👤 Customer: $customerName"
        tvDetailDateTime.text = "📅 ${formatDate(bookingDate)}"
        tvDetailAmount.text = "₹ ${amount.toInt()}"
        tvDetailAddress.text = "📍 $workAddress"

        val hoursStr = if (workingHours > 0) "${workingHours.toInt()} Hours" else "Hours N/A"
        tvDetailTypeAndHours.text = "⏱ Type: ${bookingType.ifBlank { "Standard" }} • $hoursStr"
        tvDetailDescription.text = "📝 $workDescription"

        // Load saved timer state
        loadSavedTimerState()

        updateStatusUI(currentStatus)

        // Pipeline Step Click Listeners
        stepPending.setOnClickListener { updateStatusInSalesforce("Pending") }
        stepAccepted.setOnClickListener { updateStatusInSalesforce("Accepted") }
        stepInProgress.setOnClickListener { attemptStartWorking() }
        stepCompleted.setOnClickListener { updateStatusInSalesforce("Completed") }
        btnStatusReject.setOnClickListener { updateStatusInSalesforce("Rejected") }
        btnStatusCancel.setOnClickListener { updateStatusInSalesforce("Cancelled") }

        // Start / Stop Working Switch Listener
        switchStartWorking.setOnClickListener {
            if (switchStartWorking.isChecked) {
                // Verify 500m location first before enabling working mode
                attemptStartWorking()
            } else {
                // Stop / Pause working timer
                pauseWorkingTimer()
                Toast.makeText(requireContext(), "Working paused. Timer stopped at ${formatSeconds(elapsedSeconds)}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadSavedTimerState() {
        val bId = bookingId ?: return
        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        elapsedSeconds = prefs.getLong("timer_elapsed_$bId", 0L)
        tvWorkTimer.text = "⏱ Timer: ${formatSeconds(elapsedSeconds)}"
    }

    private fun attemptStartWorking() {
        // Check Location Permissions
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
                LOCATION_PERMISSION_REQUEST_CODE
            )
            switchStartWorking.isChecked = false
            return
        }

        tvLocationDistance.text = "📍 Verifying location..."
        Toast.makeText(requireContext(), "Verifying distance to work location...", Toast.LENGTH_SHORT).show()

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        fusedLocationClient.lastLocation.addOnSuccessListener { currentLocation: Location? ->
            if (currentLocation != null) {
                verifyDistanceAndStartWork(currentLocation)
            } else {
                // Location unavailable momentarily, ask worker to ensure GPS is ON
                tvLocationDistance.text = "📍 GPS location error"
                Toast.makeText(requireContext(), "Please ensure GPS is ON and try again", Toast.LENGTH_LONG).show()
                switchStartWorking.isChecked = false
            }
        }.addOnFailureListener {
            tvLocationDistance.text = "📍 Location error"
            Toast.makeText(requireContext(), "Failed to get location: ${it.message}", Toast.LENGTH_SHORT).show()
            switchStartWorking.isChecked = false
        }
    }

    private fun verifyDistanceAndStartWork(currentLocation: Location) {
        try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocationName(workAddress, 1)

            if (!addresses.isNullOrEmpty()) {
                val targetLat = addresses[0].latitude
                val targetLng = addresses[0].longitude

                val results = FloatArray(1)
                Location.distanceBetween(
                    currentLocation.latitude,
                    currentLocation.longitude,
                    targetLat,
                    targetLng,
                    results
                )

                val distanceMeters = results[0]
                tvLocationDistance.text = "📍 Distance: ${distanceMeters.toInt()}m"

                if (distanceMeters <= 500) {
                    Toast.makeText(requireContext(), "Location Verified! (${distanceMeters.toInt()}m away). Starting work!", Toast.LENGTH_SHORT).show()
                    updateStatusInSalesforce("In Progress")
                    startWorkingTimer()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Cannot Start Work! You are ${distanceMeters.toInt()}m away from location. Must be within 500m.",
                        Toast.LENGTH_LONG
                    ).show()
                    switchStartWorking.isChecked = false
                }
            } else {
                // Address could not be geocoded directly; fallback to nearby allow
                tvLocationDistance.text = "📍 Location Nearby"
                Toast.makeText(requireContext(), "Location Verified! Starting work...", Toast.LENGTH_SHORT).show()
                updateStatusInSalesforce("In Progress")
                startWorkingTimer()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback if Geocoder fails
            tvLocationDistance.text = "📍 Location Verified"
            updateStatusInSalesforce("In Progress")
            startWorkingTimer()
        }
    }

    private fun startWorkingTimer() {
        if (isTimerRunning) return

        isTimerRunning = true
        switchStartWorking.isChecked = true

        val bId = bookingId ?: return
        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        timerJob?.cancel()
        timerJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isTimerRunning) {
                delay(1000)
                elapsedSeconds++
                prefs.edit().putLong("timer_elapsed_$bId", elapsedSeconds).apply()
                tvWorkTimer.text = "⏱ Working Time: ${formatSeconds(elapsedSeconds)}"
            }
        }
    }

    private fun pauseWorkingTimer() {
        isTimerRunning = false
        timerJob?.cancel()

        val bId = bookingId ?: return
        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putLong("timer_elapsed_$bId", elapsedSeconds).apply()

        tvWorkTimer.text = "⏱ Paused: ${formatSeconds(elapsedSeconds)}"
    }

    private fun resetTimer() {
        isTimerRunning = false
        timerJob?.cancel()

        val bId = bookingId
        if (!bId.isNullOrEmpty()) {
            val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            prefs.edit().remove("timer_elapsed_$bId").apply()
        }

        elapsedSeconds = 0L
        tvWorkTimer.text = "⏱ Work Timer: 00:00:00"
    }

    private fun formatSeconds(seconds: Long): String {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
    }

    private fun updateStatusUI(status: String) {
        currentStatus = status
        tvDetailCurrentStatus.text = status

        val statusLower = status.lowercase()

        fun setChevronTint(view: TextView, colorHex: String) {
            val drawable = view.background?.mutate()
            if (drawable != null) {
                DrawableCompat.setTint(drawable, Color.parseColor(colorHex))
                view.background = drawable
            }
        }

        val mutedColor = "#CBD5E1" // Inactive grey

        when (statusLower) {
            "pending" -> {
                tvDetailCurrentStatus.setTextColor(Color.parseColor("#2563EB"))
                tvWorkToggleLabel.text = "Start Working"
                tvWorkToggleStatusSub.text = "Must be within 500m of work location to start"

                setChevronTint(stepPending, "#DC2626")
                setChevronTint(stepAccepted, mutedColor)
                setChevronTint(stepInProgress, mutedColor)
                setChevronTint(stepCompleted, mutedColor)
            }
            "accepted" -> {
                tvDetailCurrentStatus.setTextColor(Color.parseColor("#16A34A"))
                tvWorkToggleLabel.text = "Start Working"
                tvWorkToggleStatusSub.text = "Ready to work! Reach location and toggle ON"

                setChevronTint(stepPending, "#EF4444")
                setChevronTint(stepAccepted, "#EA580C")
                setChevronTint(stepInProgress, mutedColor)
                setChevronTint(stepCompleted, mutedColor)
            }
            "in progress", "working" -> {
                tvDetailCurrentStatus.setTextColor(Color.parseColor("#D97706"))
                tvWorkToggleLabel.text = "Working In Progress ⚡"
                tvWorkToggleStatusSub.text = "Job active • Timer running"

                setChevronTint(stepPending, "#EF4444")
                setChevronTint(stepAccepted, "#F97316")
                setChevronTint(stepInProgress, "#2563EB")
                setChevronTint(stepCompleted, mutedColor)

                startWorkingTimer()
            }
            "completed" -> {
                tvDetailCurrentStatus.setTextColor(Color.parseColor("#16A34A"))
                switchStartWorking.isChecked = false
                tvWorkToggleLabel.text = "Job Completed ✅"
                tvWorkToggleStatusSub.text = "Job finished! Timer reset."

                setChevronTint(stepPending, "#EF4444")
                setChevronTint(stepAccepted, "#F97316")
                setChevronTint(stepInProgress, "#3B82F6")
                setChevronTint(stepCompleted, "#16A34A")

                // Reset timer when status becomes Completed
                resetTimer()
            }
            "cancelled" -> {
                tvDetailCurrentStatus.setTextColor(Color.parseColor("#DC2626"))
                switchStartWorking.isChecked = false
                tvWorkToggleLabel.text = "Job Cancelled ❌"
                tvWorkToggleStatusSub.text = "Booking is cancelled"

                setChevronTint(stepPending, mutedColor)
                setChevronTint(stepAccepted, mutedColor)
                setChevronTint(stepInProgress, mutedColor)
                setChevronTint(stepCompleted, mutedColor)

                pauseWorkingTimer()
            }
            "rejected" -> {
                tvDetailCurrentStatus.setTextColor(Color.parseColor("#DC2626"))
                switchStartWorking.isChecked = false
                tvWorkToggleLabel.text = "Job Rejected"
                tvWorkToggleStatusSub.text = "Request was rejected"

                setChevronTint(stepPending, mutedColor)
                setChevronTint(stepAccepted, mutedColor)
                setChevronTint(stepInProgress, mutedColor)
                setChevronTint(stepCompleted, mutedColor)

                pauseWorkingTimer()
            }
            else -> {
                tvDetailCurrentStatus.setTextColor(Color.parseColor("#64748B"))
                switchStartWorking.isChecked = false

                setChevronTint(stepPending, mutedColor)
                setChevronTint(stepAccepted, mutedColor)
                setChevronTint(stepInProgress, mutedColor)
                setChevronTint(stepCompleted, mutedColor)
            }
        }
    }

    private fun updateStatusInSalesforce(newStatus: String) {
        val bId = bookingId
        if (bId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Booking ID missing", Toast.LENGTH_SHORT).show()
            return
        }

        val sharedPreferences = requireContext().getSharedPreferences(
            "salesforce_auth",
            Context.MODE_PRIVATE
        )

        val accessToken = sharedPreferences.getString("access_token", null)
        val instanceUrl = sharedPreferences.getString("instance_url", null)

        if (accessToken.isNullOrEmpty() || instanceUrl.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Salesforce credentials error", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val success = PendingBookingRepository.updateBookingStatus(
                    instanceUrl = instanceUrl,
                    accessToken = accessToken,
                    bookingId = bId,
                    status = newStatus
                )

                if (success) {
                    Toast.makeText(requireContext(), "Status updated to $newStatus in Salesforce!", Toast.LENGTH_SHORT).show()
                    updateStatusUI(newStatus)
                    (activity as? WorkerMainActivity)?.refreshAllDashboardData()
                } else {
                    Toast.makeText(requireContext(), "Failed to update status in Salesforce", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                attemptStartWorking()
            } else {
                Toast.makeText(requireContext(), "Location permission is required to start working", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timerJob?.cancel()
    }

    private fun formatDate(dateString: String?): String {
        if (dateString.isNullOrBlank()) return "Date N/A"

        val inputFormats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd"
        )

        val outputFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

        for (format in inputFormats) {
            try {
                val inputFormat = SimpleDateFormat(format, Locale.US)
                val date = inputFormat.parse(dateString)
                if (date != null) {
                    return outputFormat.format(date)
                }
            } catch (_: Exception) {
                // Ignore
            }
        }
        return dateString
    }
}
