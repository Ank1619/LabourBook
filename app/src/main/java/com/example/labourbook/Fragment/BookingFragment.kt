package com.example.labourbook.Fragment

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.labourbook.Adapter.BookingAdapter
import com.example.labourbook.R
import com.example.labourbook.model.BookingModel
import com.example.labourbook.network.BookingCancelRepository
import com.example.labourbook.network.BookingListRepository
import com.example.labourbook.network.CustomerRepository
import com.google.android.material.tabs.TabLayout
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class BookingFragment : Fragment() {

    private lateinit var recyclerBookings: RecyclerView
    private lateinit var tabLayoutBookings: TabLayout
    private lateinit var bookingAdapter: BookingAdapter

    private var allBookings = listOf<BookingModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_booking,
            container,
            false
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        recyclerBookings =
            view.findViewById(R.id.recyclerBookings)

        tabLayoutBookings =
            view.findViewById(R.id.tabLayoutBookings)

        recyclerBookings.layoutManager =
            LinearLayoutManager(requireContext())

        // Adapter with Cancel + Booking Details click
        bookingAdapter =
            BookingAdapter(

                emptyList(),

                // Cancel button click
                { booking ->
                    showCancelConfirmation(booking)
                },

                // Booking card click
                { booking ->

                    val bundle = Bundle().apply {

                        putString(
                            "bookingId",
                            booking.Id
                        )

                        putString(
                            "bookingName",
                            booking.Name
                        )

                        putString(
                            "workerName",
                            booking.Worker__r?.Name
                        )

                        putString(
                            "bookingDate",
                            booking.Booking_Date__c
                        )

                        putString(
                            "bookingStatus",
                            booking.Booking_Labour_Status__c
                        )

                        putDouble(
                            "bookingAmount",
                            booking.Amount__c ?: 0.0
                        )

                        putString(
                            "bookingAddress",
                            booking.Work_Address__c
                        )

                        putString(
                            "bookingDescription",
                            booking.Work_Description__c
                        )
                    }

                    findNavController().navigate(
                        R.id.action_bookingFragment_to_bookingDetails,
                        bundle
                    )
                }
            )

        recyclerBookings.adapter =
            bookingAdapter

        setupTabs()

        loadBookings()
    }

    override fun onResume() {
        super.onResume()
        loadBookings()
    }

    private fun loadBookings() {

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
            accessToken == null ||
            instanceUrl == null
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

                var customerId =
                    sharedPreferences.getString(
                        "customer_id",
                        null
                    )

                if (customerId.isNullOrEmpty()) {
                    val firebaseUser = Firebase.auth.currentUser
                    if (firebaseUser != null) {
                        val phone = sharedPreferences.getString("customer_phone", "") ?: ""
                        val fetchedId = CustomerRepository.ensureCustomerId(
                            instanceUrl = instanceUrl,
                            accessToken = accessToken,
                            firebaseUser = firebaseUser,
                            phone = phone
                        )
                        if (!fetchedId.isNullOrEmpty()) {
                            customerId = fetchedId
                            sharedPreferences.edit().putString("customer_id", customerId).apply()
                        }
                    }
                }

                if (customerId.isNullOrEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "Customer information not found in Salesforce",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                val response =
                    BookingListRepository.getBookings(
                        instanceUrl = instanceUrl,
                        accessToken = accessToken,
                        customerId = customerId
                    )

                if (response != null) {

                    allBookings =
                        response.records

                    when (tabLayoutBookings.selectedTabPosition) {

                        0 -> showUpcomingBookings()

                        1 -> showCompletedBookings()

                        else -> showUpcomingBookings()
                    }

                } else {

                    Toast.makeText(
                        requireContext(),
                        "Failed to fetch bookings",
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

    private fun setupTabs() {

        tabLayoutBookings.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {

                override fun onTabSelected(
                    tab: TabLayout.Tab?
                ) {

                    when (tab?.position) {

                        0 -> showUpcomingBookings()

                        1 -> showCompletedBookings()
                    }
                }

                override fun onTabUnselected(
                    tab: TabLayout.Tab?
                ) {
                }

                override fun onTabReselected(
                    tab: TabLayout.Tab?
                ) {
                }
            }
        )
    }

    private fun showUpcomingBookings() {

        val upcomingBookings =
            allBookings.filter { booking ->

                val status =
                    booking.Booking_Labour_Status__c
                        ?.trim()
                        ?.lowercase()

                status == "pending" ||
                        status == "accepted" ||
                        status == "confirmed" ||
                        status == "in progress" ||
                        status == "upcoming"
            }

        bookingAdapter.updateList(
            upcomingBookings
        )
    }

    private fun showCompletedBookings() {

        val completedBookings =
            allBookings.filter { booking ->

                booking.Booking_Labour_Status__c
                    ?.trim()
                    ?.equals(
                        "Completed",
                        ignoreCase = true
                    ) == true
            }

        bookingAdapter.updateList(
            completedBookings
        )
    }

    private fun showCancelConfirmation(
        booking: BookingModel
    ) {

        AlertDialog.Builder(requireContext())
            .setTitle("Cancel Booking")
            .setMessage(
                "Are you sure you want to cancel ${booking.Name}?"
            )
            .setPositiveButton("Yes, Cancel") { _, _ ->

                cancelBooking(booking)
            }
            .setNegativeButton(
                "No",
                null
            )
            .show()
    }

    private fun cancelBooking(
        booking: BookingModel
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
            accessToken == null ||
            instanceUrl == null
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

                val success =
                    BookingCancelRepository.cancelBooking(
                        instanceUrl = instanceUrl,
                        accessToken = accessToken,
                        bookingId = booking.Id
                    )

                if (success) {

                    Toast.makeText(
                        requireContext(),
                        "Booking cancelled successfully",
                        Toast.LENGTH_SHORT
                    ).show()

                    loadBookings()

                } else {

                    Toast.makeText(
                        requireContext(),
                        "Failed to cancel booking",
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
