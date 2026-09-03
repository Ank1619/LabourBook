package com.example.labourbook.network

import com.example.labourbook.PendingBookingModel
import com.example.labourbook.model.UpdateBookingStatusRequest

object PendingBookingRepository {

    suspend fun getPendingBookings(
        instanceUrl: String,
        accessToken: String,
        workerId: String
    ): List<PendingBookingModel> {

        return try {

            val query = """
                SELECT
                    Id,
                    Name,
                    Booking_Type__c,
                    Work_Description__c,
                    Work_Address__c,
                    Booking_Date__c,
                    Working_Hour__c,
                    Amount__c,
                    Booking_Labour_Status__c
                FROM Booking__c
                WHERE Worker__c = '$workerId'
                AND Booking_Labour_Status__c = 'Pending'
                ORDER BY Booking_Date__c ASC
            """.trimIndent().replace("\n", " ")

            val retrofit = RetrofitClient.getClient(instanceUrl)
            val api = retrofit.create(SalesforceApi::class.java)

            val response = api.queryPendingBookings(
                authorization = "Bearer $accessToken",
                query = query
            )

            if (response.isSuccessful) {
                val records = response.body()?.records ?: emptyList()
                val validPending = mutableListOf<PendingBookingModel>()
                val now = System.currentTimeMillis()

                for (booking in records) {
                    if (BookingDateValidator.isBookingExpired(booking.Booking_Date__c, now)) {
                        // Automatically update Salesforce to Cancelled
                        try {
                            api.updateBookingStatus(
                                authorization = "Bearer $accessToken",
                                bookingId = booking.Id,
                                request = UpdateBookingStatusRequest(Booking_Labour_Status__c = "Cancelled")
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        validPending.add(booking)
                    }
                }

                validPending
            } else {
                emptyList()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun updateBookingStatus(
        instanceUrl: String,
        accessToken: String,
        bookingId: String,
        status: String
    ): Boolean {

        return try {

            val retrofit = RetrofitClient.getClient(instanceUrl)
            val api = retrofit.create(SalesforceApi::class.java)

            val response = api.updateBookingStatus(
                authorization = "Bearer $accessToken",
                bookingId = bookingId,
                request = UpdateBookingStatusRequest(
                    Booking_Labour_Status__c = status
                )
            )

            response.isSuccessful

        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
