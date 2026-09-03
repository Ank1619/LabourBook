package com.example.labourbook.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.labourbook.R
import java.text.SimpleDateFormat
import java.util.Locale

class BookingDetailsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_booking_details,
            container,
            false
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        // Receive booking data from BookingFragment
        val bookingName =
            arguments?.getString("bookingName")

        val bookingStatus =
            arguments?.getString("bookingStatus")

        val workerName =
            arguments?.getString("workerName")

        val bookingDate =
            arguments?.getString("bookingDate")

        val bookingAddress =
            arguments?.getString("bookingAddress")

        val bookingAmount =
            arguments?.getDouble("bookingAmount", 0.0)

        val bookingDescription =
            arguments?.getString("bookingDescription")

        // Views
        val tvBookingName =
            view.findViewById<TextView>(R.id.tvBookingName)

        val tvBookingStatus =
            view.findViewById<TextView>(R.id.tvBookingStatus)

        val tvWorkerName =
            view.findViewById<TextView>(R.id.tvWorkerName)

        val tvBookingDate =
            view.findViewById<TextView>(R.id.tvBookingDate)

        val tvBookingAddress =
            view.findViewById<TextView>(R.id.tvBookingAddress)

        val tvBookingAmount =
            view.findViewById<TextView>(R.id.tvBookingAmount)

        val tvBookingDescription =
            view.findViewById<TextView>(R.id.tvBookingDescription)

        // Set booking data
        tvBookingName.text =
            bookingName ?: "Booking"

        tvBookingStatus.text =
            bookingStatus ?: "Pending"

        tvWorkerName.text =
            workerName ?: "Worker not available"

        tvBookingDate.text =
            formatDate(bookingDate)

        tvBookingAddress.text =
            bookingAddress ?: "Address not available"

        tvBookingAmount.text =
            "₹${bookingAmount ?: 0}"

        tvBookingDescription.text =
            bookingDescription ?: "No description available"

        // Back button
        view.findViewById<View>(R.id.btnBack)
            .setOnClickListener {

                findNavController().navigateUp()
            }
    }

    private fun formatDate(
        dateString: String?
    ): String {

        if (dateString.isNullOrBlank()) {
            return "Date not available"
        }

        val inputFormats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ssZ"
        )

        val outputFormat =
            SimpleDateFormat(
                "dd MMM yyyy, hh:mm a",
                Locale.getDefault()
            )

        for (format in inputFormats) {

            try {

                val inputFormat =
                    SimpleDateFormat(
                        format,
                        Locale.US
                    )

                val date =
                    inputFormat.parse(dateString)

                if (date != null) {
                    return outputFormat.format(date)
                }

            } catch (e: Exception) {
                // Try next date format
            }
        }

        return dateString
    }
}