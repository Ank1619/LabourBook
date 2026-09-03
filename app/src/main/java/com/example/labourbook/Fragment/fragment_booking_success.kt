package com.example.labourbook.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.labourbook.R

class fragment_booking_success : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_booking_success,
            container,
            false
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        // Receive booking details
        val bookingId = arguments?.getString("bookingId")
        val workerName = arguments?.getString("workerName")
        val bookingDateTime = arguments?.getString("bookingDateTime")
        val bookingAddress = arguments?.getString("bookingAddress")

        // Display booking details
        view.findViewById<TextView>(R.id.tvBookingId).text =
            bookingId ?: "Booking Confirmed"

        view.findViewById<TextView>(R.id.tvWorkerName).text =
            workerName ?: "Worker"

        view.findViewById<TextView>(R.id.tvBookingDateTime).text =
            bookingDateTime ?: "Not selected"

        view.findViewById<TextView>(R.id.tvBookingAddress).text =
            bookingAddress ?: "Address not provided"

        // My Bookings
        view.findViewById<View>(R.id.btnMyBookings).setOnClickListener {
            findNavController().navigate(
                R.id.action_bookingSuccess_to_bookingFragment
            )
        }

        // Go Home
        view.findViewById<View>(R.id.btnGoHome).setOnClickListener {
            findNavController().navigate(
                R.id.action_bookingSuccess_to_homeFragment
            )
        }
    }
}