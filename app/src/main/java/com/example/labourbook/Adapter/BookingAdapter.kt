package com.example.labourbook.Adapter

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

class BookingAdapter(
    private var bookingList: List<BookingModel>,
    private val onCancelClick: (BookingModel) -> Unit,
    private val onBookingClick: (BookingModel) -> Unit
) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvBookingName: TextView = itemView.findViewById(R.id.tvBookingName)
        val tvBookingStatus: TextView = itemView.findViewById(R.id.tvBookingStatus)
        val tvWorkerName: TextView = itemView.findViewById(R.id.tvWorkerName)
        val tvBookingDate: TextView = itemView.findViewById(R.id.tvBookingDate)
        val tvBookingAddress: TextView = itemView.findViewById(R.id.tvBookingAddress)
        val tvBookingAmount: TextView = itemView.findViewById(R.id.tvBookingAmount)
        val tvBookingDescription: TextView = itemView.findViewById(R.id.tvBookingDescription)
        val btnCancelBooking: MaterialButton = itemView.findViewById(R.id.btnCancelBooking)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_booking, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookingList[position]

        holder.tvBookingName.text = booking.Name

        val status = booking.Booking_Labour_Status__c?.trim() ?: "Pending"
        holder.tvBookingStatus.text = status

        // Hide cancel button if booking is already cancelled or completed
        val statusLower = status.lowercase()
        if (statusLower == "cancelled" || statusLower == "completed") {
            holder.btnCancelBooking.visibility = View.GONE
        } else {
            holder.btnCancelBooking.visibility = View.VISIBLE
        }

        holder.tvWorkerName.text = booking.Worker__r?.Name ?: "Worker not assigned"
        holder.tvBookingDate.text = formatDate(booking.Booking_Date__c)
        holder.tvBookingAddress.text = booking.Work_Address__c ?: "Address N/A"

        val amount = booking.Amount__c
        holder.tvBookingAmount.text = if (amount != null) "₹$amount" else "₹0"

        holder.tvBookingDescription.text = booking.Work_Description__c ?: "No description provided"

        holder.btnCancelBooking.setOnClickListener {
            onCancelClick(booking)
        }

        holder.itemView.setOnClickListener {
            onBookingClick(booking)
        }
    }

    override fun getItemCount(): Int = bookingList.size

    fun updateList(newList: List<BookingModel>) {
        bookingList = newList
        notifyDataSetChanged()
    }

    private fun formatDate(dateString: String?): String {
        if (dateString.isNullOrBlank()) return "Date N/A"

        val inputFormats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ssZ"
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
                // Ignore and try next format
            }
        }
        return dateString
    }
}
