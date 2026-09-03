package com.example.labourbook.network

import android.util.Log
import com.example.labourbook.model.BookingRequest
import com.example.labourbook.model.BookingResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object BookingRepository {

    suspend fun createBooking(
        instanceUrl: String,
        accessToken: String,
        booking: BookingRequest
    ): BookingResponse? {

        val retrofit = Retrofit.Builder()
            .baseUrl("$instanceUrl/")
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()

        val api = retrofit.create(
            SalesforceApi::class.java
        )

        val response = api.createBooking(
            url = "services/data/v61.0/sobjects/Booking__c/",
            authorization = "Bearer $accessToken",
            booking = booking
        )

        if (response.isSuccessful) {

            Log.d(
                "BOOKING",
                "Success: ${response.body()}"
            )

            return response.body()

        } else {

            Log.e(
                "BOOKING",
                "Error Code: ${response.code()}"
            )

            Log.e(
                "BOOKING",
                "Error Body: ${response.errorBody()?.string()}"
            )

            return null
        }
    }
}