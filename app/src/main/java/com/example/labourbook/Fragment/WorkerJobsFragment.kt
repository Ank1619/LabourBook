package com.example.labourbook.Fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.labourbook.Adapter.WorkerBookingAdapter
import com.example.labourbook.R
import com.example.labourbook.WorkerMainActivity
import com.example.labourbook.model.BookingModel
import com.example.labourbook.network.BookingListRepository
import com.example.labourbook.network.PendingBookingRepository
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class WorkerJobsFragment : Fragment() {

    private lateinit var recyclerWorkerJobs: RecyclerView
    private lateinit var tabLayoutWorkerJobs: TabLayout
    private lateinit var cardNoWorkerJobs: View
    private lateinit var workerBookingAdapter: WorkerBookingAdapter

    private var allWorkerBookings = listOf<BookingModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_worker_jobs,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerWorkerJobs = view.findViewById(R.id.recyclerWorkerJobs)
        tabLayoutWorkerJobs = view.findViewById(R.id.tabLayoutWorkerJobs)
        cardNoWorkerJobs = view.findViewById(R.id.cardNoWorkerJobs)

        recyclerWorkerJobs.layoutManager = LinearLayoutManager(requireContext())

        workerBookingAdapter = WorkerBookingAdapter(
            emptyList(),
            onAcceptClick = { booking ->
                updateJobStatus(booking, "Accepted")
            },
            onRejectClick = { booking ->
                updateJobStatus(booking, "Rejected")
            },
            onCompleteClick = { booking ->
                updateJobStatus(booking, "Completed")
            },
            onBookingClick = { booking ->
                val bundle = Bundle().apply {
                    putString("bookingId", booking.Id)
                    putString("bookingName", booking.Name)
                    putString("customerName", booking.L_Customer__r?.Name ?: "Customer")
                    putString("workDescription", booking.Work_Description__c ?: booking.Booking_Type__c ?: "Work Description")
                    putString("workAddress", booking.Work_Address__c ?: "Location not specified")
                    putString("bookingDate", booking.Booking_Date__c)
                    putDouble("workingHours", booking.Working_Hour__c ?: 0.0)
                    putString("bookingType", booking.Booking_Type__c ?: "")
                    putDouble("amount", booking.Amount__c ?: 0.0)
                    putString("status", booking.Booking_Labour_Status__c ?: "Pending")
                }
                (activity as? WorkerMainActivity)?.showWorkerBookingDetailsFragment(bundle)
            }
        )

        recyclerWorkerJobs.adapter = workerBookingAdapter

        setupTabs()
        loadWorkerBookings()

        val initialTab = arguments?.getInt("initialTab", 0) ?: 0
        if (initialTab in 0..2) {
            tabLayoutWorkerJobs.getTabAt(initialTab)?.select()
        }
    }

    private fun setupTabs() {
        tabLayoutWorkerJobs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                filterBookingsForTab(tab?.position ?: 0)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun loadWorkerBookings() {
        val sharedPreferences = requireContext().getSharedPreferences(
            "salesforce_auth",
            Context.MODE_PRIVATE
        )
        val appPrefs = requireContext().getSharedPreferences(
            "app_prefs",
            Context.MODE_PRIVATE
        )

        val accessToken = sharedPreferences.getString("access_token", null)
        val instanceUrl = sharedPreferences.getString("instance_url", null)
        val workerId = appPrefs.getString("worker_id", null)

        if (accessToken.isNullOrEmpty() || instanceUrl.isNullOrEmpty() || workerId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Worker information missing", Toast.LENGTH_SHORT).show()
            cardNoWorkerJobs.visibility = View.VISIBLE
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = BookingListRepository.getWorkerBookings(
                    instanceUrl = instanceUrl,
                    accessToken = accessToken,
                    workerId = workerId
                )

                if (response != null) {
                    allWorkerBookings = response.records
                    filterBookingsForTab(tabLayoutWorkerJobs.selectedTabPosition)
                } else {
                    cardNoWorkerJobs.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                e.printStackTrace()
                cardNoWorkerJobs.visibility = View.VISIBLE
            }
        }
    }

    private fun filterBookingsForTab(tabPosition: Int) {
        val filteredList = when (tabPosition) {
            0 -> allWorkerBookings // All
            1 -> allWorkerBookings.filter { booking -> // Upcoming
                val status = booking.Booking_Labour_Status__c?.trim()?.lowercase()
                status == "pending" || status == "accepted" || status == "in progress" || status == "confirmed" || status == "upcoming"
            }
            2 -> allWorkerBookings.filter { booking -> // Completed
                booking.Booking_Labour_Status__c?.trim()?.equals("Completed", ignoreCase = true) == true
            }
            else -> allWorkerBookings
        }

        workerBookingAdapter.updateList(filteredList)

        if (filteredList.isEmpty()) {
            cardNoWorkerJobs.visibility = View.VISIBLE
        } else {
            cardNoWorkerJobs.visibility = View.GONE
        }
    }

    private fun updateJobStatus(booking: BookingModel, newStatus: String) {
        val sharedPreferences = requireContext().getSharedPreferences(
            "salesforce_auth",
            Context.MODE_PRIVATE
        )
        val accessToken = sharedPreferences.getString("access_token", null)
        val instanceUrl = sharedPreferences.getString("instance_url", null)

        if (accessToken.isNullOrEmpty() || instanceUrl.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Salesforce connection error", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val success = PendingBookingRepository.updateBookingStatus(
                    instanceUrl = instanceUrl,
                    accessToken = accessToken,
                    bookingId = booking.Id,
                    status = newStatus
                )

                if (success) {
                    Toast.makeText(requireContext(), "Status updated to $newStatus!", Toast.LENGTH_SHORT).show()
                    loadWorkerBookings()
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
}
