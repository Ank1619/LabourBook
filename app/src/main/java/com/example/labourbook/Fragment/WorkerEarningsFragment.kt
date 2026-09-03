package com.example.labourbook.Fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
import kotlinx.coroutines.launch

class WorkerEarningsFragment : Fragment() {

    private lateinit var tvTotalEarningsHeader: TextView
    private lateinit var recyclerWorkerEarnings: RecyclerView
    private lateinit var cardNoWorkerEarnings: View
    private lateinit var workerBookingAdapter: WorkerBookingAdapter

    private var completedBookings = listOf<BookingModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_worker_earnings,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvTotalEarningsHeader = view.findViewById(R.id.tvTotalEarningsHeader)
        recyclerWorkerEarnings = view.findViewById(R.id.recyclerWorkerEarnings)
        cardNoWorkerEarnings = view.findViewById(R.id.cardNoWorkerEarnings)

        recyclerWorkerEarnings.layoutManager = LinearLayoutManager(requireContext())

        workerBookingAdapter = WorkerBookingAdapter(
            emptyList(),
            onAcceptClick = {},
            onRejectClick = {},
            onCompleteClick = {},
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
                    putString("status", booking.Booking_Labour_Status__c ?: "Completed")
                }
                (activity as? WorkerMainActivity)?.showWorkerBookingDetailsFragment(bundle)
            }
        )

        recyclerWorkerEarnings.adapter = workerBookingAdapter

        loadCompletedEarnings()
    }

    override fun onResume() {
        super.onResume()
        loadCompletedEarnings()
    }

    private fun loadCompletedEarnings() {
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
            cardNoWorkerEarnings.visibility = View.VISIBLE
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
                    completedBookings = response.records.filter { booking ->
                        booking.Booking_Labour_Status__c?.trim()?.equals("Completed", ignoreCase = true) == true
                    }

                    val totalEarnings = completedBookings.sumOf { it.Amount__c ?: 0.0 }
                    tvTotalEarningsHeader.text = "₹ ${totalEarnings.toInt()}"

                    workerBookingAdapter.updateList(completedBookings)

                    if (completedBookings.isEmpty()) {
                        cardNoWorkerEarnings.visibility = View.VISIBLE
                    } else {
                        cardNoWorkerEarnings.visibility = View.GONE
                    }
                } else {
                    cardNoWorkerEarnings.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                e.printStackTrace()
                cardNoWorkerEarnings.visibility = View.VISIBLE
            }
        }
    }
}
