package com.example.labourbook.network

import java.text.SimpleDateFormat
import java.util.Locale

object BookingDateValidator {

    fun isBookingExpired(bookingDateString: String?, currentTimeMillis: Long = System.currentTimeMillis()): Boolean {
        if (bookingDateString.isNullOrBlank()) return false

        val inputFormats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd"
        )

        for (format in inputFormats) {
            try {
                val inputFormat = SimpleDateFormat(format, Locale.US)
                val date = inputFormat.parse(bookingDateString)
                if (date != null) {
                    return date.time < currentTimeMillis
                }
            } catch (_: Exception) {
                // Ignore and try next format
            }
        }
        return false
    }
}
