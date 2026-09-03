package com.example.labourbook.Fragment

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.labourbook.R
import com.example.labourbook.model.BookingRequest
import com.example.labourbook.network.BookingRepository
import com.example.labourbook.network.CustomerRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class fragment_booking : Fragment() {

    private var selectedDate: Calendar? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_booking2,
            container,
            false
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        // =============================
        // GET SELECTED WORKER DETAILS
        // =============================

        val workerId = arguments?.getString("workerId")
        val workerName = arguments?.getString("workerName")
        val workerCategory = arguments?.getString("workerCategory")
        val workerRate = arguments?.getString("workerRate")
        val workerHourlyRate = arguments?.getDouble("workerHourlyRate") ?: 0.0
        val workerDailyRate = arguments?.getDouble("workerDailyRate") ?: 0.0

        // =============================
        // CONNECT XML VIEWS
        // =============================

        val tvWorkerName =
            view.findViewById<TextView>(R.id.tvWorkerName)

        val tvWorkerCategory =
            view.findViewById<TextView>(R.id.tvWorkerCategory)

        val tvPrice =
            view.findViewById<TextView>(R.id.tvPrice)

        val tvSelectedDate =
            view.findViewById<TextView>(R.id.tvSelectedDate)

        val tvSelectedTime =
            view.findViewById<TextView>(R.id.tvSelectedTime)

        val tvSummaryDate =
            view.findViewById<TextView>(R.id.tvSummaryDate)

        val tvSummaryTime =
            view.findViewById<TextView>(R.id.tvSummaryTime)

        val tvSummaryType =
            view.findViewById<TextView>(R.id.tvSummaryType)

        val tvTotal =
            view.findViewById<TextView>(R.id.tvTotal)

        val etAddress =
            view.findViewById<EditText>(R.id.etAddress)

        val etWorkDescription =
            view.findViewById<EditText>(R.id.etWorkDescription)

        val rgBookingType =
            view.findViewById<RadioGroup>(R.id.rgBookingType)

        val layoutHoursInput =
            view.findViewById<View>(R.id.layoutHoursInput)

        val btnMinusHour =
            view.findViewById<View>(R.id.btnMinusHour)

        val btnPlusHour =
            view.findViewById<View>(R.id.btnPlusHour)

        val tvWorkingHours =
            view.findViewById<TextView>(R.id.tvWorkingHours)

        // Pre-fill initial address if available
        val appPrefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val salesforcePrefs = requireContext().getSharedPreferences("salesforce_auth", Context.MODE_PRIVATE)
        var savedLocation = appPrefs.getString("selected_location", null)
        if (savedLocation.isNullOrBlank()) {
            savedLocation = salesforcePrefs.getString("customer_address", null)
        }
        if (!savedLocation.isNullOrBlank() && etAddress.text.toString().isBlank()) {
            etAddress.setText(savedLocation)
        }

        // =============================
        // DISPLAY WORKER DETAILS
        // =============================

        tvWorkerName.text =
            workerName ?: "Worker"

        tvWorkerCategory.text =
            workerCategory ?: "Labour Service"

        // =============================
        // SELECT LOCATION ON MAP CLICK
        // =============================

        view.findViewById<View>(R.id.btnSelectLocationOnMap)
            ?.setOnClickListener {
                findNavController().navigate(
                    R.id.action_confirmBooking_to_mapLocationPicker
                )
            }

        // =============================
        // RATE & DURATION CALCULATION
        // =============================

        val effectiveHourlyRate = if (workerHourlyRate > 0) {
            workerHourlyRate
        } else {
            workerRate
                ?.replace("₹", "")
                ?.replace("/hr", "")
                ?.replace("Years Exp.", "")
                ?.trim()
                ?.toDoubleOrNull()
                ?: 350.0
        }

        val effectiveDailyRate = if (workerDailyRate > 0) {
            workerDailyRate
        } else {
            effectiveHourlyRate * 8.0
        }

        var isHourly = true
        var estimatedHours = 2.0
        var calculatedAmount = effectiveHourlyRate * estimatedHours

        fun recalculatePriceAndAmount() {
            if (isHourly) {
                layoutHoursInput.visibility = View.VISIBLE
                calculatedAmount = effectiveHourlyRate * estimatedHours
                tvPrice.text = "₹${effectiveHourlyRate.toInt()} / hr"
                tvSummaryType.text = "Duration: ${estimatedHours.toInt()} hours (Hourly)"
                tvTotal.text = "Total Amount: ₹${calculatedAmount.toInt()}"
            } else {
                layoutHoursInput.visibility = View.GONE
                estimatedHours = 8.0
                calculatedAmount = effectiveDailyRate
                tvPrice.text = "₹${effectiveDailyRate.toInt()} / full day"
                tvSummaryType.text = "Duration: Full Day (8 hours)"
                tvTotal.text = "Total Amount: ₹${calculatedAmount.toInt()}"
            }
        }

        rgBookingType.setOnCheckedChangeListener { _, checkedId ->
            isHourly = (checkedId == R.id.rbHourly)
            recalculatePriceAndAmount()
        }

        btnMinusHour.setOnClickListener {
            if (estimatedHours > 1.0) {
                estimatedHours -= 1.0
                tvWorkingHours.text = estimatedHours.toInt().toString()
                recalculatePriceAndAmount()
            }
        }

        btnPlusHour.setOnClickListener {
            if (estimatedHours < 24.0) {
                estimatedHours += 1.0
                tvWorkingHours.text = estimatedHours.toInt().toString()
                recalculatePriceAndAmount()
            }
        }

        recalculatePriceAndAmount()

        // =============================
        // BACK BUTTON
        // =============================

        view.findViewById<View>(R.id.btnBack)
            .setOnClickListener {
                findNavController().navigateUp()
            }

        // =============================
        // DATE PICKER
        // =============================

        view.findViewById<View>(R.id.cardDate)
            .setOnClickListener {

                val calendar = Calendar.getInstance()

                DatePickerDialog(
                    requireContext(),
                    { _, year, month, dayOfMonth ->

                        if (selectedDate == null) {
                            selectedDate = Calendar.getInstance()
                        }

                        selectedDate?.set(
                            Calendar.YEAR,
                            year
                        )

                        selectedDate?.set(
                            Calendar.MONTH,
                            month
                        )

                        selectedDate?.set(
                            Calendar.DAY_OF_MONTH,
                            dayOfMonth
                        )

                        val dateText =
                            SimpleDateFormat(
                                "dd MMM yyyy",
                                Locale.getDefault()
                            ).format(
                                selectedDate!!.time
                            )

                        tvSelectedDate.text = dateText
                        tvSummaryDate.text = dateText
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }

        // =============================
        // TIME PICKER
        // =============================

        view.findViewById<View>(R.id.cardTime)
            .setOnClickListener {

                val calendar = Calendar.getInstance()

                TimePickerDialog(
                    requireContext(),
                    { _, hourOfDay, minute ->

                        if (selectedDate == null) {
                            selectedDate = Calendar.getInstance()
                        }

                        selectedDate?.set(
                            Calendar.HOUR_OF_DAY,
                            hourOfDay
                        )

                        selectedDate?.set(
                            Calendar.MINUTE,
                            minute
                        )

                        selectedDate?.set(
                            Calendar.SECOND,
                            0
                        )

                        selectedDate?.set(
                            Calendar.MILLISECOND,
                            0
                        )

                        val timeText =
                            SimpleDateFormat(
                                "hh:mm a",
                                Locale.getDefault()
                            ).format(
                                selectedDate!!.time
                            )

                        tvSelectedTime.text = timeText
                        tvSummaryTime.text = timeText
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    false
                ).show()
            }

        // =============================
        // CONFIRM BOOKING
        // =============================

        view.findViewById<View>(R.id.btnConfirmBooking)
            .setOnClickListener {

                val address =
                    etAddress.text.toString().trim()

                val description =
                    etWorkDescription.text
                        .toString()
                        .trim()

                // =============================
                // VALIDATION
                // =============================

                if (workerId.isNullOrEmpty()) {

                    Toast.makeText(
                        requireContext(),
                        "Worker information not found",
                        Toast.LENGTH_LONG
                    ).show()

                    return@setOnClickListener
                }

                if (selectedDate == null) {

                    Toast.makeText(
                        requireContext(),
                        "Please select booking date and time",
                        Toast.LENGTH_LONG
                    ).show()

                    return@setOnClickListener
                }

                if (address.isEmpty()) {

                    etAddress.error =
                        "Work address is required"

                    etAddress.requestFocus()

                    return@setOnClickListener
                }

                if (description.isEmpty()) {

                    etWorkDescription.error =
                        "Work description is required"

                    etWorkDescription.requestFocus()

                    return@setOnClickListener
                }

                // =============================
                // GET SALESFORCE DETAILS
                // =============================

                val preferences =
                    requireContext().getSharedPreferences(
                        "salesforce_auth",
                        Context.MODE_PRIVATE
                    )

                val accessToken =
                    preferences.getString(
                        "access_token",
                        null
                    )

                val instanceUrl =
                    preferences.getString(
                        "instance_url",
                        null
                    )

                if (
                    accessToken == null ||
                    instanceUrl == null
                ) {

                    Toast.makeText(
                        requireContext(),
                        "Please connect Salesforce first",
                        Toast.LENGTH_LONG
                    ).show()

                    return@setOnClickListener
                }

                viewLifecycleOwner.lifecycleScope.launch {

                    var finalCustomerId = preferences.getString("customer_id", null)

                    // If customer_id is missing, auto-query or create in Salesforce
                    if (finalCustomerId.isNullOrEmpty()) {
                        val firebaseUser = Firebase.auth.currentUser
                        if (firebaseUser != null) {
                            val phone = preferences.getString("customer_phone", "") ?: ""
                            finalCustomerId = CustomerRepository.ensureCustomerId(
                                instanceUrl = instanceUrl,
                                accessToken = accessToken,
                                firebaseUser = firebaseUser,
                                phone = phone
                            )
                            if (!finalCustomerId.isNullOrEmpty()) {
                                preferences.edit().putString("customer_id", finalCustomerId).apply()
                            }
                        }
                    }

                    if (finalCustomerId.isNullOrEmpty()) {
                        Toast.makeText(
                            requireContext(),
                            "Customer record not found in Salesforce. Please check Salesforce connection.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@launch
                    }

                    // =============================
                    // FORMAT DATE FOR SALESFORCE
                    // =============================

                    val salesforceDate =
                        SimpleDateFormat(
                            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                            Locale.US
                        ).format(
                            selectedDate!!.time
                        )

                    // =============================
                    // FORMAT DATE FOR SUCCESS SCREEN
                    // =============================

                    val displayDateTime =
                        SimpleDateFormat(
                            "dd MMM yyyy, hh:mm a",
                            Locale.getDefault()
                        ).format(
                            selectedDate!!.time
                        )

                    // =============================
                    // CREATE BOOKING REQUEST
                    // =============================

                    val booking =
                        BookingRequest(
                            Worker__c = workerId,
                            L_Customer__c = finalCustomerId,
                            Booking_Date__c = salesforceDate,
                            Booking_Labour_Status__c = "Pending",
                            Amount__c = calculatedAmount,
                            Work_Address__c = address,
                            Work_Description__c = description,
                            Working_Hour__c = estimatedHours,
                            Booking_Type__c = if (isHourly) "Hourly" else "Full Day(8 Hrs)"
                        )

                    // =============================
                    // SEND TO SALESFORCE
                    // =============================

                    createBooking(
                        instanceUrl = instanceUrl,
                        accessToken = accessToken,
                        booking = booking,
                        workerName = workerName,
                        displayDateTime = displayDateTime,
                        bookingAddress = address
                    )
                }
            }
    }

    override fun onResume() {
        super.onResume()
        val etAddress = view?.findViewById<EditText>(R.id.etAddress)
        if (etAddress != null) {
            val appPrefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val salesforcePrefs = requireContext().getSharedPreferences("salesforce_auth", Context.MODE_PRIVATE)

            var savedLocation = appPrefs.getString("selected_location", null)
            if (savedLocation.isNullOrBlank()) {
                savedLocation = salesforcePrefs.getString("customer_address", null)
            }

            if (!savedLocation.isNullOrBlank()) {
                etAddress.setText(savedLocation)
            }
        }
    }

    // =============================
    // CREATE BOOKING
    // =============================

    private fun createBooking(
        instanceUrl: String,
        accessToken: String,
        booking: BookingRequest,
        workerName: String?,
        displayDateTime: String,
        bookingAddress: String
    ) {

        viewLifecycleOwner.lifecycleScope.launch {

            try {

                val response =
                    BookingRepository.createBooking(
                        instanceUrl = instanceUrl,
                        accessToken = accessToken,
                        booking = booking
                    )

                if (
                    response != null &&
                    response.success
                ) {

                    Toast.makeText(
                        requireContext(),
                        "Booking created successfully!",
                        Toast.LENGTH_SHORT
                    ).show()

                    // =============================
                    // PASS DATA TO SUCCESS SCREEN
                    // =============================

                    val bundle =
                        Bundle().apply {

                            // Salesforce booking record ID
                            putString(
                                "bookingId",
                                response.id
                            )

                            // Worker name
                            putString(
                                "workerName",
                                workerName
                            )

                            // Selected date and time
                            putString(
                                "bookingDateTime",
                                displayDateTime
                            )

                            // Work address
                            putString(
                                "bookingAddress",
                                bookingAddress
                            )
                        }

                    findNavController().navigate(
                        R.id.action_confirmBooking_to_bookingSuccess,
                        bundle
                    )

                } else {

                    Toast.makeText(
                        requireContext(),
                        "Failed to create booking",
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
}
