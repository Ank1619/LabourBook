package com.example.labourbook.network

import com.example.labourbook.model.CancelBookingRequest
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object BookingCancelRepository {

    suspend fun cancelBooking(
        instanceUrl: String,
        accessToken: String,
        bookingId: String
    ): Boolean {

        val retrofit = Retrofit.Builder()
            .baseUrl("$instanceUrl/")
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()

        val api =
            retrofit.create(SalesforceApi::class.java)

        val response =
            api.cancelBooking(
                authorization = "Bearer $accessToken",
                bookingId = bookingId,
                request = CancelBookingRequest()
            )

        return response.isSuccessful
    }
}