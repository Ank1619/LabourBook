package com.example.labourbook.network

import com.example.labourbook.model.BookingListResponse
import com.example.labourbook.model.UpdateBookingStatusRequest
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object BookingListRepository {

    suspend fun getBookings(
        instanceUrl: String,
        accessToken: String,
        customerId: String
    ): BookingListResponse? {

        val retrofit = Retrofit.Builder()
            .baseUrl("$instanceUrl/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(SalesforceApi::class.java)

        val query = """
            SELECT Id,
                   Name,
                   Worker__c,
                   Worker__r.Name,
                   Booking_Date__c,
                   Booking_Labour_Status__c,
                   Amount__c,
                   Work_Address__c,
                   Work_Description__c,
                   Working_Hour__c,
                   Booking_Type__c
            FROM Booking__c
            WHERE L_Customer__c = '$customerId'
            ORDER BY CreatedDate DESC
        """.trimIndent()

        try {
            val response = api.getBookings(
                authorization = "Bearer $accessToken",
                query = query
            )

            if (response.isSuccessful) {
                return processExpiredBookings(api, accessToken, response.body())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback query if new fields fail
        val fallbackQuery = """
            SELECT Id,
                   Name,
                   Worker__c,
                   Worker__r.Name,
                   Booking_Date__c,
                   Booking_Labour_Status__c,
                   Amount__c,
                   Work_Address__c,
                   Work_Description__c
            FROM Booking__c
            WHERE L_Customer__c = '$customerId'
            ORDER BY CreatedDate DESC
        """.trimIndent()

        return try {
            val response = api.getBookings(
                authorization = "Bearer $accessToken",
                query = fallbackQuery
            )
            if (response.isSuccessful) processExpiredBookings(api, accessToken, response.body()) else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getWorkerBookings(
        instanceUrl: String,
        accessToken: String,
        workerId: String
    ): BookingListResponse? {

        val retrofit = Retrofit.Builder()
            .baseUrl("$instanceUrl/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(SalesforceApi::class.java)

        val query = """
            SELECT Id,
                   Name,
                   Worker__c,
                   Worker__r.Name,
                   L_Customer__c,
                   L_Customer__r.Name,
                   Booking_Date__c,
                   Booking_Labour_Status__c,
                   Amount__c,
                   Work_Address__c,
                   Work_Description__c,
                   Working_Hour__c,
                   Booking_Type__c
            FROM Booking__c
            WHERE Worker__c = '$workerId'
            ORDER BY CreatedDate DESC
        """.trimIndent()

        try {
            val response = api.getBookings(
                authorization = "Bearer $accessToken",
                query = query
            )
            if (response.isSuccessful) return processExpiredBookings(api, accessToken, response.body())
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback query without L_Customer__r if relationship field fails
        val fallbackQuery = """
            SELECT Id,
                   Name,
                   Worker__c,
                   Worker__r.Name,
                   Booking_Date__c,
                   Booking_Labour_Status__c,
                   Amount__c,
                   Work_Address__c,
                   Work_Description__c,
                   Working_Hour__c,
                   Booking_Type__c
            FROM Booking__c
            WHERE Worker__c = '$workerId'
            ORDER BY CreatedDate DESC
        """.trimIndent()

        return try {
            val response = api.getBookings(
                authorization = "Bearer $accessToken",
                query = fallbackQuery
            )
            if (response.isSuccessful) processExpiredBookings(api, accessToken, response.body()) else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun processExpiredBookings(
        api: SalesforceApi,
        accessToken: String,
        body: BookingListResponse?
    ): BookingListResponse? {
        if (body == null || body.records.isEmpty()) return body

        val now = System.currentTimeMillis()
        val processedRecords = body.records.map { booking ->
            val status = booking.Booking_Labour_Status__c?.trim()
            if (status.equals("Pending", ignoreCase = true) && BookingDateValidator.isBookingExpired(booking.Booking_Date__c, now)) {
                try {
                    api.updateBookingStatus(
                        authorization = "Bearer $accessToken",
                        bookingId = booking.Id,
                        request = UpdateBookingStatusRequest(Booking_Labour_Status__c = "Cancelled")
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                booking.copy(Booking_Labour_Status__c = "Cancelled")
            } else {
                booking
            }
        }

        return body.copy(records = processedRecords)
    }
}
