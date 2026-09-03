package com.example.labourbook.Adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.labourbook.R
import com.example.labourbook.model.BookingModel
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Locale

class WorkerBookingAdapter(
    private var bookings: List<BookingModel>,
    private val onAcceptClick: (BookingModel) -> Unit,
    private val onRejectClick: (BookingModel) -> Unit,
    private val onCompleteClick: (BookingModel) -> Unit,
    private val onBookingClick: ((BookingModel) -> Unit)? = null
) : RecyclerView.Adapter<WorkerBookingAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvWorkerJobTitle)
        val tvStatus: TextView = itemView.findViewById(R.id.tvWorkerJobStatus)
        val tvCustomer: TextView = itemView.findViewById(R.id.tvWorkerJobCustomer)
        val tvLocation: TextView = itemView.findViewById(R.id.tvWorkerJobLocation)
        val tvDate: TextView = itemView.findViewById(R.id.tvWorkerJobDate)
        val tvPrice: TextView = itemView.findViewById(R.id.tvWorkerJobPrice)
        val btnReject: MaterialButton = itemView.findViewById(R.id.btnWorkerReject)
        val btnAccept: MaterialButton = itemView.findViewById(R.id.btnWorkerAccept)
        val btnComplete: MaterialButton = itemView.findViewById(R.id.btnWorkerComplete)
        val layoutActions: View = itemView.findViewById(R.id.layoutWorkerActions)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_worker_booking, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val booking = bookings[position]

        val title = booking.Work_Description__c ?: booking.Booking_Type__c ?: booking.Name
        holder.tvTitle.text = title

        val customerName = booking.L_Customer__r?.Name ?: "Customer"
        holder.tvCustomer.text = "👤 $customerName"

        holder.tvLocation.text = "📍 ${booking.Work_Address__c ?: "Location not specified"}"

        val amount = booking.Amount__c
        holder.tvPrice.text = if (amount != null) "₹ ${amount.toInt()}" else "Amount N/A"

        holder.tvDate.text = "📅 ${formatDate(booking.Booking_Date__c)}"

        val status = booking.Booking_Labour_Status__c?.trim() ?: "Pending"
        holder.tvStatus.text = status

        val statusLower = status.lowercase()

        when (statusLower) {
            "pending" -> {
                holder.tvStatus.setTextColor(Color.parseColor("#2563EB"))
                holder.layoutActions.visibility = View.VISIBLE
                holder.btnReject.visibility = View.VISIBLE
                holder.btnAccept.visibility = View.VISIBLE
                holder.btnComplete.visibility = View.GONE
            }
            "accepted", "in progress", "confirmed" -> {
                holder.tvStatus.setTextColor(Color.parseColor("#16A34A"))
                holder.layoutActions.visibility = View.VISIBLE
                holder.btnReject.visibility = View.GONE
                holder.btnAccept.visibility = View.GONE
                holder.btnComplete.visibility = View.VISIBLE
            }
            "completed" -> {
                holder.tvStatus.setTextColor(Color.parseColor("#16A34A"))
                holder.layoutActions.visibility = View.GONE
            }
            "cancelled", "rejected" -> {
                holder.tvStatus.setTextColor(Color.parseColor("#DC2626"))
                holder.layoutActions.visibility = View.GONE
            }
            else -> {
                holder.tvStatus.setTextColor(Color.parseColor("#64748B"))
                holder.layoutActions.visibility = View.GONE
            }
        }

        holder.btnAccept.setOnClickListener { onAcceptClick(booking) }
        holder.btnReject.setOnClickListener { onRejectClick(booking) }
        holder.btnComplete.setOnClickListener { onCompleteClick(booking) }

        holder.itemView.setOnClickListener {
            onBookingClick?.invoke(booking)
        }
    }

    override fun getItemCount(): Int = bookings.size

    fun updateList(newList: List<BookingModel>) {
        bookings = newList
        notifyDataSetChanged()
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
